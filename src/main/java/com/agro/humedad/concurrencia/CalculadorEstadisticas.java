package com.agro.humedad.concurrencia;

import com.agro.humedad.funcional.ResumenHumedad;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
public final class CalculadorEstadisticas {
    private static final int UMBRAL = 10_000;
    private static final ForkJoinPool POOL = new ForkJoinPool(nucleos());
    private CalculadorEstadisticas() {
    }
    public static int nucleos() {
        return Runtime.getRuntime().availableProcessors();
    }
    public static ResumenHumedad calcular(double[] datos) {
        if (datos.length == 0) {
            return ResumenHumedad.VACIO;
        }
        return POOL.invoke(new Tarea(datos, 0, datos.length));
    }
    static final class Tarea extends RecursiveTask<ResumenHumedad> {
        private final double[] datos;
        private final int inicio;
        private final int fin;
        Tarea(double[] datos, int inicio, int fin) {
            this.datos = datos;
            this.inicio = inicio;
            this.fin = fin;
        }
        @Override
        protected ResumenHumedad compute() {
            if (fin - inicio <= UMBRAL) {
                double suma = 0;
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (int i = inicio; i < fin; i++) {
                    double v = datos[i];
                    suma += v;
                    if (v < min) {
                        min = v;
                    }
                    if (v > max) {
                        max = v;
                    }
                }
                return new ResumenHumedad(fin - inicio, suma, min, max);
            }
            int mitad = inicio + (fin - inicio) / 2;
            Tarea izquierda = new Tarea(datos, inicio, mitad);
            Tarea derecha = new Tarea(datos, mitad, fin);
            izquierda.fork();
            ResumenHumedad resultadoDerecha = derecha.compute();
            return izquierda.join().combinar(resultadoDerecha);
        }
    }
}
