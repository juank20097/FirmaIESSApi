/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.CertificadoJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/movil")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = {"http://192.168.*.*:*", "https://192.168.*.*:*"})
public class CertificadoController {

    private final CertificadoJpaRepository certificadoRepo;

    /**
     * Retorna el certificado y password en base64 para un sistema.
     * GET /api/iess/movil/certificado/{sistema}
     */
    @GetMapping("/certificado/{sistema}")
    public ResponseEntity<Map<String, String>> obtenerCertificado(@PathVariable String sistema) {
        return certificadoRepo.findBySistema(sistema)
            .map(cert -> ResponseEntity.ok(Map.of(
                "cedula",      cert.getCedula(),
                "certificado", cert.getCertificado(),
                "password",    cert.getPassword()
            )))
            .orElse(ResponseEntity.status(404).body(
                Map.of("error", "No se encontro certificado para el sistema: " + sistema)
            ));
    }
}
