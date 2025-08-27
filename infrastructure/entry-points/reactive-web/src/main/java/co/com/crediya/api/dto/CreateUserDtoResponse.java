package co.com.crediya.api.dto;

import java.math.BigDecimal;
import java.util.Date;

public record CreateUserDtoResponse(
        String nombre,
        String apellido,
        String email,
        String documentoIdentidad,
        String telefono,
        String idRol,
        BigDecimal salarioBase,
        Date fechaNacimiento
){

}
