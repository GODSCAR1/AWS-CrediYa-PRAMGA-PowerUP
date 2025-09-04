package co.com.crediya.usecase.login;

import co.com.crediya.model.LoginRequest;
import co.com.crediya.model.LoginResponse;
import co.com.crediya.model.gateways.TokenGenerator;
import co.com.crediya.model.gateways.PasswordEncoder;
import co.com.crediya.model.gateways.UsuarioRepository;
import co.com.crediya.usecase.login.composite.LoginValidationComposite;
import co.com.crediya.usecase.login.exception.LoginValidationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log
public class LoginUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;
    private final LoginValidationComposite loginValidationComposite;

    public Mono<LoginResponse> login(LoginRequest loginRequest) {
        return this.loginValidationComposite.validate(loginRequest)
                .doOnError(
                        error -> log.severe(String.format("Error de validacion: %s",  error.getMessage()))
                )
                .then(Mono.defer(() ->
                    this.usuarioRepository.findByEmail(loginRequest.getEmail())
                            .switchIfEmpty(Mono.defer(() -> {
                                log.severe("Usuario no encontrado");
                                return Mono.error(new LoginValidationException("Credenciales incorrectas"));
                            }))
                            .flatMap(usuario -> {
                                if(!passwordEncoder.matches(loginRequest.getContrasena(), usuario.getContrasena())) {
                                    return Mono.error(new LoginValidationException("Credenciales incorrectas"));
                                }
                                return this.tokenGenerator.generateToken(usuario)
                                        .map(token -> LoginResponse.builder().token(token).build());
                            })
                ))
                .doOnSuccess(
                        loginResponse -> log.info(String.format("Usuario %s logueado exitosamente", loginRequest.getEmail()))
                );
    }
}
