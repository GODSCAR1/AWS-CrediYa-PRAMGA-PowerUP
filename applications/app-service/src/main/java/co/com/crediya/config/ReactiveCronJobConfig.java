package co.com.crediya.config;

import co.com.crediya.usecase.ReporteUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Log
public class ReactiveCronJobConfig {
    private final ReporteUseCase reporteUseCase;

    @Scheduled(cron = "0 0 16 * * MON-FRI", zone = "America/Bogota")
    public void enviarReporteDiario() {
        log.info("Iniciando tarea programada para enviar reporte diario");
        reporteUseCase.sendSQSEventReportesDiarios()
                .doOnNext(reporte -> log.info("Reporte diario: " + reporte))
                .doOnError(error -> log.severe("Error al obtener el reporte diario: " + error.getMessage()))
                .subscribe();
    }
}
