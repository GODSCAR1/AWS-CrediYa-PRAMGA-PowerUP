package co.com.crediya.usecase.validation;

import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.exception.SolicitudValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class EmailValidator implements Validator<Solicitud> {
    @Override
    public Mono<Void> validate(Solicitud solicitud) {
        if(solicitud.getEmail() == null || solicitud.getEmail().isBlank()){
            return Mono.error(new SolicitudValidationException("El email es obligatorio"));
        }
        return Mono.empty();
    }
}
