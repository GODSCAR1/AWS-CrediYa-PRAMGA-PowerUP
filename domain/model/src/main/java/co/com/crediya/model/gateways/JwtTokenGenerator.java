package co.com.crediya.model.gateways;

import co.com.crediya.model.Usuario;
import reactor.core.publisher.Mono;

public interface JwtTokenGenerator {
    Mono<String> generateToken(Usuario usuario);
}
