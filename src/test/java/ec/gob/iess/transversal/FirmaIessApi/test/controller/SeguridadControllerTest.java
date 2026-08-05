/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.controller;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.HybridEncryptionService;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.SeguridadController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeguridadController - Pruebas Unitarias")
class SeguridadControllerTest {

    @Mock private HybridEncryptionService encryptionService;
    @InjectMocks private SeguridadController controller;

    @Test
    @DisplayName("obtenerClavePublica: retorna 200 con la clave publica y metadatos del algoritmo")
    void obtenerClavePublica_retorna200() {
        when(encryptionService.getPublicKeyBase64()).thenReturn("clavePublicaBase64");

        ResponseEntity<Map<String, String>> resp = controller.obtenerClavePublica();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("clavePublica", "clavePublicaBase64");
        assertThat(resp.getBody()).containsKey("algoritmo");
        assertThat(resp.getBody()).containsKey("cifradoDatos");
        assertThat(resp.getBody()).containsKey("descripcion");
    }
}
