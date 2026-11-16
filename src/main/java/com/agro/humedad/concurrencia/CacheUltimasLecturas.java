package com.agro.humedad.concurrencia;

import com.agro.humedad.dominio.Lectura;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
public class CacheUltimasLecturas {
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Lectura> ultimaPorParcela = new HashMap<>();
    private final AtomicLong totalRecibidas = new AtomicLong();
    public void actualizar(Lectura lectura) {
        lock.lock();
        try {
            Lectura anterior = ultimaPorParcela.get(lectura.parcelaId());
            if (anterior == null || lectura.momento().isAfter(anterior.momento())) {
                ultimaPorParcela.put(lectura.parcelaId(), lectura);
            }
        } finally {
            lock.unlock();
        }
        totalRecibidas.incrementAndGet();
    }
    public Optional<Lectura> ultima(String parcelaId) {
        lock.lock();
        try {
            return Optional.ofNullable(ultimaPorParcela.get(parcelaId));
        } finally {
            lock.unlock();
        }
    }
    public Map<String, Lectura> instantanea() {
        lock.lock();
        try {
            return Map.copyOf(ultimaPorParcela);
        } finally {
            lock.unlock();
        }
    }
    public long totalRecibidas() {
        return totalRecibidas.get();
    }
}
