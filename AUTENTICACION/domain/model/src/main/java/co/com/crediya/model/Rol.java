package co.com.crediya.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Rol {
    private String id;
    private String nombre;
    private String descripcion;
}
