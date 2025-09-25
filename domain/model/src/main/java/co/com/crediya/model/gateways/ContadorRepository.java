package co.com.crediya.model.gateways;

import co.com.crediya.model.ContadorGlobal;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface ContadorRepository {
    Mono<ContadorGlobal> getById(String id);
    Mono<ContadorGlobal> save(ContadorGlobal contadorGlobal);
}
