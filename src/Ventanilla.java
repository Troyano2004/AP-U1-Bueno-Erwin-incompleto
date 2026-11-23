import java.io.IOException;
import java.util.Scanner;

/**
 * Ventanilla de TurnoJusto.
 * Uso: java -cp out Ventanilla <1|2|3>
 * Ordenes: turno, llamar, estado, token-malo, sin-token, ayuda, salir
 */
public class Ventanilla {
    private final int id;
    private final String nombre;
    private final String host;
    private final int puerto;
    private final String token;
    private final int timeoutMs;
    private final int intervaloLatidoMs;
    private final boolean registrarLatidos;
    private final Bitacora bit;
    private volatile boolean coordinadorResponde = true;

    public Ventanilla(int id, Config cfg) {
        this.id = id;
        this.nombre = "V" + id;
        this.host = cfg.texto("host");
        this.puerto = cfg.entero("puerto");
        this.token = cfg.texto("token");
        this.timeoutMs = cfg.entero("socket.timeout.ms");
        this.intervaloLatidoMs = cfg.entero("latido.intervalo.ms");
        this.registrarLatidos = cfg.logico("registrar.latidos", true);
        this.bit = new Bitacora(nombre, id);
    }

    public static void main(String[] args) {
        if (args.length != 1 || !args[0].matches("[123]")) {
            System.out.println("Uso: java -cp out Ventanilla <1|2|3>");
            return;
        }
        new Ventanilla(Integer.parseInt(args[0]), new Config("config.properties")).iniciar();
    }

    private void iniciar() {
        bit.evento("INICIO", nombre + " inicia");
        Thread latidos = new Thread(this::cicloLatidos, "latidos-" + nombre);
        latidos.setDaemon(true);
        latidos.start();

        System.out.println("[" + nombre + "] lista. Ordenes: turno | llamar | estado | token-malo | sin-token | salir");
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print(nombre + "> ");
            if (!sc.hasNextLine()) break;
            String orden = sc.nextLine().trim().toLowerCase();
            switch (orden) {
                case "turno":      solicitar("TURNO", token); break;
                case "llamar":     solicitar("LLAMAR", token); break;
                case "estado":     solicitar("ESTADO", token); break;
                case "token-malo": solicitar("TURNO", "token-falso"); break; // para E2
                case "sin-token":  solicitar("TURNO", ""); break;            // para E2
                case "":           break;
                case "salir":
                    bit.evento("FIN", nombre + " se detiene");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Ordenes: turno | llamar | estado | token-malo | sin-token | salir");
            }
        }
        bit.evento("FIN", nombre + " se detiene (fin de la entrada)");
    }

    /** Envia una solicitud y muestra la respuesta del coordinador. */
    private void solicitar(String tipo, String tokenUsado) {
        long t = bit.evento("ENVIA_" + tipo, "a=coordinador" + (tokenUsado.equals(token) ? "" : " token_prueba=" + tokenUsado));
        String linea = Mensaje.solicitud(tipo, tokenUsado, t, id, "");
        try {
            String resp = Mensaje.intercambiar(host, puerto, linea, timeoutMs);
            String[] p = Mensaje.partes(resp, 3);
            if (p == null) {
                bit.evento("RESPUESTA_INVALIDA", resp);
                System.out.println("  Respuesta invalida: " + resp);
                return;
            }
            long tr = bit.recibir(Long.parseLong(p[1]), "RECIBE_" + p[0], "de=coordinador respuesta=" + p[2]);
            switch (p[0]) {
                case "ASIGNADO": System.out.println("  Turno asignado: " + p[2]); break;
                case "LLAMADO":  System.out.println("  Atender el turno: " + p[2]); break;
                case "ESTADO":   imprimirEstado(p[2]); break;
                case "RECHAZADA": System.out.println("  RECHAZADA: " + p[2]); break;
                default: System.out.println("  " + p[0] + " " + p[2]);
            }
            System.out.println("  (lamport local=" + tr + ")");
        } catch (IOException e) {
            // E5: sin coordinador NO se inventan numeros (se prioriza la consistencia)
            bit.evento("COORDINADOR_NO_DISPONIBLE", "solicitud " + tipo + " no atendida: " + e.getMessage());
            System.out.println("  No se pudo contactar al coordinador (" + e.getMessage() + ").");
            if (tipo.equals("TURNO")) {
                System.out.println("  No se entrega numero para evitar duplicados. Pida a la persona que espere y repita 'turno'.");
            }
        }
    }

    private void imprimirEstado(String texto) {
        System.out.println("  Estado del coordinador:");
        for (String parte : texto.split(" (?=[a-z_]+=)")) System.out.println("    " + parte);
    }

    /** C1: un latido cada latido.intervalo.ms, con la marca de Lamport. */
    private void cicloLatidos() {
        // TODO C1: cada intervaloLatidoMs enviar LATIDO al coordinador con la marca
        // de Lamport, actualizar el reloj con la respuesta y avisar (una sola vez)
        // cuando el coordinador deje de responder o vuelva a hacerlo.
    }
}
