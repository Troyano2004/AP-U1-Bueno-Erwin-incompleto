import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * D3: une logs/coordinador.log, V1.log, V2.log y V3.log en logs/orden_total.log,
 * ordenado por marca de Lamport y, si empatan, por identificador de proceso.
 * Uso: java -cp out OrdenTotal
 */
public class OrdenTotal {
    record Linea(long lamport, int id, String texto) { }

    public static void main(String[] args) throws IOException {
        String[] procesos = {"coordinador", "V1", "V2", "V3"};
        List<Linea> todas = new ArrayList<>();
        for (String p : procesos) {
            Path archivo = Paths.get("logs", p + ".log");
            if (!Files.exists(archivo)) {
                System.out.println("Aviso: no existe " + archivo);
                continue;
            }
            int n = 0;
            for (String l : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                String[] c = l.split(";", 4);
                if (c.length < 4) continue;
                try {
                    todas.add(new Linea(Long.parseLong(c[0]), Integer.parseInt(c[1]), l));
                    n++;
                } catch (NumberFormatException ignorada) { }
            }
            System.out.println(archivo + ": " + n + " eventos");
        }
        // TODO D3: ordenar 'todas' por marca de Lamport y, si empatan, por id de proceso

        List<String> salida = new ArrayList<>();
        for (Linea l : todas) salida.add(l.texto());
        Path destino = Paths.get("logs", "orden_total.log");
        Files.write(destino, salida, StandardCharsets.UTF_8);
        System.out.println("Escrito " + destino + " con " + salida.size() + " eventos");
    }
}
