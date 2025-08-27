package co.com.crediya.api.dto;

import java.math.BigDecimal;
import java.util.Date;

public record CreateUserDtoRequest(
        String nombre,
        String apellido,
        String email,
        String contrasena,
        String documentoIdentidad,
        String telefono,
        BigDecimal salarioBase,
        Date fechaNacimiento
) {
}
