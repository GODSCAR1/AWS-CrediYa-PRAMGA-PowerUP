package co.com.crediya.usecase.validation;

import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.exception.SolicitudValidationException;
import reactor.core.publisher.Mono;

public class PlazoValidator implements Validator<Solicitud> {
    @Override
    public Mono<Void> validate(Solicitud solicitud) {
        return (solicitud.getPlazo() == null || solicitud.getPlazo() <= 0)
                ? Mono.error(new SolicitudValidationException("El plazo debe ser mayor a 0"))
                : Mono.empty();
    }
}
