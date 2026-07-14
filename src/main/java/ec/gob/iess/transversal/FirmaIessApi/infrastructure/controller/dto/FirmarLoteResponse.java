/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto;

import lombok.*;

/**
 * <b> DTO de salida para el endpoint POST /iess/firmaec/firmar.
 * Retorna el resultado del proceso de firma del lote con totales
 * y estado de la operacion. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 *          [Author: Juan Carlos Estevez Hidalgo, Date: 14 jul 2026]
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FirmarLoteResponse {

    /** Identificador del lote procesado. */
    private String idLote;

    /** Total de documentos enviados en el request. */
    private int totalEnviados;

    /** Total de documentos firmados exitosamente. */
    private int totalFirmados;

    /** Total de documentos que no pudieron ser firmados. */
    private int totalErrores;

    /** Mensaje descriptivo del resultado del proceso. */
    private String mensaje;

    /** Estado del proceso: OK, PARCIAL o ERROR. */
    private String estado;
}
