package ledance.infra.persistencia;

import ledance.Main;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PostgreSqlSchemaValidationTest extends PostgreSqlIntegrationTest {

    @Test
    void aplicaV1AV060ConChecksumsValidosYHibernateValidate() throws Exception {
        String databaseName = "ledance_schema_validation";
        String jdbcUrl = POSTGRESQL.getJdbcUrl().replace(POSTGRESQL.getDatabaseName(), databaseName);
        try (Connection admin = POSTGRESQL.createConnection("");
             Statement statement = admin.createStatement()) {
            admin.setAutoCommit(true);
            statement.execute("DROP DATABASE IF EXISTS " + databaseName + " WITH (FORCE)");
            statement.execute("CREATE DATABASE " + databaseName);
        }

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .defaultSchema("public")
                .schemas("public")
                .baselineOnMigrate(false)
                .load();

        flyway.migrate();
        ValidateResult validation = flyway.validateWithResult();

        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getVersion()).isEqualTo(MigrationVersion.fromVersion("60"));
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl, POSTGRESQL.getUsername(), POSTGRESQL.getPassword());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false")) {
            result.next();
            assertThat(result.getInt(1)).isZero();
        }
        assertThat(validation.validationSuccessful)
                .withFailMessage(validation.getAllErrorMessages())
                .isTrue();
        assertThat(validation.invalidMigrations).isEmpty();

        assertThatCode(() -> new SpringApplicationBuilder(Main.class).run(
                "--spring.profiles.active=test",
                "--spring.main.web-application-type=none",
                "--spring.datasource.url=" + jdbcUrl,
                "--spring.datasource.username=" + POSTGRESQL.getUsername(),
                "--spring.datasource.password=" + POSTGRESQL.getPassword(),
                "--spring.flyway.enabled=false",
                "--spring.jpa.hibernate.ddl-auto=validate").close())
                .doesNotThrowAnyException();
    }
}
