/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.usecase;

import ec.gob.iess.transversal.FirmaIessApi.application.port.StoragePort;
import ec.gob.iess.transversal.FirmaIessApi.application.usecase.EnlacesLoteUseCase;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.EnlacesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para EnlacesLoteUseCase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnlacesLoteUseCase - Pruebas Unitarias")
class EnlacesLoteUseCaseTest {

    @Mock private StoragePort storagePort;
    @InjectMocks private EnlacesLoteUseCase useCase;

    private String fecha;
    private String prefijo;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "urlExpiracionHoras", 24);
        fecha  = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        prefijo = fecha + "/temp_lote_001/";
    }

    // ── SIN DOCUMENTOS ────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerEnlaces: retorna ERROR cuando carpeta esta vacia")
    void obtenerEnlaces_carpetaVacia_retornaError() {
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of());

        EnlacesResponse resp = useCase.obtenerEnlaces("lote_001");

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getMensaje()).contains("lote_001");
        assertThat(resp.getEnlaces()).isEmpty();
        verify(storagePort, never()).generarUrlPresignada(any(), anyInt());
    }

    // ── 1 PDF DIRECTO ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerEnlaces: retorna link directo al PDF cuando hay 1 solo PDF firmado")
    void obtenerEnlaces_unSoloPdf_retornaLinkDirecto() {
        String pdfPath = prefijo + "contrato_signed.pdf";
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of(pdfPath));
        when(storagePort.generarUrlPresignada(pdfPath, 24)).thenReturn("http://minio/contrato_signed.pdf?token=abc");

        EnlacesResponse resp = useCase.obtenerEnlaces("lote_001");

        assertThat(resp.getEstado()).isEqualTo("OK");
        assertThat(resp.getEnlaces()).hasSize(1);
        assertThat(resp.getEnlaces().get(0).getNombre()).isEqualTo("contrato_signed.pdf");
        assertThat(resp.getEnlaces().get(0).getUrl()).contains("contrato_signed.pdf");
        assertThat(resp.getEnlaces().get(0).getHorasExpiracion()).isEqualTo(24);
    }

    // ── MULTIPLES PDFs SIN EMPAQUETAR ─────────────────────────────────────────

    @Test
    @DisplayName("obtenerEnlaces: retorna ERROR cuando hay multiples PDFs sin empaquetar")
    void obtenerEnlaces_multiplesPdfssinZip_retornaError() {
        String pdf1 = prefijo + "doc1_signed.pdf";
        String pdf2 = prefijo + "doc2_signed.pdf";
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of(pdf1, pdf2));

        EnlacesResponse resp = useCase.obtenerEnlaces("lote_001");

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getMensaje()).contains("empaquetar");
        assertThat(resp.getEnlaces()).isEmpty();
        verify(storagePort, never()).generarUrlPresignada(any(), anyInt());
    }

    // ── ZIPS GENERADOS ────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerEnlaces: retorna 1 enlace por cada ZIP cuando hay ZIPs")
    void obtenerEnlaces_conZips_retornaEnlacesPorZip() {
        String zip1 = prefijo + "documentos_firmados_iess_1.zip";
        String zip2 = prefijo + "documentos_firmados_iess_2.zip";
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of(zip1, zip2));
        when(storagePort.generarUrlPresignada(zip1, 24)).thenReturn("http://minio/zip1?token=aaa");
        when(storagePort.generarUrlPresignada(zip2, 24)).thenReturn("http://minio/zip2?token=bbb");

        EnlacesResponse resp = useCase.obtenerEnlaces("lote_001");

        assertThat(resp.getEstado()).isEqualTo("OK");
        assertThat(resp.getEnlaces()).hasSize(2);
        assertThat(resp.getMensaje()).contains("2");
    }

    @Test
    @DisplayName("obtenerEnlaces: retorna 1 enlace para 1 ZIP con nombre estandar")
    void obtenerEnlaces_unZip_retornaUnEnlace() {
        String zip = prefijo + "documentos_firmados_iess.zip";
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of(zip));
        when(storagePort.generarUrlPresignada(zip, 24)).thenReturn("http://minio/zip?token=xyz");

        EnlacesResponse resp = useCase.obtenerEnlaces("lote_001");

        assertThat(resp.getEstado()).isEqualTo("OK");
        assertThat(resp.getEnlaces()).hasSize(1);
        assertThat(resp.getEnlaces().get(0).getNombre()).isEqualTo("documentos_firmados_iess.zip");
        assertThat(resp.getEnlaces().get(0).getHorasExpiracion()).isEqualTo(24);
    }

    // ── FALLO EN STORAGE ──────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerEnlaces: retorna ERROR cuando falla la generacion de URL presignada")
    void obtenerEnlaces_falloUrlPresignada_retornaError() {
        String pdfPath = prefijo + "contrato_signed.pdf";
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of(pdfPath));
        when(storagePort.generarUrlPresignada(any(), anyInt()))
                .thenThrow(new RuntimeException("MinIO no disponible"));

        EnlacesResponse resp = useCase.obtenerEnlaces("lote_001");

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getMensaje()).contains("MinIO no disponible");
    }

    @Test
    @DisplayName("obtenerEnlaces: retorna ERROR cuando falla el listado de objetos")
    void obtenerEnlaces_falloListado_retornaError() {
        when(storagePort.listarObjetos(any()))
                .thenThrow(new RuntimeException("error de conexion"));

        EnlacesResponse resp = useCase.obtenerEnlaces("lote_001");

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getMensaje()).contains("error de conexion");
    }
}
