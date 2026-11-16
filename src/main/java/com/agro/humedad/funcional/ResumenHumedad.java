package com.agro.humedad.funcional;

/*Resultado estadistico inmutable*/
public record ResumenHumedad(long cantidad, double suma, double minimo, double maximo) {

    public static final ResumenHumedad VACIO =
            new ResumenHumedad(0, 0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);

    public static ResumenHumedad de(double valor) {
        return new ResumenHumedad(1, valor, valor, valor);
    }

    public ResumenHumedad combinar(ResumenHumedad otro) {
        return new ResumenHumedad(
                cantidad + otro.cantidad,
                suma + otro.suma,
                Math.min(minimo, otro.minimo),
                Math.max(maximo, otro.maximo));
    }

    public double promedio() {
        return cantidad == 0 ? 0.0 : suma / cantidad;
    }
}
