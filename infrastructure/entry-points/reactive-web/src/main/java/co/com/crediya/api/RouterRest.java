package co.com.crediya.api;

import co.com.crediya.api.config.AutenticacionPaths;
import co.com.crediya.api.dto.CreateUsuarioDtoRequest;
import co.com.crediya.api.dto.CreateUsuarioDtoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class RouterRest {
    private final AutenticacionPaths autenticacionPaths;
    private final Handler userHandler;
    @Bean
    @RouterOperation(
            operation = @Operation(
                    operationId = "createUsuario",
                    summary = "Crear nuevo usuario",
                    description = "Crea un nuevo usuario en el sistema con las respectivas validaciones",
                    tags = {"Usuarios"},
                    requestBody = @RequestBody(
                            required = true,
                            description = "Datos del usuario a crear",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CreateUsuarioDtoRequest.class)
                            )
                    ),
                    responses = {
                            @ApiResponse(
                                    responseCode = "200",
                                    description = "Usuario creado exitosamente",
                                    content = @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = CreateUsuarioDtoResponse.class)
                                    )
                            ),
                            @ApiResponse(
                                    responseCode = "400",
                                    description = "Error en la validacion de datos"
                            )
                    }
            )
    )
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST(autenticacionPaths.getCreateUsuario()), userHandler::listenCreateUsuario);
    }
}
