package co.com.crediya.usecase.usuario;

import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.usuario.gateways.TransactionalPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class TransactionalUsuarioUseCase {
    private final UsuarioUseCase usuarioUseCase;
    private final TransactionalPort transactionalPort;

    public Mono<Usuario> createUsuarioTransactional(Usuario usuario) {
        return this.transactionalPort.executeInTransaction(
                this.usuarioUseCase.createUsuario(usuario)
        );
    }
}
