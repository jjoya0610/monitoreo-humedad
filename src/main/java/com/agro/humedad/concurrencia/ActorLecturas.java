package com.agro.humedad.concurrencia;

import com.agro.humedad.dominio.Lectura;
import com.agro.humedad.funcional.FuncionesHumedad;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class ActorLecturas implements AutoCloseable {

    public record Estado(long procesadas, long rechazadas, int pendientes) {
    }

    private static final System.Logger LOG = System.getLogger(ActorLecturas.class.getName());
    private static final Object PARAR = new Object();

    private final BlockingQueue<Object> buzon = new LinkedBlockingQueue<>();
    private final Consumer<Lectura> destino;
    private final AtomicLong procesadas = new AtomicLong();
    private final AtomicLong rechazadas = new AtomicLong();
    private final Thread hilo;

    public ActorLecturas(Consumer<Lectura> destino) {
        this.destino = Objects.requireNonNull(destino);
        this.hilo = Thread.ofPlatform().name("actor-lecturas").daemon(true).unstarted(this::ciclo);
        this.hilo.start();
    }

    /** No bloquea al emisor: solo deja el mensaje en el buzon. */
    public void enviar(Lectura lectura) {
        buzon.add(Objects.requireNonNull(lectura));
    }

    private void ciclo() {
        try {
            while (true) {
                Object mensaje = buzon.take();
                if (mensaje == PARAR) {
                    break;
                }
                procesar((Lectura) mensaje);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void procesar(Lectura lectura) {
        if (!FuncionesHumedad.ES_VALIDA.test(lectura)) {
            rechazadas.incrementAndGet();
            return;
        }
        try {
            destino.accept(lectura);
            procesadas.incrementAndGet();
        } catch (RuntimeException e) {
            rechazadas.incrementAndGet();
            LOG.log(System.Logger.Level.WARNING, "El actor no pudo reenviar la lectura: " + e.getMessage());
        }
    }

    public Estado estado() {
        return new Estado(procesadas.get(), rechazadas.get(), buzon.size());
    }

    @Override
    public void close() {
        buzon.add(PARAR);
        try {
            hilo.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
