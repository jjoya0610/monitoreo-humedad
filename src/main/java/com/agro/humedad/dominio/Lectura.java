package com.agro.humedad.dominio;
import java.time.Instant;
public record Lectura(String parcelaId, String sensorId, double humedad, Instant momento) {
}
