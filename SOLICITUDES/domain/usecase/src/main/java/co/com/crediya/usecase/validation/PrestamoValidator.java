package co.com.crediya.usecase.validation;

import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.exception.SolicitudValidationException;
import co.com.crediya.usecase.message.ValidationMessage;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
@RequiredArgsConstructor
public class PrestamoValidator implements Validator<Solicitud> {
    private final TipoPrestamoRepository tipoPrestamoRepository;
    @Override
    public Mono<Void> validate(Solicitud solicitud) {
        if(solicitud.getNombreTipoPrestamo() == null || solicitud.getNombreTipoPrestamo().isBlank()){
            return Mono.error(new SolicitudValidationException(ValidationMessage.NOMBRE_TIPO_PRESTAMO_OBLIGATORIO.getMensaje()));
        }

        return this.tipoPrestamoRepository.findByNombre(solicitud.getNombreTipoPrestamo())
                .switchIfEmpty(Mono.error(new SolicitudValidationException(ValidationMessage.TIPO_PRESTAMO_NO_ENCONTRADO.getMensaje())))
                .flatMap(tipoPrestamo -> {
                    if (solicitud.getMonto().compareTo(tipoPrestamo.getMontoMinimo()) < 0
                            || solicitud.getMonto().compareTo(tipoPrestamo.getMontoMaximo()) > 0) {
                        return Mono.error(new SolicitudValidationException(String.format(ValidationMessage.MONTO_DEBE_ESTAR_ENTRE.getMensaje(),
                                tipoPrestamo.getMontoMinimo(), tipoPrestamo.getMontoMaximo())));
                    }
                    return Mono.empty();
                });

    }
}
