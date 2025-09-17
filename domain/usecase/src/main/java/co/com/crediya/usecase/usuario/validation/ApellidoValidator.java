package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import co.com.crediya.usecase.usuario.message.ValidationMessage;
import reactor.core.publisher.Mono;

public class ApellidoValidator implements Validator<Usuario> {
    @Override
    public Mono<Void> validate(Usuario usuario) {
        return (usuario.getApellido() == null || usuario.getApellido().isBlank())
                ? Mono.error(new UsuarioValidationException(ValidationMessage.APELLIDO_OBLIGATORIO.getMensaje()))
                : Mono.empty();
    }
}
