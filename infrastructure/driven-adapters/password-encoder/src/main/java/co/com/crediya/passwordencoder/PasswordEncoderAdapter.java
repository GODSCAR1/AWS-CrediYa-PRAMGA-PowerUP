package co.com.crediya.passwordencoder;

import co.com.crediya.model.gateways.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoderAdapter implements PasswordEncoder {

    private final BCryptPasswordEncoder encoder;

    public PasswordEncoderAdapter(@Value("${app.security.bcrypt-strength}") int strength) {
        this.encoder = new BCryptPasswordEncoder(strength);
    }
    @Override
    public String encode(String rawPassword) {
        return this.encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return this.encoder.matches(rawPassword, encodedPassword);
    }
}
