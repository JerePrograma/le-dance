package ledance.servicios;

import ledance.entidades.EstadoPago;
import ledance.entidades.Recargo;
import ledance.repositorios.RecargoRepositorio;
import ledance.repositorios.PagoRepositorio;
import ledance.entidades.Pago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final RecargoRepositorio recargoRepositorio;
    private final PagoRepositorio pagoRepositorio;

    public ScheduledTasks(RecargoRepositorio recargoRepositorio, PagoRepositorio pagoRepositorio) {
        this.recargoRepositorio = recargoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
    }

    /**
     * Se ejecuta a medianoche todos los días para verificar recargos aplicables.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void aplicarRecargosDiarios() {
        LocalDate hoy = LocalDate.now();
        int diaActual = hoy.getDayOfMonth();
        boolean esPrimerDiaDelMes = diaActual == 1;

        log.info("🔎 Verificando recargos para el día: {}", diaActual);

        // Obtener los recargos normales del día
        List<Recargo> recargosAplicables = recargoRepositorio.findByDiaDelMesAplicacion(diaActual);

        // Si es 1ro del mes, buscar también recargos de 30 días (del mes anterior)
        if (esPrimerDiaDelMes) {
            log.info("📅 Es el primer día del mes, buscando recargos de fin de mes anterior...");
            List<Recargo> recargosDeFinDeMes = recargoRepositorio.findByDiaDelMesAplicacion(30);
            recargosAplicables.addAll(recargosDeFinDeMes);
        }

        if (recargosAplicables.isEmpty()) {
            log.info("✅ No hay recargos programados para hoy ({})", diaActual);
            return;
        }

        for (Recargo recargo : recargosAplicables) {
            log.info("⚡ Aplicando recargo '{}' con porcentaje {}% y valor fijo {}",
                    recargo.getDescripcion(), recargo.getPorcentaje(), recargo.getValorFijo());

            // Buscar pagos que deben recibir este recargo
            aplicarRecargo(recargo);
        }

        log.info("🎯 Aplicacion de recargos completada para el día {}", diaActual);
    }

    /**
     * Aplica el recargo a los pagos pendientes del sistema.
     */
    private void aplicarRecargo(Recargo recargo) {
        List<Pago> pagosPendientes = pagoRepositorio.findPagosPendientes(EstadoPago.ACTIVO); // Debes definir este método en tu repositorio

        for (Pago pago : pagosPendientes) {
            double montoBase = pago.getMonto();
            double recargoPorcentaje = (recargo.getPorcentaje() / 100.0) * montoBase;
            double recargoFinal = recargoPorcentaje + (recargo.getValorFijo() != null ? recargo.getValorFijo() : 0.0);

            pago.setMonto(pago.getMonto() + recargoFinal); // Sumar el recargo al pago
            pagoRepositorio.save(pago);

            log.info("💰 Recargo aplicado a pago ID {}: +${} (Nuevo total: ${})",
                    pago.getId(), recargoFinal, pago.getMonto());
        }
    }
}
