/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <b> DTO de salida con la clave pública RSA del sistema,
 * usada por consumidores externos para cifrar campos sensibles
 * (cédula, correo) antes de enviarlos en el JSON de entrada. </b>
 *
 * @author Juan Carlos Estévez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estévez Hidalgo , Date: 25 jun 2026]
 *          </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicKeyResponse {

    /** Algoritmo de la clave (siempre "RSA"). */
    private String algoritmo;

    /** Formato de codificación de la clave (X.509). */
    private String formato;

    /** Clave pública codificada en Base64. */
    private String publicKey;
}
