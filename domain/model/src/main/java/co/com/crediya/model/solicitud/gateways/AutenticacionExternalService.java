package co.com.crediya.model.solicitud.gateways;

import reactor.core.publisher.Mono;

public interface AutenticacionExternalService {

    Mono<Boolean> validateUsuario(String documentoIdentidad);
}
