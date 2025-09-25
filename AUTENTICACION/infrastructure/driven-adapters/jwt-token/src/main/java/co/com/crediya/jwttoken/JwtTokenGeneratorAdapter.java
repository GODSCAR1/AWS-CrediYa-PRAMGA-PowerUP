package co.com.crediya.jwttoken;

import co.com.crediya.model.Rol;
import co.com.crediya.model.Usuario;
import co.com.crediya.model.gateways.TokenGenerator;
import co.com.crediya.model.gateways.RolRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@RequiredArgsConstructor
@Component
@Log
public class JwtTokenGeneratorAdapter implements TokenGenerator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 horas por defecto
    private long jwtExpirationMs;

    private final RolRepository rolRepository;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    @Override
    public Mono<String> generateToken(Usuario usuario) {

        SecretKey key = getSigningKey();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return rolRepository.findById(usuario.getIdRol())
                .map(Rol::getNombre)
                .map(rolName -> {
                    String token = Jwts.builder()
                            .setSubject(usuario.getId())
                            .claim("email", usuario.getEmail())          // Email
                            .claim("role", rolName)                     // Rol
                            .setIssuedAt(now)                              // Fecha de emisión
                            .setExpiration(expiryDate)                     // Fecha de expiración
                            .signWith(key)                                 // Firmar con clave secreta
                            .compact();
                    log.info("Token generado exitosamente para usuario: " + usuario.getEmail());
                    return token;
                });


    }
}
