// src/main/java/ledance/util/FilePathResolver.java
package ledance.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilidad para resolver rutas partiendo de la variable de entorno LEDANCE_HOME.
 */
public final class FilePathResolver {

    private static final String BASE_DIR = initBaseDir();

    private FilePathResolver() {
        // No instanciable
    }

    private static String initBaseDir() {
        String baseDir = System.getProperty("ledance.home");
        if (baseDir == null || baseDir.isBlank()) {
            baseDir = System.getenv("LEDANCE_HOME");
        }
        if (baseDir == null || baseDir.isBlank()) {
            throw new IllegalStateException(
                    "LEDANCE_HOME no definida (variable de entorno o propiedad ledance.home)"
            );
        }
        return baseDir;
    }

    /**
     * Resuelve una ruta a partir del directorio base,
     * concatenando los segmentos que le pases.
     * Ejemplo:
     *   FilePathResolver.of("imgs", "firma.png")
     *   → /opt/le-dance/imgs/firma.png   (suponiendo LEDANCE_HOME=/opt/le-dance)
     *
     * @param segments segmentos de la ruta relativa
     * @return Path completo
     */
    public static Path of(String... segments) {
        return Paths.get(BASE_DIR, segments);
    }
}
