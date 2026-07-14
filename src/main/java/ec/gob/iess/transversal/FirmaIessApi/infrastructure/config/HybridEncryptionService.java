/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

/**
 * Servicio de cifrado hibrido RSA + AES-256-GCM.
 *
 * <p>Esquema:</p>
 * <ul>
 *   <li>El cliente encripta los datos grandes (pkcs12, password) con AES-256-GCM</li>
 *   <li>El cliente encripta la clave AES con RSA-OAEP (clave publica del servidor)</li>
 *   <li>El servidor desencripta la clave AES con su clave privada RSA</li>
 *   <li>El servidor desencripta los datos con la clave AES recuperada</li>
 * </ul>
 *
 * <p>Requiere RSA_ENABLED=true en el .env (usa los beans del RsaConfig).</p>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rsa.enabled", havingValue = "true")
public class HybridEncryptionService {

    private static final String RSA_ALGORITHM  = "RSA/ECB/OAEPPadding";
    private static final String AES_ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LENGTH  = 12;
    private static final int    GCM_TAG_LENGTH = 128;

    // Spec OAEP con SHA-256 para hash y MGF1 -- compatible con el cliente Python
    private static final OAEPParameterSpec OAEP_SPEC = new OAEPParameterSpec(
            "SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT
    );

    /** Bean provisto por RsaConfig (ConditionalOnProperty rsa.enabled=true). */
    private final PrivateKey rsaPrivateKey;

    /** Bean provisto por RsaConfig (ConditionalOnProperty rsa.enabled=true). */
    private final PublicKey rsaPublicKey;

    /**
     * Desencripta la clave AES usando la clave privada RSA del servidor.
     */
    public SecretKey desencriptarClaveAes(String claveAesCifradaBase64) throws Exception {
        byte[] claveAesCifrada = Base64.getDecoder().decode(claveAesCifradaBase64);
        log.debug("HybridEncryptionService: tamano claveAesCifrada en bytes: {}", claveAesCifrada.length);
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey, OAEP_SPEC);
        byte[] claveAesBytes = cipher.doFinal(claveAesCifrada);
        return new SecretKeySpec(claveAesBytes, "AES");
    }

    /**
     * Desencripta datos con AES-256-GCM.
     * Formato del dato cifrado: IV (12 bytes) + ciphertext.
     */
    public String desencriptarAes(String datoCifradoBase64, SecretKey claveAes) throws Exception {
        byte[] datos      = Base64.getDecoder().decode(datoCifradoBase64);
        byte[] iv         = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[datos.length - GCM_IV_LENGTH];
        System.arraycopy(datos, 0,             iv,         0, GCM_IV_LENGTH);
        System.arraycopy(datos, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, claveAes, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return new String(cipher.doFinal(ciphertext));
    }

    /**
     * Metodo principal: desencripta pkcs12 y password del payload cifrado.
     *
     * @return array [pkcs12EnClaro, passwordEnClaro]
     */
    public String[] desencriptarPayload(String pkcs12Cifrado,
                                         String passwordCifrado,
                                         String claveAesCifrada) throws Exception {
        SecretKey claveAes = desencriptarClaveAes(claveAesCifrada);
        String    pkcs12   = desencriptarAes(pkcs12Cifrado,   claveAes);
        String    password = desencriptarAes(passwordCifrado, claveAes);
        log.debug("HybridEncryptionService: payload desencriptado correctamente");
        return new String[]{ pkcs12, password };
    }

    /**
     * Retorna la clave publica RSA en base64 para que los clientes encripten.
     */
    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(rsaPublicKey.getEncoded());
    }
}
