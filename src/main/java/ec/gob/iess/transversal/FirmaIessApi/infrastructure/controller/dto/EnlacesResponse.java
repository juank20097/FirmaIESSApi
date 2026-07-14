/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto;

import lombok.*;
import java.util.List;

/**
 * <b> DTO de salida para el endpoint GET /iess/firmaec/enlaces/{idLote}.
 * Retorna las URLs presignadas de descarga de los documentos firmados del lote. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 *          [Author: Juan Carlos Estevez Hidalgo, Date: 14 jul 2026]
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnlacesResponse {

    /** Identificador del lote consultado. */
    private String idLote;

    /** Estado de la consulta: OK o ERROR. */
    private String estado;

    /** Mensaje descriptivo del resultado. */
    private String mensaje;

    /** Lista de enlaces presignados de descarga. */
    private List<EnlaceItem> enlaces;

    /**
     * <b> DTO anidado que representa un enlace individual de descarga. </b>
     */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EnlaceItem {

        /** Nombre del archivo disponible para descarga (PDF o ZIP). */
        private String nombre;

        /** URL presignada de descarga generada por MinIO. */
        private String url;

        /** Horas de vigencia de la URL presignada antes de expirar. */
        private int horasExpiracion;
    }
}
