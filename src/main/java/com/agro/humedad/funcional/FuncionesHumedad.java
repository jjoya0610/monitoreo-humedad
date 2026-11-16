package com.agro.humedad.funcional;
import com.agro.humedad.dominio.Lectura;
import com.agro.humedad.dominio.NivelHumedad;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
public final class FuncionesHumedad {

    private FuncionesHumedad() {
    }
    public static final Predicate<Lectura> ES_VALIDA = l ->
            l != null
                    && l.parcelaId() != null
                    && !l.parcelaId().isBlank()
                    && l.humedad() >= 0.0
                    && l.humedad() <= 100.0;
    static NivelHumedad clasificarEntero(int humedad) {
        if (humedad < 20) {
            return NivelHumedad.CRITICO;
        }
        if (humedad < 40) {
            return NivelHumedad.BAJO;
        }
        if (humedad < 70) {
            return NivelHumedad.OPTIMO;
        }
        return NivelHumedad.EXCESO;
    }
    public static <T, R> Function<T, R> memoizar(Function<T, R> funcion) {
        Map<T, R> cache = new ConcurrentHashMap<>();
        return entrada -> cache.computeIfAbsent(entrada, funcion);
    }
    private static final Function<Integer, NivelHumedad> CLASIFICAR_MEMO =
            memoizar(FuncionesHumedad::clasificarEntero);

    public static final Function<Double, NivelHumedad> CLASIFICAR =
            humedad -> CLASIFICAR_MEMO.apply((int) Math.floor(humedad));
    public static final Function<Lectura, NivelHumedad> NIVEL_DE_LECTURA =
            ((Function<Lectura, Double>) Lectura::humedad).andThen(CLASIFICAR);
    public static ResumenHumedad resumir(Collection<Lectura> lecturas) {
        return lecturas.stream()
                .map(l -> ResumenHumedad.de(l.humedad()))
                .reduce(ResumenHumedad.VACIO, ResumenHumedad::combinar);
    }
    public static Map<NivelHumedad, Long> contarPorNivel(Collection<Lectura> lecturas) {
        return Map.copyOf(lecturas.stream()
                .collect(Collectors.groupingBy(NIVEL_DE_LECTURA, Collectors.counting())));
    }
    public static List<Lectura> soloAlertas(Collection<Lectura> lecturas) {
        return lecturas.stream()
                .filter(ES_VALIDA)
                .filter(l -> NIVEL_DE_LECTURA.apply(l).esAlerta())
                .toList();
    }
    public static Map<NivelHumedad, Long> mapaPorNivel(List<Object[]> filas) {
        return Map.copyOf(filas.stream().collect(Collectors.toMap(
                fila -> (NivelHumedad) fila[0],
                fila -> ((Number) fila[1]).longValue())));
    }
    public static Supplier<String> mensajeAlerta(Lectura l) {
        return () -> "Parcela " + l.parcelaId() + " (" + l.sensorId() + "): humedad "
                + l.humedad() + "% -> " + NIVEL_DE_LECTURA.apply(l).recomendacion();
    }
}
