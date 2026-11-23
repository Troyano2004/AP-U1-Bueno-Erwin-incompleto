import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * B2: lanza 30 solicitudes TURNO simultaneas (10 hilos por ventanilla).
 * Uso: java -cp out PruebaCarga
 * Reinicie el coordinador antes de cada corrida para que el contador empiece en 0.
 */
public class PruebaCarga {
    private static final int VENTANILLAS = 3;
    private static final int HILOS_POR_VENTANILLA = 10;

    public static void main(String[] args) throws Exception {
        Config cfg = new Config("config.properties");
        String host = cfg.texto("host");
        int puerto = cfg.entero("puerto");
        String token = cfg.texto("token");
        int timeout = 10_000; // en modo inseguro las respuestas tardan un poco mas
        Bitacora bit = new Bitacora("pruebacarga", 9);

        AtomicInteger asignadas = new AtomicInteger();
        AtomicInteger rechazadas = new AtomicInteger();
        AtomicInteger errores = new AtomicInteger();
        ConcurrentLinkedQueue<Integer> numeros = new ConcurrentLinkedQueue<>();

        CountDownLatch salida = new CountDownLatch(1);   // todos arrancan a la vez
        List<Thread> hilos = new ArrayList<>();
        for (int v = 1; v <= VENTANILLAS; v++) {
            for (int i = 0; i < HILOS_POR_VENTANILLA; i++) {
                final int origen = v;
                Thread h = new Thread(() -> {
                    try {
                        salida.await();
                        long t = bit.evento("ENVIA_TURNO", "hilo de V" + origen);
                        String resp = Mensaje.intercambiar(host, puerto,
                                Mensaje.solicitud("TURNO", token, t, origen, ""), timeout);
                        String[] p = Mensaje.partes(resp, 3);
                        bit.recibir(Long.parseLong(p[1]), "RECIBE_" + p[0], p[2]);
                        if (p[0].equals("ASIGNADO")) {
                            asignadas.incrementAndGet();
                            numeros.add(Integer.parseInt(p[2]));
                        } else {
                            rechazadas.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errores.incrementAndGet();
                    }
                });
                hilos.add(h);
                h.start();
            }
        }
        long inicio = System.currentTimeMillis();
        salida.countDown();
        for (Thread h : hilos) h.join();
        long ms = System.currentTimeMillis() - inicio;

        // Lo que dice el coordinador
        long t = bit.evento("ENVIA_ESTADO", "consulta final");
        String[] est = Mensaje.partes(Mensaje.intercambiar(host, puerto,
                Mensaje.solicitud("ESTADO", token, t, 1, ""), timeout), 3);
        bit.recibir(Long.parseLong(est[1]), "RECIBE_ESTADO", est[2]);
        String emitidos = "?";
        for (String parte : est[2].split(" ")) if (parte.startsWith("emitidos=")) emitidos = parte.substring(9);

        List<Integer> lista = new ArrayList<>(numeros);
        Collections.sort(lista);
        Map<Integer, Integer> repetidos = new TreeMap<>();
        for (int n : lista) repetidos.merge(n, 1, Integer::sum);
        repetidos.values().removeIf(c -> c == 1);

        System.out.println("=========== PruebaCarga ===========");
        System.out.println("Solicitudes enviadas        : " + (VENTANILLAS * HILOS_POR_VENTANILLA));
        System.out.println("Asignadas                   : " + asignadas.get());
        System.out.println("Rechazadas                  : " + rechazadas.get());
        System.out.println("Errores de conexion         : " + errores.get());
        System.out.println("Numeros distintos entregados: " + new TreeSet<>(lista).size());
        System.out.println("Turnos emitidos (coordinador): " + emitidos);
        System.out.println("Numeros recibidos           : " + lista);
        System.out.println("Repetidos (numero=veces)    : " + (repetidos.isEmpty() ? "ninguno" : repetidos));
        System.out.println("Tiempo total                : " + ms + " ms");
    }
}
