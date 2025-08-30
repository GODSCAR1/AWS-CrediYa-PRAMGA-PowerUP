package co.com.crediya.usecase.validation;

import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.AutenticacionExternalService;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.exception.EmailValidationException;
import co.com.crediya.usecase.exception.SolicitudValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class EmailValidator implements Validator<Solicitud> {
    private final AutenticacionExternalService autenticacionExternalService;
    @Override
    public Mono<Void> validate(Solicitud solicitud) {
        if(solicitud.getEmail() == null || solicitud.getEmail().isBlank()){
            return Mono.error(new SolicitudValidationException("El email es obligatorio"));
        }
        return this.autenticacionExternalService.validateUsuario(solicitud.getEmail())
                .then()
                .doOnError(error -> Mono.error(new EmailValidationException("El email no existe")));
    }
}
