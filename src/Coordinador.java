import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Coordinador de TurnoJusto (id 0).
 * Uso: java -cp out Coordinador [--inseguro]
 */
public class Coordinador {
    private static final int ID = 0;

    private final Config cfg;
    private final Bitacora bit;
    private final boolean inseguro;
    private final String token;
    private final int cupo;
    private final long limiteLatidoMs;
    private final boolean registrarLatidos;

    // ---- estado de los turnos (protegido por candadoTurnos) ----
    private final Object candadoTurnos = new Object();
    private volatile int ultimoEmitido = 0;          // ultimo numero entregado
    private final ArrayDeque<Integer> enEspera = new ArrayDeque<>();
    private int ultimoLlamado = 0;

    // ---- estado de las ventanillas (protegido por candadoVentanillas) ----
    private final Object candadoVentanillas = new Object();
    private final Map<Integer, Long> ultimoLatido = new TreeMap<>();
    private final Map<Integer, String> estadoVentanilla = new TreeMap<>();

    public Coordinador(Config cfg, boolean inseguro) {
        this.cfg = cfg;
        this.inseguro = inseguro;
        this.token = cfg.texto("token");
        this.cupo = cfg.entero("cupo");
        this.limiteLatidoMs = cfg.entero("latido.limite.ms");
        this.registrarLatidos = cfg.logico("registrar.latidos", true);
        this.bit = new Bitacora("coordinador", ID);
        for (int v = 1; v <= 3; v++) estadoVentanilla.put(v, "SIN_REGISTRO");
    }

    public static void main(String[] args) throws IOException {
        boolean inseguro = args.length > 0 && args[0].equals("--inseguro");
        Coordinador c = new Coordinador(new Config("config.properties"), inseguro);
        c.iniciar();
    }

    private void iniciar() throws IOException {
        int puerto = cfg.entero("puerto");
        bit.evento("INICIO", "modo=" + (inseguro ? "INSEGURO" : "normal") + " cupo=" + cupo + " puerto=" + puerto);
        System.out.println("[Coordinador] escuchando en el puerto " + puerto
                + " | cupo=" + cupo + " | modo=" + (inseguro ? "INSEGURO (sin exclusion mutua)" : "normal"));

        // Revisa cada 500 ms si alguna ventanilla dejo de enviar latidos
        ScheduledExecutorService vigilante = Executors.newSingleThreadScheduledExecutor();
        vigilante.scheduleAtFixedRate(this::revisarLatidos, 500, 500, TimeUnit.MILLISECONDS);

        ExecutorService hilos = Executors.newCachedThreadPool();
        try (ServerSocket servidor = new ServerSocket(puerto, 100)) {
            while (true) {
                Socket cliente = servidor.accept();
                hilos.submit(() -> atender(cliente));
            }
        }
    }

    /** Atiende una conexion. Puede recibir varias solicitudes seguidas, una por linea. */
    private void atender(Socket s) {
        try (s;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String linea;
            while ((linea = in.readLine()) != null) {
                out.println(procesar(linea));
            }
        } catch (IOException e) {
            // la ventanilla cerro la conexion; no es un error del coordinador
        }
    }

    private String procesar(String linea) {
        String[] p = Mensaje.partes(linea, 5);
        if (p == null) {
            long t = bit.evento("MENSAJE_INVALIDO", "linea=" + linea);
            return Mensaje.respuesta("RECHAZADA", t, "formato de mensaje invalido");
        }
        String tipo = p[0], tok = p[1], dato = p[4];
        long lamportRecibido;
        int origen;
        try {
            lamportRecibido = Long.parseLong(p[2]);
            origen = Integer.parseInt(p[3]);
        } catch (NumberFormatException e) {
            long t = bit.evento("MENSAJE_INVALIDO", "lamport u origen no numerico");
            return Mensaje.respuesta("RECHAZADA", t, "formato de mensaje invalido");
        }
        String quien = "V" + origen;
        boolean esLatido = tipo.equals("LATIDO");

        // Recepcion: max(local, recibido) + 1
        bit.recibir(lamportRecibido, "RECIBE_" + tipo, "de=" + quien + " lamport_emisor=" + lamportRecibido,
                !esLatido || registrarLatidos);

        // A3: autenticacion por token
        if (tok.isEmpty() || !tok.equals(token)) {
            String motivo = tok.isEmpty() ? "token ausente" : "token invalido";
            System.out.println("[Coordinador] RECHAZADA " + tipo + " de " + quien + ": " + motivo);
            long t = bit.evento("TOKEN_RECHAZADO", "de=" + quien + " tipo=" + tipo + " motivo=" + motivo);
            return Mensaje.respuesta("RECHAZADA", t, motivo);
        }

        switch (tipo) {
            case "TURNO":  return turno(quien);
            case "LLAMAR": return llamar(quien);
            case "ESTADO": return estado(quien);
            case "LATIDO": return latido(origen);
            default: {
                long t = bit.evento("ENVIA_RECHAZADA", "a=" + quien + " motivo=tipo desconocido " + tipo);
                return Mensaje.respuesta("RECHAZADA", t, "tipo de solicitud desconocido");
            }
        }
    }

    // ------------------------------------------------------------------ TURNO
    private String turno(String quien) {
        int numero;
        if (inseguro) {
            numero = turnoInseguro();
        } else {
            // Exclusion mutua: leer, comprobar el cupo y actualizar es una sola
            // operacion atomica. Solo un hilo a la vez entra aqui.
            synchronized (candadoTurnos) {
                if (ultimoEmitido >= cupo) {
                    numero = -1;
                } else {
                    ultimoEmitido = ultimoEmitido + 1;
                    numero = ultimoEmitido;
                    enEspera.addLast(numero);
                }
            }
        }
        if (numero < 0) {
            long t = bit.evento("ENVIA_RECHAZADA", "a=" + quien + " motivo=cupo agotado (" + cupo + ")");
            System.out.println("[Coordinador] TURNO de " + quien + " rechazado: cupo agotado");
            return Mensaje.respuesta("RECHAZADA", t, "cupo agotado (" + cupo + " turnos)");
        }
        long t = bit.evento("ENVIA_ASIGNADO", "a=" + quien + " turno=" + numero);
        System.out.println("[Coordinador] turno " + numero + " asignado por " + quien);
        return Mensaje.respuesta("ASIGNADO", t, String.valueOf(numero));
    }

    /** B3: lectura y escritura separadas por 50 ms, sin candado. Solo para E3. */
    private int turnoInseguro() {
        // TODO B3: leer ultimoEmitido, comprobar el cupo, dormir 50 ms y luego
        // escribir leido + 1, SIN candado. Agregar el numero a enEspera.
        return -1;
    }

    // ----------------------------------------------------------------- LLAMAR
    private String llamar(String quien) {
        Integer numero;
        synchronized (candadoTurnos) {
            numero = enEspera.pollFirst();           // FIFO: el mas antiguo primero
            if (numero != null) ultimoLlamado = numero;
        }
        if (numero == null) {
            long t = bit.evento("ENVIA_RECHAZADA", "a=" + quien + " motivo=no hay turnos en espera");
            return Mensaje.respuesta("RECHAZADA", t, "no hay turnos en espera");
        }
        long t = bit.evento("ENVIA_LLAMADO", "a=" + quien + " turno=" + numero);
        System.out.println("[Coordinador] turno " + numero + " llamado por " + quien);
        return Mensaje.respuesta("LLAMADO", t, String.valueOf(numero));
    }

    // ----------------------------------------------------------------- ESTADO
    private String estado(String quien) {
        String texto = textoEstado();
        long t = bit.evento("ENVIA_ESTADO", "a=" + quien + " " + texto);
        return Mensaje.respuesta("ESTADO", t, texto);
    }

    private String textoEstado() {
        String turnos;
        synchronized (candadoTurnos) {
            turnos = "emitidos=" + ultimoEmitido + " cupo=" + cupo + " ultimo_llamado=" + ultimoLlamado
                    + " en_espera=" + new ArrayList<>(enEspera);
        }
        StringBuilder sb = new StringBuilder(turnos).append(" ventanillas=");
        synchronized (candadoVentanillas) {
            List<String> l = new ArrayList<>();
            estadoVentanilla.forEach((id, est) -> l.add("V" + id + ":" + est));
            sb.append(String.join(",", l));
        }
        return sb.toString();
    }

    // ----------------------------------------------------------------- LATIDO
    private String latido(int origen) {
        // TODO C2: guardar la hora del latido en ultimoLatido y, si la ventanilla
        // no estaba ACTIVA, marcarla ACTIVA y registrar VENTANILLA_ACTIVA.
        long t = bit.evento("ENVIA_ACK_LATIDO", "a=V" + origen, registrarLatidos);
        return Mensaje.respuesta("ACK", t, "");
    }

    /** C2: si pasan mas de latido.limite.ms sin latidos, la ventanilla queda CAIDA. */
    private void revisarLatidos() {
        // TODO C2: recorrer ultimoLatido; si una ventanilla ACTIVA lleva mas de
        // limiteLatidoMs sin latidos, marcarla CAIDA y registrar VENTANILLA_CAIDA.
    }
}
