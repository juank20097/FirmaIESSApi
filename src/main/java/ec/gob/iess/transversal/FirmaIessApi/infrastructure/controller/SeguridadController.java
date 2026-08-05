/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.HybridEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint que expone la clave publica RSA para que los clientes
 * encripten el payload antes de llamar a los servicios de firma.
 * Solo disponible cuando RSA_ENABLED=true.
 */
@Slf4j
@RestController
@RequestMapping("/iess/seguridad")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = {"http://192.168.*.*:*", "https://192.168.*.*:*"})
@ConditionalOnProperty(name = "rsa.enabled", havingValue = "true")
public class SeguridadController {

    private final HybridEncryptionService encryptionService;

    /**
     * Retorna la clave publica RSA en base64.
     * GET /api/iess/seguridad/clave-publica
     */
    @GetMapping("/clave-publica")
    public ResponseEntity<Map<String, String>> obtenerClavePublica() {
        log.info("SeguridadController: clave publica solicitada");
        return ResponseEntity.ok(Map.of(
            "clavePublica", encryptionService.getPublicKeyBase64(),
            "algoritmo",    "RSA/ECB/OAEPPadding con SHA-256 y MGF1(SHA-256)",
            "cifradoDatos", "AES/GCM/NoPadding",
            "descripcion",  "Encripte pkcs12 y password con AES-256-GCM. Encripte la clave AES con esta clave RSA."
        ));
    }
}
