package co.com.crediya.r2dbc;

import co.com.crediya.r2dbc.entity.RolEntity;
import co.com.crediya.r2dbc.entity.UsuarioEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RolReactiveRepository extends ReactiveCrudRepository<RolEntity, String>, ReactiveQueryByExampleExecutor<RolEntity> {
    Mono<RolEntity> findByNombre(String nombre);
}
