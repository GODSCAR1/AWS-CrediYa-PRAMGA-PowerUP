package co.com.crediya.api;

import co.com.crediya.api.config.SolicitudesPaths;
import co.com.crediya.api.dto.CreateSolicitudRequest;
import co.com.crediya.api.dto.PagedResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.awt.*;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class RouterRest {
    private final SolicitudesPaths solicitudesPaths;
    private final Handler solicitudesHandler;
    @Bean
    @RouterOperations({
            @RouterOperation(
                    operation = @Operation(
                            operationId = "createSolicitud",
                            summary = "Crear nueva solicitud",
                            description = "Crea una nueva solicitud en el sistema con las respectivas validaciones",
                            tags = {"Solicitudes"},
                            requestBody = @RequestBody(
                                    required = true,
                                    description = "Datos de la solicitud a crear",
                                    content = @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = CreateSolicitudRequest.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Usuario creado exitosamente",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = CreateSolicitudRequest.class)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Error en la validacion de datos"
                                    )
                            }
                    ),
                    path = "/api/v1/solicitud",
                    method = RequestMethod.POST
            ),
            @RouterOperation(
                    operation = @Operation(
                            operationId = "getSolicitudes",
                            summary = "Obtener solicitudes paginadas",
                            description = "Obtiene una lista paginada de solicitudes que requieren revision manual",
                            tags = {"Solicitudes"},
                            parameters = {
                                    @Parameter(
                                            name = "page",
                                            description = "Número de página (por defecto 0)",
                                            required = false
                                    ),
                                    @Parameter(
                                            name = "size",
                                            description = "Tamaño de página (por defecto 20)",
                                            required = false
                                    ),
                                    @Parameter(
                                            name = "sortBy",
                                            description = "Campo para ordenar (por defecto 'monto')",
                                            required = false
                                    ),
                                    @Parameter(
                                            name = "sortDirection",
                                            description = "Dirección de ordenamiento ('asc' o 'desc', por defecto 'desc')",
                                            required = false
                                    )
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Lista de solicitudes paginadas",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = PagedResponseDTO.class)
                                            )
                                    )
                            }
                    ),
                    path = "/api/v1/solicitud/{nombreEstado}",
                    method = RequestMethod.GET
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST(solicitudesPaths.getCreateSolicitud()), solicitudesHandler::listenCreateSolicitud)
                .andRoute(GET(solicitudesPaths.getGetSolicitudes()), solicitudesHandler::obtenerSolicitudes);
    }
}
