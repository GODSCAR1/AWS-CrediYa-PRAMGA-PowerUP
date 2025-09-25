package co.com.crediya.api;

import co.com.crediya.api.config.ReportePath;
import co.com.crediya.api.dto.ContadorGlobalDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class RouterRest {
    private final ReportePath reportePath;
    private final Handler reporteHandler;
    @Bean
    @RouterOperations({
            @RouterOperation(
                    operation = @Operation(
                            operationId = "getReporte",
                            summary = "Obtiene el reporte",
                            description = "Obtiene el reporte con el total de solicitudes aprobadas",
                            tags = {"Reporte"},
                            security = @SecurityRequirement(name = "bearerAuth"),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Reporte obtenido exitosamente",
                                            content = {
                                                    @Content(
                                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                            schema = @Schema(implementation = ContadorGlobalDTO.class)
                                                    )
                                            }
                                    )}),
                    path = "/api/v1/reporte",
                    method = RequestMethod.GET)

    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(GET(reportePath.getGetReporte()), reporteHandler::listenGetReporte);
    }
}
