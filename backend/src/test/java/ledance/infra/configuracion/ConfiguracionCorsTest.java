package ledance.infra.configuracion;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfiguracionCorsTest {

    @Test
    void usaSoloLosOrigenesConfigurados() {
        List<String> origins = List.of("http://localhost:5173", "https://example.test");
        var properties = new AppProperties(
                ZoneId.of("America/Argentina/Buenos_Aires"),
                Path.of("receipts"),
                origins
        );
        var source = (UrlBasedCorsConfigurationSource)
                new ConfiguracionCors(properties).corsConfigurationSource();

        assertEquals(origins,
                source.getCorsConfigurations().get("/**").getAllowedOrigins());
    }
}
