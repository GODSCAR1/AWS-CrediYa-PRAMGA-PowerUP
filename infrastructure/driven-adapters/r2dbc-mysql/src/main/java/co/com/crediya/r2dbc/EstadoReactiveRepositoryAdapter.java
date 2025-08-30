package co.com.crediya.r2dbc;

import co.com.crediya.model.estados.Estado;
import co.com.crediya.model.estados.gateways.EstadoRepository;
import co.com.crediya.r2dbc.entity.EstadoEntity;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class EstadoReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Estado/* change for domain model */,
        EstadoEntity/* change for adapter model */,
        String,
        EstadoReactiveRepository
        > implements EstadoRepository {
    public EstadoReactiveRepositoryAdapter(EstadoReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.map(d, Estado.class/* change for domain model */));
    }


    @Override
    public Mono<Estado> findByNombre(String nombre) {
        return this.repository.findByNombre(nombre).map(this::toEntity);
    }
}
