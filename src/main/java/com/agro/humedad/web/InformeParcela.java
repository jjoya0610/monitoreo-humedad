package com.agro.humedad.web;

import com.agro.humedad.dominio.NivelHumedad;

import java.util.Map;
public record InformeParcela(String parcelaId, ResumenRespuesta resumen,
                             Map<NivelHumedad, Long> lecturasPorNivel, LecturaRespuesta ultima) {
}
