package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.usuario.Validator;
import reactor.core.publisher.Mono;

//TODO: Crear una excepcion personalizada
public class ApellidoValidator implements Validator<Usuario> {
    @Override
    public Mono<Void> validate(Usuario usuario) {
        return (usuario.getApellido() == null || usuario.getApellido().isBlank())
                ? Mono.error(new RuntimeException())
                : Mono.empty();
    }
}
