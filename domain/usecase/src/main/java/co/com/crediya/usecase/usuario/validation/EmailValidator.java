package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.model.gateways.UsuarioRepository;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class EmailValidator implements Validator<Usuario> {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9.%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern pattern = Pattern.compile(EMAIL_REGEX);

    private final UsuarioRepository usuarioRepository;
    @Override
    public Mono<Void> validate(Usuario usuario) {
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            return Mono.error(new UsuarioValidationException("El email es obligatorio"));
        }
        if (!pattern.matcher(usuario.getEmail()).matches()){
            return Mono.error(new UsuarioValidationException("El email es invalido"));
        }
        return usuarioRepository.findByEmail(usuario.getEmail())
                .flatMap(u -> Mono.error(new UsuarioValidationException("El email ya existe")))
                .cast(Void.class)
                .switchIfEmpty(Mono.empty());

    }
}
