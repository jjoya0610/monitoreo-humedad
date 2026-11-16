package com.agro.humedad.seguridad;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CaptchaFilter implements WebFilter {

    private final CaptchaServicio captcha;

    public CaptchaFilter(CaptchaServicio captcha) {
        this.captcha = captcha;
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String ruta = exchange.getRequest().getPath().value();
        if (!ruta.startsWith("/api/")) {
            return chain.filter(exchange);
        }
        String token = exchange.getRequest().getHeaders().getFirst("X-Captcha-Token");
        if (token == null) {
            token = exchange.getRequest().getQueryParams().getFirst("captcha");
        }
        return captcha.validar(token)
                .flatMap(valido -> valido ? chain.filter(exchange) : rechazar(exchange));
    }
    private Mono<Void> rechazar(ServerWebExchange exchange) {
        ServerHttpResponse respuesta = exchange.getResponse();
        respuesta.setStatusCode(HttpStatus.FORBIDDEN);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] cuerpo = "{\"error\":\"Captcha invalido o ausente\"}".getBytes(StandardCharsets.UTF_8);
        return respuesta.writeWith(Mono.just(respuesta.bufferFactory().wrap(cuerpo)));
    }
}
