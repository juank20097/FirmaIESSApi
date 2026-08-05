/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.HybridEncryptionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HybridEncryptionService - Pruebas Unitarias")
class HybridEncryptionServiceTest {

    private static final OAEPParameterSpec OAEP_SPEC = new OAEPParameterSpec(
            "SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT);

    private static PublicKey publicKey;
    private static PrivateKey privateKey;

    @BeforeAll
    static void generarClaves() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    private HybridEncryptionService newService() {
        return new HybridEncryptionService(privateKey, publicKey);
    }

    private String cifrarClaveAesConRsa(SecretKey claveAes) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SPEC);
        return Base64.getEncoder().encodeToString(cipher.doFinal(claveAes.getEncoded()));
    }

    private String cifrarConAes(String textoPlano, SecretKey claveAes) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, claveAes, new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(textoPlano.getBytes());
        byte[] resultado = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, resultado, 0, iv.length);
        System.arraycopy(ciphertext, 0, resultado, iv.length, ciphertext.length);
        return Base64.getEncoder().encodeToString(resultado);
    }

    @Test
    @DisplayName("desencriptarPayload: recupera pkcs12 y password originales")
    void desencriptarPayload_recuperaValoresOriginales() throws Exception {
        KeyGenerator aesGen = KeyGenerator.getInstance("AES");
        aesGen.init(256);
        SecretKey claveAes = aesGen.generateKey();

        String claveAesCifrada = cifrarClaveAesConRsa(claveAes);
        String pkcs12Cifrado = cifrarConAes("pkcs12-base64-contenido", claveAes);
        String passwordCifrado = cifrarConAes("miClaveSecreta", claveAes);

        String[] resultado = newService().desencriptarPayload(pkcs12Cifrado, passwordCifrado, claveAesCifrada);

        assertThat(resultado[0]).isEqualTo("pkcs12-base64-contenido");
        assertThat(resultado[1]).isEqualTo("miClaveSecreta");
    }

    @Test
    @DisplayName("desencriptarClaveAes: lanza excepcion cuando la clave cifrada es invalida")
    void desencriptarClaveAes_valorInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> newService().desencriptarClaveAes(
                Base64.getEncoder().encodeToString("no-es-una-clave-rsa-valida".getBytes())))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("getPublicKeyBase64: retorna la clave publica codificada en base64")
    void getPublicKeyBase64_retornaClaveCodificada() {
        String resultado = newService().getPublicKeyBase64();

        assertThat(resultado).isEqualTo(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
    }
}
