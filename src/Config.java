import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Lee config.properties desde la carpeta donde se ejecuta el programa. */
public class Config {
    private final Properties props = new Properties();

    public Config(String ruta) {
        try (Reader r = new InputStreamReader(new FileInputStream(ruta), StandardCharsets.UTF_8)) {
            props.load(r);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer " + ruta + ": " + e.getMessage());
        }
    }

    public String texto(String clave) {
        String v = props.getProperty(clave);
        if (v == null) throw new RuntimeException("Falta la clave '" + clave + "' en config.properties");
        return v.trim();
    }

    public int entero(String clave) {
        return Integer.parseInt(texto(clave));
    }

    public boolean logico(String clave, boolean porDefecto) {
        String v = props.getProperty(clave);
        return v == null ? porDefecto : Boolean.parseBoolean(v.trim());
    }
}
