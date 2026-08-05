/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.controller;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.CertificadoController;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.CertificadoEntity;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.CertificadoJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CertificadoController - Pruebas Unitarias")
class CertificadoControllerTest {

    @Mock private CertificadoJpaRepository certificadoRepo;
    @InjectMocks private CertificadoController controller;

    @Test
    @DisplayName("obtenerCertificado: retorna 200 con los datos del certificado cuando existe")
    void obtenerCertificado_existente_retorna200() {
        CertificadoEntity entity = CertificadoEntity.builder()
                .sistema("iess").cedula("1003422365")
                .certificado("base64cert").password("clave123").build();
        when(certificadoRepo.findBySistema("iess")).thenReturn(Optional.of(entity));

        ResponseEntity<Map<String, String>> resp = controller.obtenerCertificado("iess");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("cedula", "1003422365");
        assertThat(resp.getBody()).containsEntry("certificado", "base64cert");
        assertThat(resp.getBody()).containsEntry("password", "clave123");
    }

    @Test
    @DisplayName("obtenerCertificado: retorna 404 cuando no existe el sistema")
    void obtenerCertificado_inexistente_retorna404() {
        when(certificadoRepo.findBySistema("desconocido")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, String>> resp = controller.obtenerCertificado("desconocido");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsEntry("error",
                "No se encontro certificado para el sistema: desconocido");
    }
}
