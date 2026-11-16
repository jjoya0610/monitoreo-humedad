package com.agro.humedad.servicio;

import com.agro.humedad.concurrencia.CacheUltimasLecturas;
import com.agro.humedad.concurrencia.CalculadorEstadisticas;
import com.agro.humedad.concurrencia.FlujoLecturas;
import com.agro.humedad.dominio.Lectura;
import com.agro.humedad.dominio.NivelHumedad;
import com.agro.humedad.funcional.FuncionesHumedad;
import com.agro.humedad.funcional.ResumenHumedad;
import com.agro.humedad.repositorio.LecturaHumedadEntidad;
import com.agro.humedad.repositorio.LecturaHumedadRepositorio;
import com.agro.humedad.web.InformeParcela;
import com.agro.humedad.web.LecturaRespuesta;
import com.agro.humedad.web.ResumenRespuesta;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * Capa reactiva. JPA es bloqueante, por eso toda consulta a la base de datos se ejecuta
 * en Schedulers.boundedElastic() (el planificador pensado para E/S): el hilo del
 * servidor nunca se queda esperando (RESPONSIVO). Los timeouts y fallbacks dan RESILIENCIA.
 */
@Service
public class HumedadServicio {

    private static final Duration LIMITE = Duration.ofSeconds(3);
    private static final Duration LIMITE_CALCULO = Duration.ofSeconds(5);

    private record ResumenYNiveles(ResumenHumedad resumen, Map<NivelHumedad, Long> niveles) {
    }

    private final LecturaHumedadRepositorio repositorio;
    private final CacheUltimasLecturas cache;
    private final FlujoLecturas flujo;
    // Hilos virtuales: ideales para tareas que esperan a la base de datos.
    private final ExecutorService ejecutorTareas = Executors.newVirtualThreadPerTaskExecutor();

    public HumedadServicio(LecturaHumedadRepositorio repositorio, CacheUltimasLecturas cache,
                           FlujoLecturas flujo) {
        this.repositorio = repositorio;
        this.cache = cache;
        this.flujo = flujo;
    }

    public Mono<List<String>> parcelas() {
        return Mono.fromCallable(repositorio::listarParcelas)
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(LIMITE)
                .onErrorReturn(List.of());
    }

    public Mono<LecturaRespuesta> ultima(String parcelaId) {
        Mono<Lectura> deCache = Mono.defer(() -> Mono.justOrEmpty(cache.ultima(parcelaId)));
        Mono<Lectura> deBaseDeDatos = Mono
                .fromCallable(() -> repositorio.findFirstByParcelaIdOrderByFechaLecturaDesc(parcelaId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> Mono.justOrEmpty(opt.map(LecturaHumedadEntidad::aLectura)));
        return deCache
                .switchIfEmpty(deBaseDeDatos)
                .map(LecturaRespuesta::de)
                .timeout(LIMITE)
                .onErrorMap(TimeoutException.class, this::noDisponible)
                .switchIfEmpty(Mono.error(() -> sinLecturas(parcelaId)));
    }

    public Mono<ResumenRespuesta> resumen(String parcelaId) {
        return Mono.fromCallable(() -> repositorio.humedades(parcelaId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(lista -> lista.stream().mapToDouble(Double::doubleValue).toArray())
                .map(CalculadorEstadisticas::calcular)
                .filter(r -> r.cantidad() > 0)
                .map(r -> ResumenRespuesta.de(parcelaId, r))
                .timeout(LIMITE_CALCULO)
                .onErrorMap(TimeoutException.class, this::noDisponible)
                .switchIfEmpty(Mono.error(() -> sinLecturas(parcelaId)));
    }

    /**
     * PARTICIONAMIENTO DE TAREAS (paralela): tres funciones DISTINTAS corren a la vez con
     * CompletableFuture (resumen estadistico, conteo por nivel, ultima lectura) y se
     * combinan con thenCombine cuando todas terminan.
     */
    public Mono<InformeParcela> informe(String parcelaId) {
        return Mono.defer(() -> {
            CompletableFuture<ResumenHumedad> tareaResumen = CompletableFuture.supplyAsync(
                    () -> CalculadorEstadisticas.calcular(repositorio.humedades(parcelaId).stream()
                            .mapToDouble(Double::doubleValue).toArray()), ejecutorTareas);
            CompletableFuture<Map<NivelHumedad, Long>> tareaNiveles = CompletableFuture.supplyAsync(
                    () -> FuncionesHumedad.mapaPorNivel(repositorio.contarPorNivel(parcelaId)), ejecutorTareas);
            CompletableFuture<Optional<Lectura>> tareaUltima = CompletableFuture.supplyAsync(
                    () -> cache.ultima(parcelaId).or(() -> repositorio
                            .findFirstByParcelaIdOrderByFechaLecturaDesc(parcelaId)
                            .map(LecturaHumedadEntidad::aLectura)), ejecutorTareas);

            return Mono.fromFuture(tareaResumen
                    .thenCombine(tareaNiveles, ResumenYNiveles::new)
                    .thenCombine(tareaUltima, (rn, ultima) -> new InformeParcela(
                            parcelaId,
                            ResumenRespuesta.de(parcelaId, rn.resumen()),
                            rn.niveles(),
                            ultima.map(LecturaRespuesta::de).orElse(null))));
        })
                .filter(i -> i.resumen().cantidad() > 0)
                .timeout(LIMITE_CALCULO)
                .onErrorMap(TimeoutException.class, this::noDisponible)
                .switchIfEmpty(Mono.error(() -> sinLecturas(parcelaId)));
    }

    public Flux<LecturaRespuesta> historial(String parcelaId, int limite) {
        return Mono.fromCallable(() -> repositorio.findByParcelaIdOrderByFechaLecturaDesc(
                        parcelaId, PageRequest.of(0, limite)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(LecturaHumedadEntidad::aLectura)
                .map(LecturaRespuesta::de)
                .timeout(LIMITE)
                .onErrorMap(TimeoutException.class, this::noDisponible);
    }

    /** Todas las lecturas validas, en vivo. */
    public Flux<LecturaRespuesta> enVivo() {
        return flujo.comoFlux().map(LecturaRespuesta::de);
    }

    /** Solo lecturas en alerta (CRITICO o EXCESO): filter + map, como en la teoria. */
    public Flux<LecturaRespuesta> alertas() {
        return flujo.comoFlux()
                .filter(l -> FuncionesHumedad.NIVEL_DE_LECTURA.apply(l).esAlerta())
                .map(LecturaRespuesta::de);
    }

    private ResponseStatusException noDisponible(TimeoutException e) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio tardo demasiado. Intente de nuevo.");
    }

    private ResponseStatusException sinLecturas(String parcelaId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No hay lecturas para la parcela " + parcelaId);
    }
}
