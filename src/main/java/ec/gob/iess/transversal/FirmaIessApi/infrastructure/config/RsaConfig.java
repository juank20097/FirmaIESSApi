/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * <b> Configuración condicional del par de claves RSA usado para cifrar
 * campos sensibles puntuales (cédula, correo) enviados por consumidores
 * externos. Las claves se leen en formato Base64 (DER, sin cabeceras PEM)
 * desde RSA_PUBLIC_KEY y RSA_PRIVATE_KEY (provistas vía Vault o .env).
 * Solo se activa cuando RSA_ENABLED=true en el .env. </b>
 *
 * @author Juan Carlos Estévez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estévez Hidalgo , Date: 25 jun 2026]
 *          </p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "rsa.enabled", havingValue = "true")
public class RsaConfig {

    /** Clave pública RSA en Base64 (formato X.509/DER, sin cabeceras PEM). */
    @Value("${rsa.public-key}")
    private String publicKeyBase64;

    /** Clave privada RSA en Base64 (formato PKCS8/DER, sin cabeceras PEM). */
    @Value("${rsa.private-key}")
    private String privateKeyBase64;

    /**
     * <b> Construye el bean PublicKey a partir del Base64 configurado. </b>
     *
     * @return clave pública RSA
     * @throws RuntimeException si la clave no puede ser parseada
     */
    @Bean
    public PublicKey rsaPublicKey() {
        try {
            byte[] bytes = Base64.getDecoder().decode(limpiar(publicKeyBase64));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            log.error("RSA: error al construir la clave pública. Causa: {}", e.getMessage());
            throw new RuntimeException("No se pudo cargar la clave pública RSA", e);
        }
    }

    /**
     * <b> Construye el bean PrivateKey a partir del Base64 configurado. </b>
     *
     * @return clave privada RSA
     * @throws RuntimeException si la clave no puede ser parseada
     */
    @Bean
    public PrivateKey rsaPrivateKey() {
        try {
            byte[] bytes = Base64.getDecoder().decode(limpiar(privateKeyBase64));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            log.error("RSA: error al construir la clave privada. Causa: {}", e.getMessage());
            throw new RuntimeException("No se pudo cargar la clave privada RSA", e);
        }
    }

    /**
     * <b> Quita cabeceras PEM y espacios en blanco si el valor configurado
     * viene en formato PEM en lugar de Base64 puro. </b>
     *
     * @param valor valor crudo de la propiedad
     * @return Base64 limpio, sin cabeceras ni saltos de línea
     */
    private String limpiar(String valor) {
        return valor
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
    }
}
