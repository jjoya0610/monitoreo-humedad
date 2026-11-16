package com.agro.humedad;

import com.agro.humedad.dominio.Lectura;
import com.agro.humedad.dominio.NivelHumedad;
import com.agro.humedad.funcional.FuncionesHumedad;
import com.agro.humedad.funcional.ResumenHumedad;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FuncionesHumedadTest {

    private final Instant t = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void clasificaEnLosLimites() {
        assertEquals(NivelHumedad.CRITICO, FuncionesHumedad.CLASIFICAR.apply(19.9));
        assertEquals(NivelHumedad.BAJO, FuncionesHumedad.CLASIFICAR.apply(20.0));
        assertEquals(NivelHumedad.OPTIMO, FuncionesHumedad.CLASIFICAR.apply(40.0));
        assertEquals(NivelHumedad.EXCESO, FuncionesHumedad.CLASIFICAR.apply(70.0));
    }

    @Test
    void esPuraYDeterminista() {
        Lectura l = new Lectura("P1", "S1", 55.5, t);
        assertEquals(FuncionesHumedad.NIVEL_DE_LECTURA.apply(l), FuncionesHumedad.NIVEL_DE_LECTURA.apply(l));
    }

    @Test
    void rechazaLecturasInvalidas() {
        assertFalse(FuncionesHumedad.ES_VALIDA.test(new Lectura("P1", "S1", 150, t)));
        assertFalse(FuncionesHumedad.ES_VALIDA.test(new Lectura("P1", "S1", -1, t)));
        assertTrue(FuncionesHumedad.ES_VALIDA.test(new Lectura("P1", "S1", 50, t)));
    }

    @Test
    void resumeConMapYReduce() {
        List<Lectura> ls = List.of(new Lectura("P1", "S1", 10, t), new Lectura("P1", "S1", 50, t),
                new Lectura("P1", "S1", 90, t));
        ResumenHumedad r = FuncionesHumedad.resumir(ls);
        assertEquals(3, r.cantidad());
        assertEquals(50.0, r.promedio(), 1e-9);
        assertEquals(2, FuncionesHumedad.soloAlertas(ls).size());
    }

    @Test
    void conviertePilasDeBaseDeDatosEnMapaInmutable() {
        var mapa = FuncionesHumedad.mapaPorNivel(List.of(
                new Object[]{NivelHumedad.OPTIMO, 7L}, new Object[]{NivelHumedad.CRITICO, 2L}));
        assertEquals(7L, mapa.get(NivelHumedad.OPTIMO));
        assertEquals(2L, mapa.get(NivelHumedad.CRITICO));
        assertThrows(UnsupportedOperationException.class, () -> mapa.put(NivelHumedad.BAJO, 1L));
    }
}
