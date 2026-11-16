package com.agro.humedad;

import com.agro.humedad.concurrencia.ActorLecturas;
import com.agro.humedad.concurrencia.CacheUltimasLecturas;
import com.agro.humedad.concurrencia.CalculadorEstadisticas;
import com.agro.humedad.dominio.Lectura;
import com.agro.humedad.funcional.ResumenHumedad;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrenciaTest {

    private final Instant t = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void forkJoinCoincideConElCalculoSecuencial() {
        double[] datos = new Random(1).doubles(500_000, 0, 100).toArray();
        ResumenHumedad fj = CalculadorEstadisticas.calcular(datos);
        var seq = Arrays.stream(datos).summaryStatistics();
        assertEquals(seq.getCount(), fj.cantidad());
        assertEquals(seq.getAverage(), fj.promedio(), 1e-9);
        assertEquals(seq.getMin(), fj.minimo());
        assertEquals(seq.getMax(), fj.maximo());
    }

    @Test
    void actorValidaYReenvia() {
        List<Lectura> recibidas = new CopyOnWriteArrayList<>();
        ActorLecturas actor = new ActorLecturas(recibidas::add);
        actor.enviar(new Lectura("P1", "S1", 50, t));
        actor.enviar(new Lectura("P1", "S1", 150, t));
        actor.close();
        assertEquals(1, recibidas.size());
        assertEquals(1, actor.estado().rechazadas());
    }

    @Test
    void cacheNoPierdeActualizacionesConHilosVirtuales() {
        CacheUltimasLecturas cache = new CacheUltimasLecturas();
        try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 2000; i++) {
                int n = i;
                ex.submit(() -> cache.actualizar(new Lectura("P" + (n % 5), "S" + n, 50, Instant.now())));
            }
        }
        assertEquals(2000, cache.totalRecibidas());
        assertEquals(5, cache.instantanea().size());
    }
}
