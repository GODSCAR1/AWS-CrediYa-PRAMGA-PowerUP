package co.com.crediya.usecase.composite;

import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.validation.MontoValidator;
import co.com.crediya.usecase.validation.PlazoValidator;
import co.com.crediya.usecase.validation.PrestamoValidator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public class SolicitudValidationComposite implements Validator<Solicitud> {

    private final List<Validator<Solicitud>> validator;


    public SolicitudValidationComposite(
            MontoValidator montoValidator,
            PlazoValidator plazoValidator,
            PrestamoValidator prestamoValidator
    ) {
        this.validator = Arrays.asList(
                montoValidator,
                plazoValidator,
                prestamoValidator
                );
    }
    @Override
    public Mono<Void> validate(Solicitud solicitud) {
        return Flux.fromIterable(this.validator)
                .flatMap(v -> v.validate(solicitud), 1)
                .then();
    }
}
