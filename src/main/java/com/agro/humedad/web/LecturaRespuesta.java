package com.agro.humedad.web;

import com.agro.humedad.dominio.Lectura;
import com.agro.humedad.dominio.NivelHumedad;
import com.agro.humedad.funcional.FuncionesHumedad;

import java.time.Instant;

public record LecturaRespuesta(String parcelaId, String sensorId, double humedad,
                               NivelHumedad nivel, String recomendacion, Instant momento) {

    public static LecturaRespuesta de(Lectura l) {
        NivelHumedad nivel = FuncionesHumedad.NIVEL_DE_LECTURA.apply(l);
        return new LecturaRespuesta(l.parcelaId(), l.sensorId(), l.humedad(), nivel,
                nivel.recomendacion(), l.momento());
    }
}
