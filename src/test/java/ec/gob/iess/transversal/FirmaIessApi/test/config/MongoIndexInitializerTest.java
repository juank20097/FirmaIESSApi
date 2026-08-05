/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.MongoIndexInitializer;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MongoIndexInitializer - Pruebas Unitarias")
class MongoIndexInitializerTest {

    @Mock private MongoTemplate mongoTemplate;
    @Mock private EntityManagerFactory entityManagerFactory;
    @Mock private Metamodel metamodel;
    @Mock private EntityType<?> entityType;
    @Mock private IndexOperations indexOperations;

    private MongoIndexInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new MongoIndexInitializer();
        ReflectionTestUtils.setField(initializer, "mongoTemplate", mongoTemplate);
        ReflectionTestUtils.setField(initializer, "entityManagerFactory", entityManagerFactory);
    }

    @Test
    @DisplayName("inicializarIndices: no hace nada cuando no hay entidades JPA registradas")
    void inicializarIndices_sinEntidades_noHaceNada() {
        when(entityManagerFactory.getMetamodel()).thenReturn(metamodel);
        when(metamodel.getEntities()).thenReturn(Set.of());

        initializer.inicializarIndices();

        verifyNoInteractions(mongoTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("inicializarIndices: crea la coleccion y el indice TTL cuando la coleccion no existe")
    void inicializarIndices_coleccionInexistente_creaColeccionEIndice() {
        when(entityManagerFactory.getMetamodel()).thenReturn(metamodel);
        doReturn("FirDocfirmado").when(entityType).getName();
        when(metamodel.getEntities()).thenReturn((Set) Set.of(entityType));
        when(mongoTemplate.collectionExists("AUD_GEN.FIRDOCFIRMADO")).thenReturn(false);
        when(mongoTemplate.indexOps("AUD_GEN.FIRDOCFIRMADO")).thenReturn(indexOperations);

        initializer.inicializarIndices();

        verify(mongoTemplate).createCollection("AUD_GEN.FIRDOCFIRMADO");
        verify(indexOperations).ensureIndex(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("inicializarIndices: no crea la coleccion cuando ya existe, solo el indice")
    void inicializarIndices_coleccionExistente_soloCreaIndice() {
        when(entityManagerFactory.getMetamodel()).thenReturn(metamodel);
        doReturn("Certificado").when(entityType).getName();
        when(metamodel.getEntities()).thenReturn((Set) Set.of(entityType));
        when(mongoTemplate.collectionExists("AUD_GEN.CERTIFICADO")).thenReturn(true);
        when(mongoTemplate.indexOps("AUD_GEN.CERTIFICADO")).thenReturn(indexOperations);

        initializer.inicializarIndices();

        verify(mongoTemplate, never()).createCollection(anyString());
        verify(indexOperations).ensureIndex(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("inicializarIndices: continua sin lanzar excepcion cuando falla una coleccion")
    void inicializarIndices_falloEnColeccion_noPropagaExcepcion() {
        when(entityManagerFactory.getMetamodel()).thenReturn(metamodel);
        doReturn("Certificado").when(entityType).getName();
        when(metamodel.getEntities()).thenReturn((Set) Set.of(entityType));
        when(mongoTemplate.collectionExists("AUD_GEN.CERTIFICADO"))
                .thenThrow(new RuntimeException("mongo caido"));

        initializer.inicializarIndices();

        verify(mongoTemplate).collectionExists("AUD_GEN.CERTIFICADO");
    }
}
