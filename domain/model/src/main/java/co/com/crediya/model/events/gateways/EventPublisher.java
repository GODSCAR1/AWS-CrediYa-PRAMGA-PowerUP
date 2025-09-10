package co.com.crediya.model.events.gateways;

import co.com.crediya.model.events.SolicitudEvent;

public interface EventPublisher {
    void publishEventAsync(SolicitudEvent event);
}
