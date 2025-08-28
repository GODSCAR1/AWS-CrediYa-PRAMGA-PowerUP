package co.com.crediya.r2dbc;

import co.com.crediya.usecase.usuario.gateways.TransactionalPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TransactionalAdapter implements TransactionalPort {

    private final TransactionalOperator transactionalOperator;
    @Override
    public <T> Mono<T> executeInTransaction(Mono<T> operation) {
        return operation.as(transactionalOperator::transactional);
    }
}
