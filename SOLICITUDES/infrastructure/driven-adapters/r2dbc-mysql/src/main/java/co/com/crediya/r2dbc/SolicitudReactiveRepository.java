package co.com.crediya.r2dbc;

import co.com.crediya.r2dbc.entity.SolicitudEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SolicitudReactiveRepository extends ReactiveCrudRepository<SolicitudEntity, String>, ReactiveQueryByExampleExecutor<SolicitudEntity> {

    @Query("SELECT s.* FROM solicitud s " +
            "JOIN estados e ON e.id_estado = s.id_estado " +
            "WHERE s.email = :email AND e.nombre = :estadoNombre")
    Flux<SolicitudEntity> findByEmailAndEstadoNombre(String email, String estadoNombre);


}
