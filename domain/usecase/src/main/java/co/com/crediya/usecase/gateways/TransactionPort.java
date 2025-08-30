package co.com.crediya.usecase.gateways;

import reactor.core.publisher.Mono;

public interface TransactionPort {
    <T> Mono<T> executeInTransaction(Mono<T> operation);
}
