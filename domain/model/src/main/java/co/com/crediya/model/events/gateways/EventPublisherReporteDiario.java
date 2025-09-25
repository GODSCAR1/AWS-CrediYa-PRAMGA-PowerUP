package co.com.crediya.model.events.gateways;

import co.com.crediya.model.events.ReporteDiarioEvent;

public interface EventPublisherReporteDiario {
    void publishEventAsync(ReporteDiarioEvent event);
}
