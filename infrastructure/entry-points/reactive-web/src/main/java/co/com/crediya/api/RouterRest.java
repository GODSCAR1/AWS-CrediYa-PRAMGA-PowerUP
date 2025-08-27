package co.com.crediya.api;

import co.com.crediya.api.config.AutenticacionPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST(autenticacionPaths.getCreateUsuario()), userHandler::listenCreateUsuario);
    }
}
