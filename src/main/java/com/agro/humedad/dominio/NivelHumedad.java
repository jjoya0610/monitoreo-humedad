package com.agro.humedad.dominio;
public enum NivelHumedad {
    CRITICO("Riego urgente"),
    BAJO("Programar riego"),
    OPTIMO("Humedad adecuada para el cafe"),
    EXCESO("Riesgo de encharcamiento");
    private final String recomendacion;
    NivelHumedad(String recomendacion) {
        this.recomendacion = recomendacion;
    }
    public String recomendacion() {
        return recomendacion;
    }
    public boolean esAlerta() {
        return this == CRITICO || this == EXCESO;
    }
}
