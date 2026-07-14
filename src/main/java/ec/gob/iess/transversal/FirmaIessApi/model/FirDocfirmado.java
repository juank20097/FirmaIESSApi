/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FirDocfirmado {

    private UUID idDoc;
    private String cedula;
    private String error;
    private boolean firmasValidas;
    private boolean integridadDocumento;

    @com.fasterxml.jackson.annotation.JsonProperty("archivo")
    private byte[] archivo;

    private String nombreDocumento;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String deletedBy;
    private LocalDateTime deletedAt;

    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonProperty("certificado")
    private List<FirDocfirmadoDetalle> firmas = new ArrayList<>();
}
