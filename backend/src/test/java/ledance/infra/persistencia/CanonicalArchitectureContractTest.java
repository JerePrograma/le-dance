package ledance.infra.persistencia;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalArchitectureContractTest {

    private static final List<String> REMOVED_MODEL = List.of(
            "DetallePago", "PaymentProcessor", "PaymentCalculationServicio", "ProcesoEjecutado",
            "esClon", "es_clon");
    private static final Pattern JAVA_FLOATING_POINT = Pattern.compile("\\b(?:Double|double|Float|float)\\b");
    private static final Pattern UNCONTROLLED_TIME = Pattern.compile("\\b(?:LocalDate|LocalDateTime|YearMonth)\\.now\\(\\s*\\)");
    private static final Pattern TYPESCRIPT_MONEY_NUMBER = Pattern.compile(
            "(?i)\\b(?:monto|importe|precio|saldo|credito|valorCuota|matricula|claseSuelta|clasePrueba|recargo|porcentaje|valorFijo|costoParticular)\\??\\s*:\\s*number\\b");

    private final Path root = repositoryRoot();

    @Test
    void conservaUnaSolaMigracionCanonica() throws IOException {
        Path migrations = root.resolve("backend/src/main/resources/db/migration");
        try (Stream<Path> files = Files.list(migrations)) {
            assertThat(files.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).toList())
                    .containsExactly("V1__canonical_schema.sql");
        }
    }

    @Test
    void noReintroduceModeloFinancieroEliminadoNiAntipatronesProductivos() throws IOException {
        String backend = source(root.resolve("backend/src/main"));
        String frontend = source(root.resolve("frontend/src"));

        for (String removed : REMOVED_MODEL) {
            assertThat(backend).as("modelo eliminado en backend: %s", removed).doesNotContain(removed);
            assertThat(frontend).as("modelo eliminado en frontend: %s", removed).doesNotContain(removed);
        }
        assertThat(filesContaining(root.resolve("backend/src/main"), JAVA_FLOATING_POINT))
                .as("float permitido solo para anchos de columnas PDF, nunca para dinero")
                .containsExactly("backend/src/main/java/ledance/servicios/pdfs/PdfService.java");
        assertThat(UNCONTROLLED_TIME.matcher(backend).find()).as("reloj del sistema sin Clock").isFalse();
        assertThat(backend).doesNotContain("printStackTrace(", "@Data", "/api/deudas", "/api/email");
        assertThat(TYPESCRIPT_MONEY_NUMBER.matcher(frontend).find())
                .as("contrato monetario TypeScript declarado como number").isFalse();
    }

    private String source(Path sourceRoot) throws IOException {
        StringBuilder content = new StringBuilder();
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java")
                            || file.toString().endsWith(".sql")
                            || file.toString().endsWith(".ts")
                            || file.toString().endsWith(".tsx"))
                    .toList()) {
                content.append(Files.readString(path)).append('\n');
            }
        }
        return content.toString();
    }

    private Set<String> filesContaining(Path sourceRoot, Pattern pattern) throws IOException {
        Set<String> matches = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path path : paths.filter(Files::isRegularFile).filter(file -> file.toString().endsWith(".java")).toList()) {
                if (pattern.matcher(Files.readString(path)).find()) {
                    matches.add(root.relativize(path).toString().replace('\\', '/'));
                }
            }
        }
        return matches;
    }

    private Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        return current.getFileName().toString().equals("backend") ? current.getParent() : current;
    }
}
