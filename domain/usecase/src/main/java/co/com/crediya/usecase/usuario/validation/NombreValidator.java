package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import reactor.core.publisher.Mono;

public class NombreValidator implements Validator<Usuario> {
    @Override
    public Mono<Void> validate(Usuario usuario) {
        return (usuario.getNombre() == null || usuario.getNombre().isBlank())
                ? Mono.error(new UsuarioValidationException("El nombre es obligatorio"))
                : Mono.empty();
    }
}
