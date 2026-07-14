/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * <b> Utilidad de cifrado/descifrado RSA para campos sensibles puntuales
 * (cédula, correo) enviados por consumidores externos en el JSON de entrada.
 * El consumidor cifra con la clave pública publicada; solo este backend,
 * con la clave privada, puede descifrar.
 * Se activa únicamente cuando RSA_ENABLED=true (bean RsaConfig). </b>
 *
 * @author Juan Carlos Estévez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estévez Hidalgo , Date: 25 jun 2026]
 *          </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rsa.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RsaCipherUtil {

    /** Algoritmo de cifrado RSA con padding OAEP (recomendado, resistente a ataques de padding). */
    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /** Clave pública, usada solo si se necesitara cifrar desde este backend (pruebas, herramientas). */
    private final PublicKey publicKey;

    /** Clave privada, usada para descifrar los campos recibidos del consumidor externo. */
    private final PrivateKey privateKey;

    /**
     * <b> Cifra un texto plano con la clave pública configurada.
     * Pensado principalmente para generar valores de prueba; el cifrado real
     * lo realiza el consumidor externo con la clave pública publicada. </b>
     *
     * @param textoPlano texto a cifrar
     * @return texto cifrado codificado en Base64
     */
    public String encrypt(String textoPlano) {
        if (textoPlano == null || textoPlano.isBlank()) {
            return textoPlano;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cifrado);
        } catch (Exception e) {
            log.error("RSA: error al cifrar el valor. Causa: {}", e.getMessage());
            throw new IllegalStateException("Error al cifrar el campo con RSA", e);
        }
    }

    /**
     * <b> Descifra un texto cifrado (Base64) recibido del consumidor externo,
     * usando la clave privada del backend. </b>
     *
     * @param textoCifradoBase64 texto cifrado codificado en Base64
     * @return texto plano descifrado
     */
    public String decrypt(String textoCifradoBase64) {
        if (textoCifradoBase64 == null || textoCifradoBase64.isBlank()) {
            return textoCifradoBase64;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] cifrado = Base64.getDecoder().decode(textoCifradoBase64);
            byte[] descifrado = cipher.doFinal(cifrado);
            return new String(descifrado, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("RSA: error al descifrar el valor recibido. Causa: {}", e.getMessage());
            throw new IllegalArgumentException(
                    "El campo recibido no está cifrado correctamente con la clave pública RSA del sistema", e);
        }
    }
}
