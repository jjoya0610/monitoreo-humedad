package com.agro.humedad.repositorio;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LecturaHumedadRepositorio extends JpaRepository<LecturaHumedadEntidad, Long> {

    Optional<LecturaHumedadEntidad> findFirstByParcelaIdOrderByFechaLecturaDesc(String parcelaId);

    List<LecturaHumedadEntidad> findByParcelaIdOrderByFechaLecturaDesc(String parcelaId, Pageable pageable);

    @Query("select l.humedad from LecturaHumedadEntidad l where l.parcelaId = :parcela")
    List<Double> humedades(@Param("parcela") String parcelaId);

    @Query("select l.nivel, count(l) from LecturaHumedadEntidad l where l.parcelaId = :parcela group by l.nivel")
    List<Object[]> contarPorNivel(@Param("parcela") String parcelaId);

    @Query("select distinct l.parcelaId from LecturaHumedadEntidad l order by l.parcelaId")
    List<String> listarParcelas();
}
