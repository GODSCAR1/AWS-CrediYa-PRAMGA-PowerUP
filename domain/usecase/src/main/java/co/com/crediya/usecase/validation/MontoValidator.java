package co.com.crediya.usecase.validation;

import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.exception.SolicitudValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
@RequiredArgsConstructor
public class MontoValidator implements Validator<Solicitud> {

    private final TipoPrestamoRepository tipoPrestamoRepository;
    @Override
    public Mono<Void> validate(Solicitud solicitud) {

        if (solicitud.getMonto() == null) {
            return Mono.error(new SolicitudValidationException("El monto es obligatorio"));
        }

        if(solicitud.getMonto().compareTo(BigDecimal.ZERO) <= 0){
            return Mono.error(new SolicitudValidationException("El monto debe ser mayor a cero"));
        }

        return Mono.empty();

    }
}
