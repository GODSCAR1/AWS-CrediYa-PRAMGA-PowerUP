package co.com.crediya.usecase.login.validation;

import co.com.crediya.model.LoginRequest;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.login.exception.LoginValidationException;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import reactor.core.publisher.Mono;

public class ContrasenaLoginValidator implements Validator<LoginRequest> {
    @Override
    public Mono<Void> validate(LoginRequest loginRequest) {
        return (loginRequest.getContrasena() == null || loginRequest.getContrasena().isBlank())
                ? Mono.error(new LoginValidationException("La contrasena es obligatoria"))
                : Mono.empty();
    }
}
