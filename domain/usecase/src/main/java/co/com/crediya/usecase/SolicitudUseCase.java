package co.com.crediya.usecase;

import co.com.crediya.model.estados.Estado;
import co.com.crediya.model.estados.gateways.EstadoRepository;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import co.com.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.crediya.usecase.composite.SolicitudValidationComposite;
import co.com.crediya.usecase.exception.EstadoValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log
public class SolicitudUseCase {
    private final SolicitudValidationComposite solicitudValidationComposite;
    private final SolicitudRepository solicitudRepository;
    private final EstadoRepository estadoRepository;
    private final TipoPrestamoRepository tipoPrestamoRepository;
    public Mono<Solicitud> createSolicitud(Solicitud solicitud){
        return this.solicitudValidationComposite.validate(solicitud)
                .doOnError(
                        error -> log.warning("Error en la validacion de la solicitud: " + error.getMessage())
                )
                .then(Mono.defer(() -> {

                    Mono<String> estadoMono = this.estadoRepository.findByNombre("Pendiente de revision")
                            .switchIfEmpty(Mono.defer(() -> {
                                log.warning("No se encontró el estado 'Pendiente de revision'");
                                return Mono.error(new EstadoValidationException("El estado 'Pendiente de revision' no existe"));
                            }))
                            .map(Estado::getId);

                    Mono<String> tipoPrestamoMono = this.tipoPrestamoRepository.findByNombre(solicitud.getNombreTipoPrestamo())
                            .map(TipoPrestamo::getId);
                    
                    return Mono.zip(estadoMono, tipoPrestamoMono)
                            .flatMap(tuple -> {
                                String estadoId = tuple.getT1();
                                String tipoPrestamoId = tuple.getT2();
                                solicitud.setIdEstado(estadoId);
                                solicitud.setIdTipoPrestamo(tipoPrestamoId);
                                return this.solicitudRepository.save(solicitud)
                                        .map(solicitudGuardada -> {
                                            solicitudGuardada.setNombreTipoPrestamo(solicitud.getNombreTipoPrestamo());
                                            return solicitudGuardada;
                                        });
                            })
                            .doOnSuccess(solicitudGuardada ->
                                    log.info(String.format("Solicitud %s creada exitosamente", solicitudGuardada.getId())));

                }));
    }
}
