package com.agro.humedad.concurrencia;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class ConcurrenciaConfig {
    @Bean
    public CacheUltimasLecturas cacheUltimasLecturas() {
        return new CacheUltimasLecturas();
    }
    /** El actor entrega las lecturas validas al flujo reactivo. */
    @Bean
    public ActorLecturas actorLecturas(FlujoLecturas flujo) {
        return new ActorLecturas(flujo::publicar);
    }
}
