/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto;

import lombok.*;

/**
 * <b> DTO de salida para el endpoint POST /iess/firmaec/empaquetar/{idLote}.
 * Retorna el resultado del proceso de empaquetado de PDFs firmados en ZIPs. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 *          [Author: Juan Carlos Estevez Hidalgo, Date: 14 jul 2026]
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmpaquetarResponse {

    /** Identificador del lote empaquetado. */
    private String idLote;

    /** Estado del proceso: OK o ERROR. */
    private String estado;

    /** Total de archivos ZIP generados. */
    private int totalZips;

    /** Total de documentos PDF empaquetados. */
    private int totalDocumentos;

    /** Mensaje descriptivo del resultado del proceso. */
    private String mensaje;
}
