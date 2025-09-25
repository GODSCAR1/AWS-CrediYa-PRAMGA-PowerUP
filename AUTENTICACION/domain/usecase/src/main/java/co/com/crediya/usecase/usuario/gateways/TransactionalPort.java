package co.com.crediya.usecase.usuario.gateways;

import reactor.core.publisher.Mono;

public interface TransactionalPort {
    <T> Mono<T> executeInTransaction(Mono<T> operation);
}
