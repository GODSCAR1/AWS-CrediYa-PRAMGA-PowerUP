package co.com.crediya.usecase;

import co.com.crediya.model.ContadorGlobal;
import co.com.crediya.model.dto.SQSDTO;
import co.com.crediya.model.events.ReporteDiarioEvent;
import co.com.crediya.model.events.gateways.EventPublisherReporteDiario;
import co.com.crediya.model.gateways.ContadorRepository;
import co.com.crediya.usecase.messages.ReporteMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;
@RequiredArgsConstructor
@Log
public class ReporteUseCase {
    private final ContadorRepository contadorRepository;
    private final EventPublisherReporteDiario eventPublisherReporteDiario;
    private static final String GLOBAL_COUNTERS_ID = "GLOBAL_COUNTERS";
    public Mono<ContadorGlobal> getReporte(){
        log.info(ReporteMessage.OBTENIENDO_REPORTE.getMensaje());
        return this.contadorRepository.getById(GLOBAL_COUNTERS_ID)
                .switchIfEmpty(Mono.defer(() -> {
                    ContadorGlobal inicial = ContadorGlobal.createInitial();
                    return this.contadorRepository.save(inicial);
                }));
    }

    public Mono<Void> processSQSEventReportes(SQSDTO sqsdto){
        log.info(ReporteMessage.INCREMENTANDO_CONTADORES.getMensaje());
        return this.contadorRepository.getById(GLOBAL_COUNTERS_ID)
                .map(contadorExistente -> {
                    log.info("Contador existente encontrado: " + contadorExistente);
                    contadorExistente.setCantidadPrestamosAprobados(
                            contadorExistente.getCantidadPrestamosAprobados() + 1);
                    contadorExistente.setCantidadTotalPrestada(
                            contadorExistente.getCantidadTotalPrestada().add(sqsdto.getMonto()));
                    return contadorExistente;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("No existe contador, creando con datos del SQS");
                    ContadorGlobal nuevo = ContadorGlobal.createInitial();
                    nuevo.setCantidadPrestamosAprobados(1L);
                    nuevo.setCantidadTotalPrestada(sqsdto.getMonto());
                    return Mono.just(nuevo);
                }))
                .flatMap(this.contadorRepository::save)
                .doOnSuccess(guardado -> log.info("Contador actualizado: " + guardado))
                .then();
    }

    public Mono<Void> sendSQSEventReportesDiarios(){
        return this.getReporte().map(
                contadorGlobal -> ReporteDiarioEvent.builder()
                        .cantidadPrestamosAprobados(contadorGlobal.getCantidadPrestamosAprobados())
                        .cantidadTotalPrestada(contadorGlobal.getCantidadTotalPrestada())
                        .build()
        ).doOnNext(eventPublisherReporteDiario::publishEventAsync)
                .doOnError(error -> log.severe(ReporteMessage.ERRROR_ENVIANDO_REPORTE_DIARIO.getMensaje() + error.getMessage()))
                .doOnSuccess(unused -> log.info(ReporteMessage.REPORTE_DIARIO_ENVIADO_EXITOSAMENTE.getMensaje()))
                .then();
    }
}
