package co.com.crediya.api;

import co.com.crediya.api.dto.ContadorGlobalDTO;
import co.com.crediya.usecase.ReporteUseCase;
import org.reactivecommons.utils.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;


@Component
@RequiredArgsConstructor
public class Handler {
    private final ReporteUseCase reporteUseCase;
    private final ObjectMapper objectMapper;

    public Mono<ServerResponse> listenGetReporte(ServerRequest serverRequest) {
        return reporteUseCase.getReporte()
                .flatMap(reporte -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(objectMapper.map(reporte, ContadorGlobalDTO.class))
                );
    }

}
