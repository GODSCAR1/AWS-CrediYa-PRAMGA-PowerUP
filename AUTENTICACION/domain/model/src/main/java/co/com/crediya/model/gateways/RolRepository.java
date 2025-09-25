package co.com.crediya.model.gateways;

import co.com.crediya.model.Rol;
import reactor.core.publisher.Mono;

public interface RolRepository {
    Mono<Rol> findByNombre(String nombre);

    Mono<Rol> findById(String id);
}
