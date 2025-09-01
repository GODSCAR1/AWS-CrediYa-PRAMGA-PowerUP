package co.com.crediya.usecase.login.composite;

import co.com.crediya.model.LoginRequest;

import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.login.validation.ContrasenaLoginValidator;
import co.com.crediya.usecase.login.validation.EmailLoginValidator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public class LoginValidationComposite implements Validator<LoginRequest> {
    private final List<Validator<LoginRequest>> validator;

    public LoginValidationComposite(EmailLoginValidator emailLoginValidator,
                                    ContrasenaLoginValidator contrasenaLoginValidator) {
        this.validator = Arrays.asList(emailLoginValidator, contrasenaLoginValidator);
    }
    @Override
    public Mono<Void> validate(LoginRequest loginRequest) {
        return Flux.fromIterable(this.validator)
                .flatMap(v -> v.validate(loginRequest), 1)
                .then();
    }
}
