package co.com.crediya.sqs.listener;

import co.com.crediya.model.dto.SQSDTO;
import co.com.crediya.sqs.listener.message.SQSMessage;
import co.com.crediya.usecase.ReporteUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.function.Function;

@Service
@Log
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {
    private final ObjectMapper objectMapper;
    private final ReporteUseCase reporteUseCase;

    @Override
    public Mono<Void> apply(Message message) {
        log.info(String.format(SQSMessage.MENSAJE_RECIBIDO.getMensaje(), message.body()));
        return Mono.fromCallable(() -> objectMapper.readValue(message.body(), SQSDTO.class))
                .flatMap(reporteUseCase::processSQSEventReportes)
                .doOnSuccess(unused -> log.info(SQSMessage.MENSAJE_PROCESADO.getMensaje()))
                .doOnError(error -> log.severe(SQSMessage.ERROR_PROCESANDO_MENSAJE.getMensaje()))
                .then();
    }
}
