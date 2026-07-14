/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <b> DTO de salida para la entidad Documento.
 * Retorna únicamente la metadata del archivo incluyendo los 7 atributos de auditoría. </b>
 *
 * @author Juan Carlos Estévez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estévez Hidalgo , Date: 07 may 2026]
 *          </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoResponse {

    private UUID id;
    private UUID personaId;
    private String nombreOriginal;
    private String tipoMime;
    private Long tamanioBytes;
    private String descripcion;
    private String bucket;

    /** Estado: A = Activo, I = Inactivo, E = Eliminado lógico. */
    private String status;

    // ── Auditoría estándar PAS-EST-002 sección 6.1 ─────────
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String deletedBy;
    private LocalDateTime deletedAt;
}
