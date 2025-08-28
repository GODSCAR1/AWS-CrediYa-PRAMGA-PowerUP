package co.com.crediya.r2dbc;

import co.com.crediya.r2dbc.entity.UsuarioEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UsuarioReactiveRepository extends ReactiveCrudRepository<UsuarioEntity, String>, ReactiveQueryByExampleExecutor<UsuarioEntity> {

    Mono<UsuarioEntity> findByEmail(String email);

    Mono<UsuarioEntity> findByDocumentoIdentidad(String documentoIdentidad);
}
