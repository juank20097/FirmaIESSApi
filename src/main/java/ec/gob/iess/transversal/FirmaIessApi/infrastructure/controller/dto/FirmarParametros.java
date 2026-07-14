/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto;

import lombok.*;

/**
 * <b> DTO con los parametros de posicion y estilo de la firma digital
 * aplicados a los documentos del lote. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 *          [Author: Juan Carlos Estevez Hidalgo, Date: 14 jul 2026]
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FirmarParametros {

    /** Coordenada X del cuadro de firma en el PDF (eje horizontal). */
    private String llx;

    /** Coordenada Y del cuadro de firma en el PDF (eje vertical). */
    private String lly;

    /** Numero de pagina donde se ubicara la firma (desde 1). */
    private String pagina;

    /** Tipo de estampado de la firma. Valor valido: QR. */
    private String tipoEstampado;
}
