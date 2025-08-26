package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.usuario.Validator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

//TODO: Implimentar excepción personalizada.
@Component
public class SalarioBaseValidator implements Validator<Usuario> {
    private static final BigDecimal SALARIO_BASE_MINIMO = BigDecimal.ZERO;
    private static final BigDecimal SALARIO_BASE_MAXIMO = BigDecimal.valueOf(15000000);
    @Override
    public Mono<Void> validate(Usuario usuario) {
        if(usuario.getSalarioBase() == null){
            return Mono.error(new RuntimeException());
        }
        if(usuario.getSalarioBase().compareTo(SALARIO_BASE_MINIMO) < 0){
            return Mono.error(new RuntimeException());
        }
        if(usuario.getSalarioBase().compareTo(SALARIO_BASE_MAXIMO) > 0){
            return Mono.error(new RuntimeException());
        }
        return Mono.empty();
    }
}
