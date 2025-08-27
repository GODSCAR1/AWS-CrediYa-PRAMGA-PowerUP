package co.com.crediya.usecase.usuario.composite;

import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.usuario.Validator;
import co.com.crediya.usecase.usuario.validation.*;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public class UsuarioValidationComposite implements Validator<Usuario> {

    private final List<Validator<Usuario>> validator;

    public UsuarioValidationComposite(
            NombreValidator nombreValidator,
            ApellidoValidator apellidoValidator,
            EmailValidator emailValidator,
            DocumentoIdentidadValidator documentoIdentidadValidator,
            ContrasenaValidator contrasenaValidator,
            SalarioBaseValidator salarioBaseValidator
    ) {
        this.validator = Arrays.asList(
                nombreValidator,
                apellidoValidator,
                emailValidator,
                documentoIdentidadValidator,
                contrasenaValidator,
                salarioBaseValidator
        );
    }
    @Override
    public Mono<Void> validate(Usuario usuario) {
        return this.validator.stream()
                .map(v -> v.validate(usuario))
                .reduce(Mono.empty(), Mono::then);
    }
}
