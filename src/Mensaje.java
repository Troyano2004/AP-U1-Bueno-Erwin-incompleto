import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Formato de los mensajes (una linea por mensaje, terminada en \n):
 *   Solicitud: TIPO|token|lamport|origen|dato
 *   Respuesta: TIPO|lamport|dato
 * El salto de linea delimita cada mensaje, asi varias solicitudes seguidas
 * por la misma conexion no se mezclan (readLine lee exactamente una).
 */
public final class Mensaje {
    private Mensaje() { }

    public static String solicitud(String tipo, String token, long lamport, int origen, String dato) {
        return tipo + "|" + token + "|" + lamport + "|" + origen + "|" + limpiar(dato);
    }

    public static String respuesta(String tipo, long lamport, String dato) {
        return tipo + "|" + lamport + "|" + limpiar(dato);
    }

    /** Separa los campos de un mensaje. Devuelve null si no tiene la cantidad esperada. */
    public static String[] partes(String linea, int campos) {
        if (linea == null) return null;
        String[] p = linea.split("\\|", -1);
        return p.length == campos ? p : null;
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }

    /** Abre una conexion, envia una linea y espera una linea de respuesta. */
    public static String intercambiar(String host, int puerto, String linea, int timeoutMs) throws IOException {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, puerto), timeoutMs);
            s.setSoTimeout(timeoutMs);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            out.println(linea);
            String resp = in.readLine();
            if (resp == null) throw new IOException("el coordinador cerro la conexion sin responder");
            return resp;
        }
    }
}
