/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto;

import lombok.*;
import java.util.List;

/**
 * <b> DTO de entrada para el endpoint POST /iess/firmaec/firmar.
 *
 * Soporta dos modos de envio:
 * Sin cifrado: usar los campos pkcs12 y password directamente en base64.
 * Con cifrado hibrido RSA+AES: usar los campos pkcs12Cifrado, passwordCifrado
 * y claveAesCifrada. La presencia de claveAesCifrada activa el modo cifrado. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 *          [Author: Juan Carlos Estevez Hidalgo, Date: 14 jul 2026]
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FirmarLoteRequest {

    /** Identificador unico del lote de firma. Maximo 10 documentos por llamada. */
    private String idLote;

    /** Cedula del firmante (10 digitos). */
    private String cedula;

    // -- Modo sin cifrado --------------------------------------------------

    /** Certificado .p12 en base64. Usar cuando no se usa cifrado hibrido. */
    private String pkcs12;

    /** Contrasena del certificado en base64. Usar cuando no se usa cifrado hibrido. */
    private String password;

    // -- Modo con cifrado hibrido RSA + AES --------------------------------

    /** Certificado .p12 cifrado con AES-256-GCM en base64. */
    private String pkcs12Cifrado;

    /** Contrasena del certificado cifrada con AES-256-GCM en base64. */
    private String passwordCifrado;

    /**
     * Clave AES cifrada con la clave publica RSA del servidor en base64.
     * La presencia de este campo activa el modo de cifrado hibrido.
     */
    private String claveAesCifrada;

    // -- Modo por sistema registrado (certificado guardado en BD) ----------

    /**
     * Nombre del sistema con certificado pre-registrado en la tabla
     * certificados (ver CertificadoEntity). Si viene presente y pkcs12/
     * pkcs12Cifrado estan vacios, el certificado y la cedula se resuelven
     * del lado del servidor -- el cliente nunca ve el .p12 ni la password.
     */
    private String certificadoSistema;

    /** Lista de documentos a firmar. Maximo 10 por llamada. */
    private List<FirmarDocumentoItem> documentos;

    /** Parametros globales de firma aplicados a todos los documentos del lote. */
    private FirmarParametros parametros;

    /**
     * <b> Indica si el request viene con cifrado hibrido activo. </b>
     *
     * @return true si claveAesCifrada esta presente y no esta vacia
     */
    public boolean esCifrado() {
        return claveAesCifrada != null && !claveAesCifrada.isBlank();
    }
}
