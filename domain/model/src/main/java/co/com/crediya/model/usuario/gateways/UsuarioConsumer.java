package co.com.crediya.model.usuario.gateways;

import co.com.crediya.model.usuario.Usuario;
import reactor.core.publisher.Flux;

import java.util.List;

public interface UsuarioConsumer {
    Flux<Usuario> getUsuariosByEmails(List<String> emails);
}
