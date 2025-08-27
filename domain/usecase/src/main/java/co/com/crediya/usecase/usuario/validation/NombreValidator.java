package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.usuario.Validator;
import reactor.core.publisher.Mono;

//TODO: Crear una excepcion personalizada
public class NombreValidator implements Validator<Usuario> {
    @Override
    public Mono<Void> validate(Usuario usuario) {
        return (usuario.getNombre() == null || usuario.getNombre().isBlank())
                ? Mono.error(new RuntimeException("El nombre es obligatorio"))
                : Mono.empty();
    }
}
