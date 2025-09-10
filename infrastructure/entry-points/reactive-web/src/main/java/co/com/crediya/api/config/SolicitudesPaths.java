package co.com.crediya.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "routes.paths")
public class SolicitudesPaths {
    private String createSolicitud;

    private String getSolicitudes;

    private String aproveSolicitud;

    private String rejectSolicitud;
}
