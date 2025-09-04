package co.com.crediya.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
@EnableMethodSecurity
public class SecurityConfig {
    public static final String H_X_USER_EMAIL = "X-User-Email";
    public static final String H_X_USER_ROLE  = "X-User-Role";
    public static final String H_X_SERVICE_NAME = "X-Service-Name";

    private static final Set<String> ALLOWED_SERVICES = Set.of(
            "solicitudes-service"
    );

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         AuthenticationWebFilter gatewayHeaderAuthFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .addFilterAt(gatewayHeaderAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(auth -> auth
                        // públicos reales
                        .pathMatchers("/actuator/health", "/public/**", "/api/v1/login",
                                "/swagger-ui/**","/v3/api-docs/**").permitAll()
                        // endpoints privados
                        .pathMatchers("/api/v1/usuario").hasAnyRole("ADMIN", "ASESOR")
                        .pathMatchers("/api/v1/usuario/update/{email}").hasRole("ADMIN")
                        .pathMatchers("/api/v1/usuario/emails").hasRole("INTERNAL_SERVICE")
                        .anyExchange().authenticated()
                )
                .build();
    }

    @Bean
    public AuthenticationWebFilter gatewayHeaderAuthFilter(ReactiveAuthenticationManager passthroughAuthManager) {
        AuthenticationWebFilter filter = new AuthenticationWebFilter(passthroughAuthManager);
        filter.setServerAuthenticationConverter(this::convertFromGatewayHeaders);
        return filter;
    }

    @Bean
    public ReactiveAuthenticationManager passthroughAuthManager() {
        // Acepta el Authentication creado por el converter (no revalidamos JWT aquí)
        return Mono::just;
    }

    private Mono<Authentication> convertFromGatewayHeaders(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        String email  = headers.getFirst(H_X_USER_EMAIL);
        String roles  = headers.getFirst(H_X_USER_ROLE);
        String serviceName = headers.getFirst(H_X_SERVICE_NAME);

        if (email == null && roles != null && roles.contains("INTERNAL_SERVICE")) {

            if (serviceName == null || !ALLOWED_SERVICES.contains(serviceName)) {
                return Mono.empty();
            }

            email = serviceName + "@crediya.internal";
        }

        if (email == null || roles == null) {
            return Mono.empty(); // Esto permite que la request continúe sin autenticación
        }

        // Soporta uno o varios roles separados por coma
        Collection<GrantedAuthority> authorities =
                Arrays.stream(roles.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> "ROLE_" + s)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, "N/A", authorities
        );

        return Mono.just(auth);
    }

}


