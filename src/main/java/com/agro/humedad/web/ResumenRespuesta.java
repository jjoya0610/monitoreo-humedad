package com.agro.humedad.web;

import com.agro.humedad.dominio.NivelHumedad;
import com.agro.humedad.funcional.FuncionesHumedad;
import com.agro.humedad.funcional.ResumenHumedad;

public record ResumenRespuesta(String parcelaId, long cantidad, double promedio,
                               double minimo, double maximo, NivelHumedad nivelPromedio) {

    public static ResumenRespuesta de(String parcelaId, ResumenHumedad r) {
        return new ResumenRespuesta(parcelaId, r.cantidad(), redondear(r.promedio()),
                r.minimo(), r.maximo(), FuncionesHumedad.CLASIFICAR.apply(r.promedio()));
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
