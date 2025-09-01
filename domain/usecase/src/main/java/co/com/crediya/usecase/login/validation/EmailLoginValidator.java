package co.com.crediya.usecase.login.validation;

import co.com.crediya.model.LoginRequest;
import co.com.crediya.model.gateways.UsuarioRepository;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.login.exception.LoginValidationException;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class  EmailLoginValidator implements Validator<LoginRequest> {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9.%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern pattern = Pattern.compile(EMAIL_REGEX);

    @Override
    public Mono<Void> validate(LoginRequest loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getEmail().isBlank()) {
            return Mono.error(new LoginValidationException("El email es obligatorio"));
        }
        if (!pattern.matcher(loginRequest.getEmail()).matches()){
            return Mono.error(new LoginValidationException("El email es invalido"));
        }
        return Mono.empty();
    }
}
