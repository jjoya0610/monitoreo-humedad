package com.agro.humedad.concurrencia;

import com.agro.humedad.dominio.Lectura;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
@Component
public class FlujoLecturas {
    private final SubmissionPublisher<Lectura> publicador =
            new SubmissionPublisher<>(Executors.newVirtualThreadPerTaskExecutor(), Flow.defaultBufferSize());
    public void publicar(Lectura lectura) {
        publicador.submit(lectura);
    }
    public void suscribir(Flow.Subscriber<Lectura> suscriptor) {
        publicador.subscribe(suscriptor);
    }
    /**Un cliente lento no frena al resto */
    public Flux<Lectura> comoFlux() {
        return JdkFlowAdapter.flowPublisherToFlux(publicador).onBackpressureDrop();
    }
    @PreDestroy
    void cerrar() {
        publicador.close();
    }
}
