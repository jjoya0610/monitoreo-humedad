package com.agro.humedad.web;

import com.agro.humedad.concurrencia.ActorLecturas;
import com.agro.humedad.concurrencia.CacheUltimasLecturas;
import com.agro.humedad.concurrencia.CalculadorEstadisticas;
import com.agro.humedad.concurrencia.SimuladorSensores;
import com.agro.humedad.servicio.HumedadServicio;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/humedad")
public class HumedadControlador {

    private static final Pattern ID_VALIDO = Pattern.compile("^[A-Za-z0-9_-]{1,40}$");

    private final HumedadServicio servicio;
    private final CacheUltimasLecturas cache;
    private final ActorLecturas actor;
    private final SimuladorSensores simulador;

    public HumedadControlador(HumedadServicio servicio, CacheUltimasLecturas cache,
                              ActorLecturas actor, SimuladorSensores simulador) {
        this.servicio = servicio;
        this.cache = cache;
        this.actor = actor;
        this.simulador = simulador;
    }

    @GetMapping("/parcelas")
    public Mono<List<String>> parcelas() {
        return servicio.parcelas();
    }

    @GetMapping("/{parcelaId}/ultima")
    public Mono<LecturaRespuesta> ultima(@PathVariable("parcelaId") String parcelaId) {
        return servicio.ultima(validar(parcelaId));
    }

    @GetMapping("/{parcelaId}/resumen")
    public Mono<ResumenRespuesta> resumen(@PathVariable("parcelaId") String parcelaId) {
        return servicio.resumen(validar(parcelaId));
    }

    @GetMapping("/{parcelaId}/informe")
    public Mono<InformeParcela> informe(@PathVariable("parcelaId") String parcelaId) {
        return servicio.informe(validar(parcelaId));
    }

    @GetMapping("/{parcelaId}/historial")
    public Flux<LecturaRespuesta> historial(@PathVariable("parcelaId") String parcelaId,
                                            @RequestParam(name = "limite", defaultValue = "20") int limite) {
        return servicio.historial(validar(parcelaId), Math.max(1, Math.min(limite, 100)));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<LecturaRespuesta> stream() {
        return servicio.enVivo();
    }

    @GetMapping(value = "/alertas", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<LecturaRespuesta> alertas() {
        return servicio.alertas();
    }

    @GetMapping("/estado")
    public Mono<EstadoSistema> estado() {
        return Mono.fromSupplier(() -> new EstadoSistema(
                CalculadorEstadisticas.nucleos(),
                cache.totalRecibidas(),
                cache.instantanea().size(),
                simulador.estaActivo(),
                actor.estado()));
    }

    @PostMapping("/simular")
    public Mono<ResponseEntity<Map<String, Object>>> simular(
            @RequestParam(name = "sensores", defaultValue = "5") int sensores,
            @RequestParam(name = "lecturas", defaultValue = "10") int lecturas) {
        int s = Math.max(1, Math.min(sensores, 1000));
        int l = Math.max(1, Math.min(lecturas, 200));
        boolean iniciada = simulador.simular(s, l, 300);
        HttpStatus estado = iniciada ? HttpStatus.ACCEPTED : HttpStatus.CONFLICT;
        Map<String, Object> cuerpo = Map.of(
                "iniciada", iniciada,
                "sensores", s,
                "lecturasPorSensor", l);
        return Mono.just(ResponseEntity.status(estado).body(cuerpo));
    }

    private String validar(String id) {
        if (id == null || !ID_VALIDO.matcher(id).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de parcela invalido");
        }
        return id;
    }
}
