package co.com.crediya.model.events.gateways;

import co.com.crediya.model.events.SolicitudEventNotificaciones;

public interface EventPublisherNotificaciones {
    void publishEventAsync(SolicitudEventNotificaciones event);
}
