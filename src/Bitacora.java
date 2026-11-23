import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Reloj de Lamport + bitacora del proceso.
 * Cada evento se registra en logs/<proceso>.log con el formato
 * lamport;id_proceso;evento;detalle
 *
 * El reloj y la escritura van dentro del mismo bloque synchronized para que
 * las lineas del archivo queden en el mismo orden que los valores del reloj.
 */
public class Bitacora {
    private final int idProceso;
    private final PrintWriter salida;
    private long reloj;

    public Bitacora(String nombreProceso, int idProceso) {
        this.idProceso = idProceso;
        File carpeta = new File("logs");
        carpeta.mkdirs();
        File archivo = new File(carpeta, nombreProceso + ".log");
        // Si el proceso se reinicia, el reloj continua desde el ultimo valor
        // registrado para no repetir marcas entre ejecuciones (E1 a E5).
        this.reloj = ultimoValor(archivo);
        try {
            this.salida = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(archivo, true), StandardCharsets.UTF_8), true);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo abrir " + archivo + ": " + e.getMessage());
        }
    }

    /** Evento local o envio: reloj = reloj + 1. Devuelve la marca para adjuntarla al mensaje. */
    public synchronized long evento(String evento, String detalle) {
        return evento(evento, detalle, true);
    }

    public synchronized long evento(String evento, String detalle, boolean registrar) {
        // TODO D1: incrementar el reloj en 1 antes del evento local o envio
        if (registrar) escribir(evento, detalle);
        return reloj;
    }

    /** Recepcion: reloj = max(local, recibido) + 1. */
    public synchronized long recibir(long recibido, String evento, String detalle) {
        return recibir(recibido, evento, detalle, true);
    }

    public synchronized long recibir(long recibido, String evento, String detalle, boolean registrar) {
        // TODO D1: al recibir, fijar el reloj en max(local, recibido) + 1
        if (registrar) escribir(evento, detalle);
        return reloj;
    }

    public synchronized long valor() {
        return reloj;
    }

    private void escribir(String evento, String detalle) {
        // el ; separa campos, asi que no puede aparecer en el detalle
        salida.println(reloj + ";" + idProceso + ";" + evento + ";" + detalle.replace(';', ','));
    }

    private static long ultimoValor(File archivo) {
        long max = 0;
        if (!archivo.exists()) return 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                int p = linea.indexOf(';');
                if (p > 0) {
                    try { max = Math.max(max, Long.parseLong(linea.substring(0, p))); }
                    catch (NumberFormatException ignorada) { }
                }
            }
        } catch (IOException e) {
            System.err.println("Aviso: no se pudo leer " + archivo + ", el reloj inicia en 0");
        }
        return max;
    }
}
