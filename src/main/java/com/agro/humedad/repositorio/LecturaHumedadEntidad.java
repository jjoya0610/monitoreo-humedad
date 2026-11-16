package com.agro.humedad.repositorio;
import com.agro.humedad.dominio.Lectura;
import com.agro.humedad.dominio.NivelHumedad;
import com.agro.humedad.funcional.FuncionesHumedad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

/**
 * PERSISTENCIA AVANZADA: dos indices compuestos y auditoria temporal automatica
 * (fecha de creacion y de modificacion).
 */
@Entity
@Table(name = "tb_lectura_humedad", indexes = {
        @Index(name = "idx_lectura_parcela_fecha", columnList = "parcela_id, fecha_lectura"),
        @Index(name = "idx_lectura_parcela_nivel", columnList = "parcela_id, nivel")
})
@EntityListeners(AuditingEntityListener.class)
public class LecturaHumedadEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parcela_id", nullable = false, length = 40)
    private String parcelaId;

    @Column(name = "sensor_id", nullable = false, length = 40)
    private String sensorId;

    @Column(name = "humedad", nullable = false)
    private double humedad;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 15)
    private NivelHumedad nivel;

    @Column(name = "fecha_lectura", nullable = false)
    private Instant fechaLectura;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @LastModifiedDate
    @Column(name = "fecha_modificacion")
    private Instant fechaModificacion;

    protected LecturaHumedadEntidad() {
    }

    public static LecturaHumedadEntidad desde(Lectura lectura) {
        LecturaHumedadEntidad e = new LecturaHumedadEntidad();
        e.parcelaId = lectura.parcelaId();
        e.sensorId = lectura.sensorId();
        e.humedad = lectura.humedad();
        e.nivel = FuncionesHumedad.NIVEL_DE_LECTURA.apply(lectura);
        e.fechaLectura = lectura.momento();
        return e;
    }

    public Lectura aLectura() {
        return new Lectura(parcelaId, sensorId, humedad, fechaLectura);
    }

    public Long getId() {
        return id;
    }

    public String getParcelaId() {
        return parcelaId;
    }

    public String getSensorId() {
        return sensorId;
    }

    public double getHumedad() {
        return humedad;
    }

    public NivelHumedad getNivel() {
        return nivel;
    }

    public Instant getFechaLectura() {
        return fechaLectura;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaModificacion() {
        return fechaModificacion;
    }
}
