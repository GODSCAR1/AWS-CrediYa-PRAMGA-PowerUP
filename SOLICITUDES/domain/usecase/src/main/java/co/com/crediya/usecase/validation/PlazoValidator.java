package co.com.crediya.usecase.validation;

import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.exception.SolicitudValidationException;
import co.com.crediya.usecase.message.ValidationMessage;
import reactor.core.publisher.Mono;

public class PlazoValidator implements Validator<Solicitud> {
    @Override
    public Mono<Void> validate(Solicitud solicitud) {
        return (solicitud.getPlazo() == null || solicitud.getPlazo() <= 0)
                ? Mono.error(new SolicitudValidationException(ValidationMessage.PLAZO_DEBE_SER_MAYOR_A_CERO.getMensaje()))
                : Mono.empty();
    }
}
