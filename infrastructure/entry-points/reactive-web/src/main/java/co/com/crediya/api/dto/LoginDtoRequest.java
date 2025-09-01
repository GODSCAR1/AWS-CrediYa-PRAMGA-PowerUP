package co.com.crediya.api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginDtoRequest {
    private String email;

    private String contrasena;
}
