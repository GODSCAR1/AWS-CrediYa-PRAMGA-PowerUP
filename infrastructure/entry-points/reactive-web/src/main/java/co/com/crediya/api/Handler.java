package co.com.crediya.api;

import co.com.crediya.api.dto.*;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.usecase.SolicitudUseCase;
import co.com.crediya.usecase.TransactionalSolicitudUseCase;
import lombok.RequiredArgsConstructor;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;



@Component
@RequiredArgsConstructor
public class Handler {
    private final TransactionalSolicitudUseCase transactionalSolicitudUseCase;
    private final SolicitudUseCase solicitudUseCase;
    private final ObjectMapper objectMapper;
    public Mono<ServerResponse> listenCreateSolicitud(ServerRequest serverRequest) {
        String emailHeader = serverRequest.headers().firstHeader("X-User-Email");
        return serverRequest.bodyToMono(CreateSolicitudRequest.class)
                .flatMap(request ->
                            Mono.just(objectMapper.map(request, Solicitud.class)))
                .flatMap(s -> this.transactionalSolicitudUseCase.createSolicitudTransactional(s, emailHeader))
                .flatMap(solicitudCreada ->
                        ServerResponse.created(serverRequest.uri())
                                .bodyValue(objectMapper.map(solicitudCreada, CreateSolicitudResponse.class))
                );
    }

    public Mono<ServerResponse> obtenerSolicitudes(ServerRequest request) {

        String nombreEstado = request.pathVariable("nombreEstado");

        int page = request.queryParam("page")
                .map(Integer::parseInt)
                .orElse(0);

        int size = request.queryParam("size")
                .map(Integer::parseInt)
                .orElse(20);

        String sortBy = request.queryParam("sortBy")
                .orElse("monto");

        String sortDirection = request.queryParam("sortDirection")
                .orElse("desc");

        return solicitudUseCase.getSolicitudPaged(
                        nombreEstado,page, size, sortBy, sortDirection)
                .map(pagedSolicitud -> PagedResponseDTO.<SolicitudInfoDTO>builder()
                        .content(pagedSolicitud.getContent().stream()
                                .map( p -> objectMapper.map(p, SolicitudInfoDTO.class))
                                .toList())
                        .metadata(PagedResponseDTO.PageMetadata.builder()
                                .page(pagedSolicitud.getPageNumber())
                                .size(pagedSolicitud.getPageSize())
                                .totalElements(pagedSolicitud.getTotalElements())
                                .totalPages(pagedSolicitud.getTotalPages())
                                .first(pagedSolicitud.isFirst())
                                .last(pagedSolicitud.isLast())
                                .build())
                        .build())
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> aprobarSolicitud(ServerRequest request) {
        String idSolicitud = request.pathVariable("id");

        return transactionalSolicitudUseCase.handleSolicitudManualTransactional(idSolicitud, Boolean.TRUE)
                .flatMap(solicitudAprobada ->
                        ServerResponse.ok()
                                .bodyValue(objectMapper.map(solicitudAprobada, SolicitudDTO.class))
                );
    }

    public Mono<ServerResponse> rechazarSolicitud(ServerRequest request) {
        String idSolicitud = request.pathVariable("id");

        return transactionalSolicitudUseCase.handleSolicitudManualTransactional(idSolicitud, Boolean.FALSE)
                .flatMap(solicitudRechazada ->
                        ServerResponse.ok()
                                .bodyValue(objectMapper.map(solicitudRechazada, SolicitudDTO.class))
                );
    }
}
