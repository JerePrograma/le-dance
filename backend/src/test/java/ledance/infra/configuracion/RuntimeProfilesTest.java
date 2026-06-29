package ledance.infra.configuracion;

import ledance.servicios.ScheduledTasks;
import ledance.servicios.asistencia.AsistenciaMensualServicio;
import ledance.servicios.email.EmailService;
import ledance.servicios.email.IEmailService;
import ledance.servicios.email.NoOpEmailService;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.notificaciones.NotificacionService;
import ledance.servicios.recargo.RecargoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RuntimeProfilesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ProfileConfiguration.class)
            .withPropertyValues(
                    "spring.mail.imap.host=imap.example.test",
                    "spring.mail.imap.port=993",
                    "spring.mail.imap.username=test",
                    "spring.mail.imap.password=test",
                    "spring.mail.imap.properties.mail.imap.ssl.enable=true"
            );

    @Test
    void devActivaUnSoloEmailNoOpYSinSchedulers() {
        runWithProfile("dev", "false", context -> {
            assertThat(context).hasSingleBean(IEmailService.class);
            assertThat(context).hasSingleBean(NoOpEmailService.class);
            assertThat(context).doesNotHaveBean(ScheduledTasks.class);
        });
    }

    @Test
    void testActivaUnSoloEmailNoOpYSinSchedulers() {
        runWithProfile("test", "false", context -> {
            assertThat(context).hasSingleBean(IEmailService.class);
            assertThat(context).hasSingleBean(NoOpEmailService.class);
            assertThat(context).doesNotHaveBean(ScheduledTasks.class);
        });
    }

    @Test
    void prodActivaEmailRealYSchedulersConfigurables() {
        runWithProfile("prod", "true", context -> {
            assertThat(context).hasSingleBean(IEmailService.class);
            assertThat(context).hasSingleBean(EmailService.class);
            assertThat(context).hasSingleBean(ScheduledTasks.class);
        });
        runWithProfile("prod", "false", context ->
                assertThat(context).doesNotHaveBean(ScheduledTasks.class));
    }

    @Test
    void perfilAusenteFallaCerrado() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseMessage("Debe activar exactamente un perfil Spring explícito: dev, test o prod");
        });
    }

    private void runWithProfile(
            String profile,
            String schedulingEnabled,
            ContextConsumer<org.springframework.boot.test.context.assertj.AssertableApplicationContext> assertions
    ) {
        runner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .withPropertyValues("app.scheduling-enabled=" + schedulingEnabled)
                .run(assertions);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            ActiveProfileGuard.class,
            EmailService.class,
            NoOpEmailService.class,
            ScheduledTasks.class
    })
    static class ProfileConfiguration {

        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }

        @Bean
        MensualidadServicio mensualidadServicio() {
            return mock(MensualidadServicio.class);
        }

        @Bean
        MatriculaServicio matriculaServicio() {
            return mock(MatriculaServicio.class);
        }

        @Bean
        RecargoServicio recargoServicio() {
            return mock(RecargoServicio.class);
        }

        @Bean
        AsistenciaMensualServicio asistenciaMensualServicio() {
            return mock(AsistenciaMensualServicio.class);
        }

        @Bean
        NotificacionService notificacionService() {
            return mock(NotificacionService.class);
        }
    }
}
