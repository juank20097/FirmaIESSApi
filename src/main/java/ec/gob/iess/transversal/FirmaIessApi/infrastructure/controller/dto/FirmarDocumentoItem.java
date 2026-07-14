/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto;

import lombok.*;

/**
 * <b> DTO que representa un documento individual dentro del lote de firma.
 * Contiene el nombre del archivo y su contenido en base64. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 *          [Author: Juan Carlos Estevez Hidalgo, Date: 14 jul 2026]
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FirmarDocumentoItem {

    /** Nombre del archivo PDF incluyendo la extension. Ej: contrato.pdf */
    private String nombre;

    /** Contenido del PDF codificado en base64. */
    private String documento;
}
