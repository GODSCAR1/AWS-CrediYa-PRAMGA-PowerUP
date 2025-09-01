package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import reactor.core.publisher.Mono;

public class ContrasenaValidator implements Validator<Usuario> {
    @Override
    public Mono<Void> validate(Usuario usuario) {
        return (usuario.getContrasena() == null || usuario.getContrasena().isBlank())
                ? Mono.error(new UsuarioValidationException("La contrasena es obligatoria"))
                : Mono.empty();
    }
}
