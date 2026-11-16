package com.agro.humedad.web;

import com.agro.humedad.dominio.NivelHumedad;

import java.util.Map;

/** Informe completo de una parcela, armado con tres tareas que corren en paralelo. */
public record InformeParcela(String parcelaId, ResumenRespuesta resumen,
                             Map<NivelHumedad, Long> lecturasPorNivel, LecturaRespuesta ultima) {
}
