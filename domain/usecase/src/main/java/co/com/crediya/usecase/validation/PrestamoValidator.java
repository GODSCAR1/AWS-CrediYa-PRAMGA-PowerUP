package co.com.crediya.usecase.validation;

import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.exception.SolicitudValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
@RequiredArgsConstructor
public class PrestamoValidator implements Validator<Solicitud> {
    private final TipoPrestamoRepository tipoPrestamoRepository;
    @Override
    public Mono<Void> validate(Solicitud solicitud) {
        if(solicitud.getNombreTipoPrestamo() == null || solicitud.getNombreTipoPrestamo().isBlank()){
            return Mono.error(new SolicitudValidationException("El nombre del tipo de préstamo no puede ser nulo o vacio"));
        }

        return this.tipoPrestamoRepository.findByNombre(solicitud.getNombreTipoPrestamo())
                .switchIfEmpty(Mono.error(new SolicitudValidationException("El tipo de préstamo no existe")))
                .flatMap(tipoPrestamo -> {
                    if (solicitud.getMonto().compareTo(tipoPrestamo.getMontoMinimo()) < 0
                            || solicitud.getMonto().compareTo(tipoPrestamo.getMontoMaximo()) > 0) {
                        return Mono.error(new SolicitudValidationException("El monto debe estar entre "
                                + tipoPrestamo.getMontoMinimo() + " y " + tipoPrestamo.getMontoMaximo()));
                    }
                    return Mono.empty();
                });

    }
}
