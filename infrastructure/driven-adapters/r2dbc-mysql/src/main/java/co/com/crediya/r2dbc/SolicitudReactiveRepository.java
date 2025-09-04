package co.com.crediya.r2dbc;

import co.com.crediya.r2dbc.entity.SolicitudEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface SolicitudReactiveRepository extends ReactiveCrudRepository<SolicitudEntity, String>, ReactiveQueryByExampleExecutor<SolicitudEntity> {

}
