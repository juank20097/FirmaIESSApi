/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.security;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.security.RsaCipherUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RsaCipherUtil - Pruebas Unitarias")
class RsaCipherUtilTest {

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

    private RsaCipherUtil newUtil() {
        return new RsaCipherUtil(publicKey, privateKey);
    }

    @Test
    @DisplayName("encrypt + decrypt: recupera el texto plano original")
    void encryptYDecrypt_recuperaTextoOriginal() {
        RsaCipherUtil util = newUtil();

        String cifrado = util.encrypt("1003422365");
        String descifrado = util.decrypt(cifrado);

        assertThat(descifrado).isEqualTo("1003422365");
        assertThat(cifrado).isNotEqualTo("1003422365");
    }

    @Test
    @DisplayName("encrypt: retorna null cuando el texto plano es null")
    void encrypt_textoNull_retornaNull() {
        assertThat(newUtil().encrypt(null)).isNull();
    }

    @Test
    @DisplayName("encrypt: retorna vacio cuando el texto plano esta en blanco")
    void encrypt_textoEnBlanco_retornaMismoValor() {
        assertThat(newUtil().encrypt("   ")).isEqualTo("   ");
    }

    @Test
    @DisplayName("decrypt: retorna null cuando el texto cifrado es null")
    void decrypt_textoNull_retornaNull() {
        assertThat(newUtil().decrypt(null)).isNull();
    }

    @Test
    @DisplayName("decrypt: retorna vacio cuando el texto cifrado esta en blanco")
    void decrypt_textoEnBlanco_retornaMismoValor() {
        assertThat(newUtil().decrypt("")).isEmpty();
    }

    @Test
    @DisplayName("decrypt: lanza excepcion cuando el valor no esta cifrado correctamente")
    void decrypt_valorInvalido_lanzaExcepcion() {
        RsaCipherUtil util = newUtil();

        assertThatThrownBy(() -> util.decrypt("no-es-base64-cifrado-valido"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("encrypt: lanza excepcion cuando la clave publica es invalida")
    void encrypt_claveInvalida_lanzaExcepcion() {
        RsaCipherUtil util = new RsaCipherUtil(null, privateKey);

        assertThatThrownBy(() -> util.encrypt("texto"))
                .isInstanceOf(IllegalStateException.class);
    }
}
