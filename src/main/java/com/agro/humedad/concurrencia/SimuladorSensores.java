package com.agro.humedad.concurrencia;
import com.agro.humedad.dominio.Lectura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
@Component
public class SimuladorSensores {

    private final ActorLecturas actor;
    private final AtomicBoolean activo = new AtomicBoolean(false);
    private final boolean autoarranque;

    public SimuladorSensores(ActorLecturas actor,
                             @Value("${agro.simulador.autoarranque:true}") boolean autoarranque) {
        this.actor = actor;
        this.autoarranque = autoarranque;
    }
    @EventListener(ApplicationReadyEvent.class)
    void alIniciar() {
        if (autoarranque) {
            simular(5, 20, 500);
        }
    }
    public boolean estaActivo() {
        return activo.get();
    }
    /**Devuelve false si ya hay una simulacion en curso*/
    public boolean simular(int sensores, int lecturasPorSensor, long pausaMs) {
        if (!activo.compareAndSet(false, true)) {
            return false;
        }
        Thread.ofVirtual().name("simulador-principal").start(() -> {
            try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                IntStream.rangeClosed(1, sensores)
                        .forEach(n -> ejecutor.submit(() -> emitir(n, lecturasPorSensor, pausaMs)));
            } finally {
                activo.set(false);
            }
        });
        return true;
    }
    private void emitir(int numero, int lecturas, long pausaMs) {
        String sensor = String.format(Locale.ROOT, "SENSOR-%03d", numero);
        String parcela = "PARCELA-" + (1 + (numero - 1) % 5);
        ThreadLocalRandom azar = ThreadLocalRandom.current();
        try {
            for (int i = 0; i < lecturas; i++) {
                double valor = azar.nextDouble() < 0.05 ? 150.0 : 10 + azar.nextDouble() * 80;
                actor.enviar(new Lectura(parcela, sensor, Math.round(valor * 10) / 10.0, Instant.now()));
                Thread.sleep(pausaMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
