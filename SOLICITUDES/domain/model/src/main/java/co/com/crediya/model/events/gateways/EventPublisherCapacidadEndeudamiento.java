package co.com.crediya.model.events.gateways;

import co.com.crediya.model.events.SolicitudEventCapacidadEndeudamiento;
import co.com.crediya.model.events.SolicitudEventNotificaciones;

public interface EventPublisherCapacidadEndeudamiento {
    void publishEventAsync(SolicitudEventCapacidadEndeudamiento event);
}
