/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.mapper;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.mapper.FirDocfirmadoDetalleMapper;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.mapper.FirDocfirmadoMapper;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoDetalleEntity;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoEntity;
import ec.gob.iess.transversal.FirmaIessApi.model.FirDocfirmado;
import ec.gob.iess.transversal.FirmaIessApi.model.FirDocfirmadoDetalle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para FirDocfirmadoMapper y FirDocfirmadoDetalleMapper.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FirDocfirmadoMapper - Pruebas Unitarias")
class FirDocfirmadoMapperTest {

    @Mock private FirDocfirmadoDetalleMapper detalleMapper;
    @InjectMocks private FirDocfirmadoMapper mapper;

    private UUID idDoc;
    private FirDocfirmado dominio;
    private FirDocfirmadoEntity entity;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mapper, "detalleMapper", detalleMapper);
        idDoc = UUID.randomUUID();

        dominio = FirDocfirmado.builder()
                .idDoc(idDoc)
                .cedula("1003422365")
                .nombreDocumento("contrato.pdf")
                .firmasValidas(true)
                .integridadDocumento(true)
                .status("A")
                .createdBy("system")
                .createdAt(LocalDateTime.now())
                .build();

        entity = FirDocfirmadoEntity.builder()
                .idDoc(idDoc)
                .cedula("1003422365")
                .nombreDocumento("contrato.pdf")
                .firmasValidas(true)
                .integridadDocumento(true)
                .status("A")
                .createdBy("system")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── DOMAIN --> ENTITY ─────────────────────────────────────────────────────

    @Test
    @DisplayName("toEntity: mapea todos los campos correctamente")
    void toEntity_mapeaCamposCorrectamente() {
        FirDocfirmadoEntity resultado = mapper.toEntity(dominio);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdDoc()).isEqualTo(idDoc);
        assertThat(resultado.getCedula()).isEqualTo("1003422365");
        assertThat(resultado.getNombreDocumento()).isEqualTo("contrato.pdf");
        assertThat(resultado.isFirmasValidas()).isTrue();
        assertThat(resultado.isIntegridadDocumento()).isTrue();
        assertThat(resultado.getStatus()).isEqualTo("A");
    }

    @Test
    @DisplayName("toEntity: asigna status A cuando es null en el dominio")
    void toEntity_statusNull_asignaActivo() {
        dominio.setStatus(null);
        FirDocfirmadoEntity resultado = mapper.toEntity(dominio);
        assertThat(resultado.getStatus()).isEqualTo("A");
    }

    @Test
    @DisplayName("toEntity: retorna null cuando el dominio es null")
    void toEntity_dominioNull_retornaNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("toEntity: mapea lista de firmas cuando existen")
    void toEntity_conFirmas_mapearDetalles() {
        FirDocfirmadoDetalle detalle = FirDocfirmadoDetalle.builder()
                .cedula("1003422365").nombre("Juan Carlos").build();
        dominio.setFirmas(List.of(detalle));

        FirDocfirmadoDetalleEntity detalleEntity = FirDocfirmadoDetalleEntity.builder()
                .cedula("1003422365").nombre("Juan Carlos").build();
        when(detalleMapper.toEntity(any(), any())).thenReturn(detalleEntity);

        FirDocfirmadoEntity resultado = mapper.toEntity(dominio);

        assertThat(resultado.getFirmas()).hasSize(1);
        assertThat(resultado.getFirmas().get(0).getCedula()).isEqualTo("1003422365");
    }

    // ── ENTITY --> DOMAIN ─────────────────────────────────────────────────────

    @Test
    @DisplayName("toDomain: mapea todos los campos correctamente")
    void toDomain_mapeaCamposCorrectamente() {
        FirDocfirmado resultado = mapper.toDomain(entity);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdDoc()).isEqualTo(idDoc);
        assertThat(resultado.getCedula()).isEqualTo("1003422365");
        assertThat(resultado.getNombreDocumento()).isEqualTo("contrato.pdf");
        assertThat(resultado.isFirmasValidas()).isTrue();
        assertThat(resultado.isIntegridadDocumento()).isTrue();
        assertThat(resultado.getStatus()).isEqualTo("A");
        assertThat(resultado.getCreatedBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("toDomain: retorna null cuando la entidad es null")
    void toDomain_entidadNull_retornaNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    // ── DETALLE MAPPER ────────────────────────────────────────────────────────

    @Test
    @DisplayName("FirDocfirmadoDetalleMapper toEntity: mapea campos correctamente")
    void detalleToEntity_mapeaCamposCorrectamente() {
        FirDocfirmadoDetalleMapper detalleMapperReal = new FirDocfirmadoDetalleMapper();
        UUID idDet = UUID.randomUUID();

        FirDocfirmadoDetalle detalle = FirDocfirmadoDetalle.builder()
                .idDocDet(idDet)
                .cedula("1003422365")
                .nombre("Juan Carlos")
                .apellido("Estevez")
                .cargo("Analista")
                .status("A")
                .build();

        FirDocfirmadoDetalleEntity resultado = detalleMapperReal.toEntity(detalle, entity);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdDocDet()).isEqualTo(idDet);
        assertThat(resultado.getCedula()).isEqualTo("1003422365");
        assertThat(resultado.getNombre()).isEqualTo("Juan Carlos");
        assertThat(resultado.getApellido()).isEqualTo("Estevez");
        assertThat(resultado.getCargo()).isEqualTo("Analista");
        assertThat(resultado.getStatus()).isEqualTo("A");
        assertThat(resultado.getFirmados()).isEqualTo(entity);
    }

    @Test
    @DisplayName("FirDocfirmadoDetalleMapper toEntity: asigna status A cuando es null")
    void detalleToEntity_statusNull_asignaActivo() {
        FirDocfirmadoDetalleMapper detalleMapperReal = new FirDocfirmadoDetalleMapper();
        FirDocfirmadoDetalle detalle = FirDocfirmadoDetalle.builder()
                .cedula("1003422365").status(null).build();

        FirDocfirmadoDetalleEntity resultado = detalleMapperReal.toEntity(detalle, entity);
        assertThat(resultado.getStatus()).isEqualTo("A");
    }

    @Test
    @DisplayName("FirDocfirmadoDetalleMapper toDomain: mapea campos correctamente")
    void detalleToDomain_mapeaCamposCorrectamente() {
        FirDocfirmadoDetalleMapper detalleMapperReal = new FirDocfirmadoDetalleMapper();
        UUID idDet = UUID.randomUUID();

        FirDocfirmadoDetalleEntity detalleEntity = FirDocfirmadoDetalleEntity.builder()
                .idDocDet(idDet)
                .firmados(entity)
                .cedula("1003422365")
                .nombre("Juan Carlos")
                .status("A")
                .build();

        FirDocfirmadoDetalle resultado = detalleMapperReal.toDomain(detalleEntity);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdDocDet()).isEqualTo(idDet);
        assertThat(resultado.getIdDoc()).isEqualTo(idDoc);
        assertThat(resultado.getCedula()).isEqualTo("1003422365");
        assertThat(resultado.getNombre()).isEqualTo("Juan Carlos");
    }

    @Test
    @DisplayName("FirDocfirmadoDetalleMapper toDomain: retorna null cuando entidad es null")
    void detalleToDomain_null_retornaNull() {
        FirDocfirmadoDetalleMapper detalleMapperReal = new FirDocfirmadoDetalleMapper();
        assertThat(detalleMapperReal.toDomain(null)).isNull();
    }
}
