package co.com.crediya.api;

import co.com.crediya.api.config.AutenticacionPaths;
import co.com.crediya.api.dto.CreateUsuarioDtoRequest;
import co.com.crediya.api.dto.CreateUsuarioDtoResponse;
import co.com.crediya.api.dto.SearchUsuarioResponse;
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

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class RouterRest {
    private final AutenticacionPaths autenticacionPaths;
    private final Handler userHandler;
    @Bean
    @RouterOperations({
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
                                            responseCode = "201",
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
                    ),
                    path = "/api/v1/usuario",
                    method = RequestMethod.POST
            ),
    @RouterOperation(
            operation = @Operation(
                    operationId = "searchUsuario",
                    summary = "Buscar usuario por documento de identidad",
                    description = "Busca un usuario por documento de identidad y devuelve true si existe o mensaje de error si no existe",
                    tags = {"Usuarios"},
                    parameters = {
                            @Parameter(
                                    name = "documentoIdentidad",
                                    description = "documento de ientidad del usuario",
                                    required = true,
                                    example = "12345678",
                                    schema = @Schema(type = "string")
                            )
                    },
                    responses = {
                            @ApiResponse(
                                    responseCode = "200",
                                    description = "Usuario encontrado exitosamente",
                                    content = @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = SearchUsuarioResponse.class)
                                    )
                            ),
                            @ApiResponse(
                                    responseCode = "404",
                                    description = "Usuario no encontrado"
                            ),
                            @ApiResponse(
                                    responseCode = "400",
                                    description = "Documento de identidad no proporcionado o invalido"
                            )
                    }
            ),
            path = "/api/v1/usuario/{documentoIdentidad}",
            method = RequestMethod.GET
        )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST(autenticacionPaths.getCreateUsuario()), userHandler::listenCreateUsuario)
        .andRoute(GET(autenticacionPaths.getSearchUsuario()), userHandler::listenSearchUsuario);
    }

}
