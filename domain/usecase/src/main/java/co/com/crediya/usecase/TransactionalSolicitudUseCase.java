package co.com.crediya.usecase;

import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.usecase.gateways.TransactionPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class TransactionalSolicitudUseCase {
    private final SolicitudUseCase solicitudUseCase;
    private final TransactionPort transactionPort;

    public Mono<Solicitud> createSolicitudTransactional(Solicitud solicitud, String emailHeader) {
        return this.transactionPort.executeInTransaction(
                this.solicitudUseCase.createSolicitud(solicitud, emailHeader)
        );
    }

    public Mono<Solicitud> handleSolicitudManualTransactional(String id, Boolean aprobado) {
        return this.transactionPort.executeInTransaction(
                this.solicitudUseCase.handleSolicitudManual(id, aprobado)
        );
    }
}
