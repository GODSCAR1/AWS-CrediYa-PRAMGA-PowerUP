package co.com.crediya.usecase;

import reactor.core.publisher.Mono;
@FunctionalInterface
public interface Validator<T> {
    Mono<Void> validate (T object);
}
