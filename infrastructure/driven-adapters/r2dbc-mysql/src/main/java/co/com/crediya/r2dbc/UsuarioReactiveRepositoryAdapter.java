package co.com.crediya.r2dbc;

import co.com.crediya.model.Usuario;
import co.com.crediya.model.gateways.UsuarioRepository;
import co.com.crediya.r2dbc.entity.UsuarioEntity;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class UsuarioReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Usuario/* change for domain model */,
        UsuarioEntity/* change for adapter model */,
        String,
        UsuarioReactiveRepository
> implements UsuarioRepository {
    public UsuarioReactiveRepositoryAdapter(UsuarioReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.map(d, Usuario.class));
    }
    @Override
    public Mono<Usuario> save(Usuario usuario){
        return super.save(usuario);
    }

    @Override
    public Mono<Usuario> findByEmail(String email){
        return repository.findByEmail(email).map(this::toEntity);
    }
}
