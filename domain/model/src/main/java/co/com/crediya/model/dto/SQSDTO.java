package co.com.crediya.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SQSDTO {
    private String idSolicitud;
    private String estado;
    private String email;
    private String motivo;
}
