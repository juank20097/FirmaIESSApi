/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "FIR_DOCFIRMADOSDET_T")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FirDocfirmadoDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_docdet", updatable = false, nullable = false)
    private UUID idDocDet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_doc", nullable = false)
    private FirDocfirmadoEntity firmados;

    @Column(name = "apellido", length = 100)
    private String apellido;

    @Column(name = "cargo", length = 256)
    private String cargo;

    @Column(name = "cedula", length = 13, nullable = false)
    private String cedula;

    @Column(name = "cer_digvalido")
    private boolean certificadoDigitalValido;

    @Column(name = "cert_vigente")
    private boolean certificadoVigente;

    @Column(name = "claves_uso", length = 100)
    private String clavesUso;

    @Column(name = "emitido_para", length = 256)
    private String emitidoPara;

    @Column(name = "emitido_por", length = 256)
    private String emitidoPor;

    @Column(name = "ent_certificadora", length = 256)
    private String entidadCertificadora;

    @Column(name = "fec_firma")
    private LocalDateTime fechaFirma;

    @Column(name = "fec_revocado")
    private LocalDateTime fechaRevocado;

    @Column(name = "fec_sellotiempo")
    private LocalDateTime fechaSellotiempo;

    @Column(name = "institucion", length = 256)
    private String institucion;

    @Column(name = "integridad")
    private boolean integridadFirma;

    @Column(name = "localizacion", length = 256)
    private String localizacion;

    @Column(name = "nombre", length = 100)
    private String nombre;

    @Column(name = "razon_firma", length = 256)
    private String razonFirma;

    @Column(name = "sellado_tiempo")
    private boolean selladoTiempo;

    @Column(name = "serial")
    private String serial;

    @Column(name = "valido_desde")
    private LocalDateTime validoDesde;

    @Column(name = "valido_hasta")
    private LocalDateTime validoHasta;

    @Column(name = "status", length = 1)
    private String status;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
