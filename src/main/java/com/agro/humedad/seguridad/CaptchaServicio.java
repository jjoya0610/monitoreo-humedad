package com.agro.humedad.seguridad;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
@Service
public class CaptchaServicio {

    private static final Logger LOG = LoggerFactory.getLogger(CaptchaServicio.class);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaGoogle(boolean success) {
    }

    private final WebClient cliente;
    private final String secreto;
    private final String tokenDemo;
    private final String url;

    public CaptchaServicio(WebClient.Builder builder,
                           @Value("${agro.captcha.secret:}") String secreto,
                           @Value("${agro.captcha.token-demo:AGRO-CAPTCHA-DEMO}") String tokenDemo,
                           @Value("${agro.captcha.url:https://www.google.com/recaptcha/api/siteverify}") String url) {
        this.cliente = builder.build();
        this.secreto = secreto;
        this.tokenDemo = tokenDemo;
        this.url = url;
    }

    @PostConstruct
    void avisarModo() {
        if (secreto.isBlank()) {
            LOG.warn("Captcha en modo DEMO (token fijo). Configure CAPTCHA_SECRET para usar reCAPTCHA real.");
        }
    }

    public Mono<Boolean> validar(String token) {
        if (token == null || token.isBlank()) {
            return Mono.just(false);
        }
        if (secreto.isBlank()) {
            return Mono.just(MessageDigest.isEqual(
                    tokenDemo.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)));
        }
        return cliente.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("secret", secreto).with("response", token))
                .retrieve()
                .bodyToMono(RespuestaGoogle.class)
                .map(RespuestaGoogle::success)
                .timeout(Duration.ofSeconds(2))
                .onErrorReturn(false);
    }
}
