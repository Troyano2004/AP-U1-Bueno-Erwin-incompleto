
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

        // TODO B2:
        //  1. Crear 30 hilos (10 por ventanilla) que esperen un CountDownLatch.
        //  2. Liberarlos a la vez; cada uno envia TURNO y cuenta ASIGNADO o RECHAZADA.
        //  3. Al final pedir ESTADO y mostrar: asignadas, rechazadas,
        //     numeros distintos y turnos emitidos segun el coordinador.
        System.out.println("PruebaCarga pendiente (TODO B2)");
    }
}
