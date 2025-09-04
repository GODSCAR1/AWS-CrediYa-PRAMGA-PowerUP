package co.com.crediya.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
@EnableMethodSecurity
public class SecurityConfig {
    public static final String H_X_USER_EMAIL = "X-User-Email";
    public static final String H_X_USER_ROLE  = "X-User-Role";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         AuthenticationWebFilter gatewayHeaderAuthFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .addFilterAt(gatewayHeaderAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(auth -> auth
                        // públicos reales
                        .pathMatchers("/actuator/health", "/public/**","/swagger-ui/**",
                                "/v3/api-docs/**").permitAll()
                        .pathMatchers(HttpMethod.POST,"/api/v1/solicitud").hasRole("CLIENTE")
                        .pathMatchers(HttpMethod.GET,"/api/v1/solicitud").hasRole("ASESOR")
                        .pathMatchers(HttpMethod.GET,"/api/v1/solicitud/{nombreEstado}").hasRole("ASESOR")
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



