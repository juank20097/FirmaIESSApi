/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.persistence;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.mongo.AuditoriaDocument;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.mongo.AuditoriaMongoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaMongoService - Pruebas Unitarias")
class AuditoriaMongoServiceTest {

    @Mock private MongoTemplate mongoTemplate;

    private AuditoriaMongoService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriaMongoService();
        ReflectionTestUtils.setField(service, "mongoTemplate", mongoTemplate);
    }

    @Test
    @DisplayName("insertarAuditoria: inserta el documento en la coleccion indicada")
    void insertarAuditoria_insertaDocumento() {
        AuditoriaDocument doc = AuditoriaDocument.builder().audOperation("I").build();

        service.insertarAuditoria(doc, "AUD_GEN.PERSONA");

        verify(mongoTemplate).insert(doc, "AUD_GEN.PERSONA");
    }

    @Test
    @DisplayName("insertarAuditoria: no propaga la excepcion cuando falla la escritura")
    void insertarAuditoria_fallaEscritura_noPropagaExcepcion() {
        AuditoriaDocument doc = AuditoriaDocument.builder().audOperation("I").build();
        doThrow(new RuntimeException("mongo caido")).when(mongoTemplate).insert(doc, "AUD_GEN.PERSONA");

        service.insertarAuditoria(doc, "AUD_GEN.PERSONA");

        verify(mongoTemplate).insert(doc, "AUD_GEN.PERSONA");
    }

    @Test
    @DisplayName("buscarPorUsuario: retorna los documentos encontrados por usuario")
    void buscarPorUsuario_retornaDocumentos() {
        AuditoriaDocument doc = AuditoriaDocument.builder().audAppUser("juan").build();
        when(mongoTemplate.find(any(Query.class), eq(AuditoriaDocument.class), eq("AUD_GEN.PERSONA")))
                .thenReturn(List.of(doc));

        List<AuditoriaDocument> resultado = service.buscarPorUsuario("AUD_GEN.PERSONA", "juan");

        assertThat(resultado).containsExactly(doc);
    }

    @Test
    @DisplayName("buscarPorEntidadYOperacion: retorna los documentos con la operacion indicada")
    void buscarPorEntidadYOperacion_retornaDocumentos() {
        AuditoriaDocument doc = AuditoriaDocument.builder().audOperation("I").build();
        when(mongoTemplate.find(any(Query.class), eq(AuditoriaDocument.class), eq("AUD_GEN.PERSONA")))
                .thenReturn(List.of(doc));

        List<AuditoriaDocument> resultado = service.buscarPorEntidadYOperacion("AUD_GEN.PERSONA", "I");

        assertThat(resultado).containsExactly(doc);
    }

    @Test
    @DisplayName("buscarPorRangoFechas: retorna los documentos dentro del rango")
    void buscarPorRangoFechas_retornaDocumentos() {
        AuditoriaDocument doc = AuditoriaDocument.builder().build();
        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now();
        when(mongoTemplate.find(any(Query.class), eq(AuditoriaDocument.class), eq("AUD_GEN.PERSONA")))
                .thenReturn(List.of(doc));

        List<AuditoriaDocument> resultado = service.buscarPorRangoFechas("AUD_GEN.PERSONA", desde, hasta);

        assertThat(resultado).containsExactly(doc);
    }
}
