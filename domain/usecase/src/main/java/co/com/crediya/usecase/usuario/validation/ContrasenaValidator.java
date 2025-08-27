package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.usuario.Validator;
import reactor.core.publisher.Mono;
//TODO: Crear una excepcion personalizada
public class ContrasenaValidator implements Validator<Usuario> {
    @Override
    public Mono<Void> validate(Usuario usuario) {
        return (usuario.getContrasena() == null || usuario.getContrasena().isBlank())
                ? Mono.error(new RuntimeException())
                : Mono.empty();
    }
}
