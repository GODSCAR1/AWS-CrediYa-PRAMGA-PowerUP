package co.com.crediya.consumer;

import co.com.crediya.model.usuario.Usuario;
import co.com.crediya.model.usuario.gateways.UsuarioConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestConsumer implements UsuarioConsumer {
    private final WebClient webClient;

    @Value("${adapter.restconsumer.usuario.searchEndpoint}")
    private String searchEndpoint;
    @Override
    public Flux<Usuario> getUsuariosByEmails(List<String> emails) {
        UsuarioEmailRequest request = UsuarioEmailRequest.builder()
                .emails(emails)
                .build();
        return webClient
                .post()
                .uri(searchEndpoint)
                .header("X-User-Role", "INTERNAL_SERVICE")
                .header("X-Service-Name", "solicitudes-service")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(UsuarioEmailResponse.class)
                .map(usuarioEmailResponse -> Usuario.builder()
                                .email(usuarioEmailResponse.getEmail())
                                .nombre(usuarioEmailResponse.getNombre())
                                .salarioBase(usuarioEmailResponse.getSalarioBase())
                                .build());
    }
}
