/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.RsaConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RsaConfig - Pruebas Unitarias")
class RsaConfigTest {

    private static String publicKeyBase64;
    private static String privateKeyBase64;

    private RsaConfig config;

    @BeforeAll
    static void generarClaves() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    @BeforeEach
    void setUp() {
        config = new RsaConfig();
    }

    @Test
    @DisplayName("rsaPublicKey: construye la clave publica a partir del Base64 configurado")
    void rsaPublicKey_construyeClave() {
        ReflectionTestUtils.setField(config, "publicKeyBase64", publicKeyBase64);

        PublicKey key = config.rsaPublicKey();

        assertThat(key).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    @DisplayName("rsaPublicKey: acepta formato PEM con cabeceras y saltos de linea")
    void rsaPublicKey_formatoPem_construyeClave() {
        String pem = "-----BEGIN PUBLIC KEY-----\n" + publicKeyBase64 + "\n-----END PUBLIC KEY-----";
        ReflectionTestUtils.setField(config, "publicKeyBase64", pem);

        PublicKey key = config.rsaPublicKey();

        assertThat(key).isNotNull();
    }

    @Test
    @DisplayName("rsaPublicKey: lanza excepcion cuando la clave no es un Base64 valido")
    void rsaPublicKey_claveInvalida_lanzaExcepcion() {
        ReflectionTestUtils.setField(config, "publicKeyBase64", "no-es-una-clave-valida-!!!");

        assertThatThrownBy(() -> config.rsaPublicKey())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se pudo cargar la clave pública RSA");
    }

    @Test
    @DisplayName("rsaPrivateKey: construye la clave privada a partir del Base64 configurado")
    void rsaPrivateKey_construyeClave() {
        ReflectionTestUtils.setField(config, "privateKeyBase64", privateKeyBase64);

        PrivateKey key = config.rsaPrivateKey();

        assertThat(key).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    @DisplayName("rsaPrivateKey: lanza excepcion cuando la clave no es un Base64 valido")
    void rsaPrivateKey_claveInvalida_lanzaExcepcion() {
        ReflectionTestUtils.setField(config, "privateKeyBase64", "no-es-una-clave-valida-!!!");

        assertThatThrownBy(() -> config.rsaPrivateKey())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se pudo cargar la clave privada RSA");
    }
}
