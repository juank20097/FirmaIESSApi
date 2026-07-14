/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto;

import lombok.*;

import java.util.List;

/**
 * <b> DTO genérico de respuesta paginada.
 * Reutilizable en todos los endpoints de listado del sistema. </b>
 *
 * @param <T> tipo del contenido de la página
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
public class PageResponse<T> {

    private List<T> contenido;
    private int paginaActual;
    private int tamanioPagina;
    private long totalElementos;
    private int totalPaginas;
    private boolean primera;
    private boolean ultima;
}
