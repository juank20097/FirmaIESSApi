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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "FIR_DOCFIRMADOS_T")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FirDocfirmadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_doc", updatable = false, nullable = false)
    private UUID idDoc;

    @Column(name = "cedula", length = 10, nullable = false)
    private String cedula;

    @Column(name = "error", length = 500)
    private String error;

    @Column(name = "firmas_validas")
    private boolean firmasValidas;

    @Column(name = "integridad_documento")
    private boolean integridadDocumento;

    @Column(name = "nombre_documento", length = 255, nullable = false)
    private String nombreDocumento;

    @Column(name = "status", length = 1, nullable = false)
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

    @OneToMany(mappedBy = "firmados", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FirDocfirmadoDetalleEntity> firmas = new ArrayList<>();
}
