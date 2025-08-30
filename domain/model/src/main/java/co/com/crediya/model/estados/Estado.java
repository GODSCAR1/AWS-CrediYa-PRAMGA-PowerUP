package co.com.crediya.model.estados;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Estado {
    private String id;
    private String nombre;
    private String descripcion;
}
