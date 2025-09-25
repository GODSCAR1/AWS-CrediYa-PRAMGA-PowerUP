package co.com.crediya.r2dbc;

import co.com.crediya.r2dbc.entity.TipoPrestamoEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface TipoPrestamoReactiveRepository extends ReactiveCrudRepository<TipoPrestamoEntity, String>, ReactiveQueryByExampleExecutor<TipoPrestamoEntity> {
    Mono<TipoPrestamoEntity> findByNombre(String nombre);
}
