package co.com.crediya.model.events.gateways;

import co.com.crediya.model.events.SolicitudEventReportes;

public interface EventPublisherReportes {
    void publishEventAsync(SolicitudEventReportes event);
}
