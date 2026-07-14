/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.usecase;

import ec.gob.iess.transversal.FirmaIessApi.application.port.StoragePort;
import ec.gob.iess.transversal.FirmaIessApi.application.usecase.FirDocfirmadoUseCase;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.LoteContextHolder;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.mapper.FirDocfirmadoMapper;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoEntity;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoJpaRepository;
import ec.gob.iess.transversal.FirmaIessApi.model.FirDocfirmado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para FirDocfirmadoUseCase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FirDocfirmadoUseCase - Pruebas Unitarias")
class FirDocfirmadoUseCaseTest {

    @Mock private FirDocfirmadoJpaRepository repository;
    @Mock private FirDocfirmadoMapper mapper;
    @Mock private StoragePort storagePort;
    @Mock private LoteContextHolder loteContextHolder;
    @InjectMocks private FirDocfirmadoUseCase useCase;

    private FirDocfirmado documento;
    private FirDocfirmadoEntity entity;
    private UUID idDoc;

    @BeforeEach
    void setUp() {
        idDoc = UUID.randomUUID();
        documento = FirDocfirmado.builder()
                .cedula("1003422365")
                .nombreDocumento("contrato.pdf")
                .firmasValidas(true)
                .integridadDocumento(true)
                .build();
        entity = FirDocfirmadoEntity.builder()
                .idDoc(idDoc)
                .cedula("1003422365")
                .nombreDocumento("contrato.pdf")
                .status("A")
                .build();
    }

    // ── CREAR CON IDLOTE DESDE CONTEXTO ──────────────────────────────────────

    @Test
    @DisplayName("crear: obtiene idLote del contexto y persiste el documento")
    void crear_obtieneLoteDeContexto_exitoso() {
        when(loteContextHolder.obtener("1003422365")).thenReturn("lote_001");
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toDomain(any())).thenReturn(documento);

        FirDocfirmado resultado = useCase.crear(documento);

        assertThat(resultado).isNotNull();
        verify(loteContextHolder).obtener("1003422365");
        verify(repository).save(any());
    }

    @Test
    @DisplayName("crear: asigna status A antes de persistir")
    void crear_asignaStatusActivo() {
        documento.setStatus(null);
        when(loteContextHolder.obtener(any())).thenReturn("lote_001");
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toDomain(any())).thenReturn(documento);

        useCase.crear(documento);

        assertThat(documento.getStatus()).isEqualTo("A");
    }

    // ── CREAR CON IDLOTE EXPLICITO ────────────────────────────────────────────

    @Test
    @DisplayName("crear con idLote: sube PDF a MinIO cuando archivo no es nulo")
    void crearConIdLote_subePDFaMinIO() {
        documento.setArchivo(new byte[]{1, 2, 3});
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toDomain(any())).thenReturn(documento);

        useCase.crear(documento, "lote_001");

        verify(storagePort).almacenarBytes(any(), contains("lote_001"), eq("application/pdf"));
    }

    @Test
    @DisplayName("crear con idLote: no llama a MinIO cuando archivo es nulo")
    void crearConIdLote_sinArchivo_noLlamaMinIO() {
        documento.setArchivo(null);
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toDomain(any())).thenReturn(documento);

        useCase.crear(documento, "lote_001");

        verify(storagePort, never()).almacenarBytes(any(), any(), any());
    }

    @Test
    @DisplayName("crear con idLote: no llama a MinIO cuando archivo esta vacio")
    void crearConIdLote_archivoVacio_noLlamaMinIO() {
        documento.setArchivo(new byte[0]);
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toDomain(any())).thenReturn(documento);

        useCase.crear(documento, "lote_001");

        verify(storagePort, never()).almacenarBytes(any(), any(), any());
    }

    @Test
    @DisplayName("crear con idLote: fallo en MinIO no revierte la persistencia en BD")
    void crearConIdLote_falloMinIO_noPropagaExcepcion() {
        documento.setArchivo(new byte[]{1, 2, 3});
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toDomain(any())).thenReturn(documento);
        doThrow(new RuntimeException("MinIO no disponible"))
                .when(storagePort).almacenarBytes(any(), any(), any());

        // No debe lanzar excepcion -- el fallo de MinIO es silencioso
        FirDocfirmado resultado = useCase.crear(documento, "lote_001");

        assertThat(resultado).isNotNull();
        verify(repository).save(any());
    }

    @Test
    @DisplayName("crear con idLote: usa idDoc como carpeta cuando idLote es null")
    void crearConIdLote_idLoteNull_usaIdDoc() {
        documento.setArchivo(new byte[]{1, 2, 3});
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);
        FirDocfirmado domainConId = FirDocfirmado.builder()
                .idDoc(idDoc).cedula("1003422365")
                .nombreDocumento("contrato.pdf").build();
        when(mapper.toDomain(any())).thenReturn(domainConId);

        useCase.crear(documento, null);

        verify(storagePort).almacenarBytes(any(), contains("temp_" + idDoc), eq("application/pdf"));
    }
}
