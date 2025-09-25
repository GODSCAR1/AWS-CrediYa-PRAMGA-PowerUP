package co.com.crediya.api;

import co.com.crediya.api.config.AutenticacionPaths;
import co.com.crediya.api.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
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
                            security = @SecurityRequirement(name = "bearerAuth"),
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
                            operationId = "getAllUsuariosByEmails",
                            summary = "Obtener usuarios por emails",
                            description = "Obtiene una lista de usuarios basados en una lista de emails proporcionada",
                            tags = {"Usuarios"},
                            requestBody = @RequestBody(
                                    required = true,
                                    description = "Lista de emails para buscar usuarios",
                                    content = @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = UsuarioEmailRequest.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Usuarios encontrados exitosamente",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = UsuarioEmailResponse.class)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Error en la validacion de datos"
                                    )
                            }
                    ),
                    path = "/api/v1/usuario/emails",
                    method = RequestMethod.POST
            ),
            @RouterOperation(
                    operation = @Operation(
                            operationId = "login",
                            summary = "Login de usuario",
                            description = "Autentica a un usuario y devuelve un token JWT si las credenciales son correctas",
                            tags = {"Autenticación"},
                            requestBody = @RequestBody(
                                    required = true,
                                    description = "Credenciales de login",
                                    content = @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = LoginDtoRequest.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Login exitoso, token JWT generado",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = LoginDtoResponse.class)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "401",
                                            description = "Credenciales incorrectas"
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Error en la validacion de datos"
                                    )
                            }
                    ),
                    path = "/api/v1/login",
                    method = RequestMethod.POST
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST(autenticacionPaths.getCreateUsuario()), userHandler::listenCreateUsuario)
                .andRoute(POST(autenticacionPaths.getSearchUsuarioByEmails()), userHandler::listenSearchUsuariosByEmails)
                .andRoute(POST(autenticacionPaths.getLogin()), userHandler::listenLogin);
    }

}
