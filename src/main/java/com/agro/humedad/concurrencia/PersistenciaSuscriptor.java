package com.agro.humedad.concurrencia;
import com.agro.humedad.dominio.Lectura;
import com.agro.humedad.repositorio.LecturaHumedadEntidad;
import com.agro.humedad.repositorio.LecturaHumedadRepositorio;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.Flow;

/**
 * Suscriptor que guarda cada lectura en la base de datos y actualiza la cache.
 * Pide UNA lectura a la vez (request(1)): asi nunca se ve desbordado.
 * Si falla una lectura, la registra y sigue con la siguiente (resiliencia).
 */
@Component
public class PersistenciaSuscriptor implements Flow.Subscriber<Lectura> {

    private static final Logger LOG = LoggerFactory.getLogger(PersistenciaSuscriptor.class);
    private final FlujoLecturas flujo;
    private final LecturaHumedadRepositorio repositorio;
    private final CacheUltimasLecturas cache;
    private volatile Flow.Subscription suscripcion;
    public PersistenciaSuscriptor(FlujoLecturas flujo, LecturaHumedadRepositorio repositorio, CacheUltimasLecturas cache) {
        this.flujo = flujo;
        this.repositorio = repositorio;
        this.cache = cache;
    }
    @PostConstruct
    void iniciar() {
        flujo.suscribir(this);
    }
    @Override
    public void onSubscribe(Flow.Subscription s) {
        this.suscripcion = s;
        s.request(1);
    }
    @Override
    public void onNext(Lectura lectura) {
        try {
            cache.actualizar(lectura);
            repositorio.save(LecturaHumedadEntidad.desde(lectura));
        } catch (Exception e) {
            LOG.error("No se pudo guardar la lectura de {}: {}", lectura.parcelaId(), e.getMessage());
        } finally {
            suscripcion.request(1);
        }
    }
    @Override
    public void onError(Throwable t) {
        LOG.error("El flujo de lecturas termino con error", t);
    }
    @Override
    public void onComplete() {
        LOG.info("Flujo de lecturas cerrado");
    }
}
