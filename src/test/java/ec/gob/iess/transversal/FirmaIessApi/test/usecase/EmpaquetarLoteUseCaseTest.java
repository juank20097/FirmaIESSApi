/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.usecase;

import ec.gob.iess.transversal.FirmaIessApi.application.port.StoragePort;
import ec.gob.iess.transversal.FirmaIessApi.application.usecase.EmpaquetarLoteUseCase;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.EmpaquetarResponse;
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
 * Pruebas unitarias para EmpaquetarLoteUseCase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmpaquetarLoteUseCase - Pruebas Unitarias")
class EmpaquetarLoteUseCaseTest {

    @Mock private StoragePort storagePort;
    @InjectMocks private EmpaquetarLoteUseCase useCase;

    private String fecha;
    private String prefijo;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "zipNombre", "documentos_firmados_iess");
        ReflectionTestUtils.setField(useCase, "urlExpiracionHoras", 24);
        fecha  = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        prefijo = fecha + "/temp_lote_001/";
    }

    // ── EMPAQUETAR ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("empaquetar: retorna ERROR cuando no hay PDFs firmados")
    void empaquetar_sinPdfs_retornaError() {
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of());

        EmpaquetarResponse resp = useCase.empaquetar("lote_001");

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getMensaje()).contains("lote_001");
        assertThat(resp.getTotalZips()).isEqualTo(0);
        verify(storagePort, never()).almacenarBytes(any(), any(), any());
    }

    @Test
    @DisplayName("empaquetar: genera 1 ZIP para un solo PDF firmado")
    void empaquetar_unPdf_generaUnZip() {
        String pdfPath = prefijo + "contrato_signed.pdf";
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of(pdfPath));
        when(storagePort.obtenerBytes(pdfPath)).thenReturn(new byte[]{1, 2, 3, 4});

        EmpaquetarResponse resp = useCase.empaquetar("lote_001");

        assertThat(resp.getEstado()).isEqualTo("OK");
        assertThat(resp.getTotalZips()).isEqualTo(1);
        assertThat(resp.getTotalDocumentos()).isEqualTo(1);
        verify(storagePort).almacenarBytes(any(), contains("documentos_firmados_iess.zip"), eq("application/zip"));
        verify(storagePort).eliminarObjeto(pdfPath);
    }

    @Test
    @DisplayName("empaquetar: genera 1 ZIP para multiples PDFs del mismo lote")
    void empaquetar_variospdfs_generaUnZip() {
        String pdf1 = prefijo + "doc1_signed.pdf";
        String pdf2 = prefijo + "doc2_signed.pdf";
        String pdf3 = prefijo + "doc3_signed.pdf";
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of(pdf1, pdf2, pdf3));
        when(storagePort.obtenerBytes(pdf1)).thenReturn(new byte[100]);
        when(storagePort.obtenerBytes(pdf2)).thenReturn(new byte[100]);
        when(storagePort.obtenerBytes(pdf3)).thenReturn(new byte[100]);

        EmpaquetarResponse resp = useCase.empaquetar("lote_001");

        assertThat(resp.getEstado()).isEqualTo("OK");
        assertThat(resp.getTotalZips()).isEqualTo(1);
        assertThat(resp.getTotalDocumentos()).isEqualTo(3);
        verify(storagePort, times(3)).eliminarObjeto(any());
    }

    @Test
    @DisplayName("empaquetar: ignora archivos que no son _signed.pdf")
    void empaquetar_ignoraArchivosNoSignedPdf() {
        String pdfFirmado = prefijo + "contrato_signed.pdf";
        String zipExistente = prefijo + "documentos_firmados_iess.zip";
        String otroCosa = prefijo + "readme.txt";
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of(pdfFirmado, zipExistente, otroCosa));
        when(storagePort.obtenerBytes(pdfFirmado)).thenReturn(new byte[]{1, 2, 3});

        EmpaquetarResponse resp = useCase.empaquetar("lote_001");

        assertThat(resp.getEstado()).isEqualTo("OK");
        assertThat(resp.getTotalDocumentos()).isEqualTo(1);
        verify(storagePort, never()).obtenerBytes(zipExistente);
        verify(storagePort, never()).obtenerBytes(otroCosa);
    }

    @Test
    @DisplayName("empaquetar: retorna ERROR cuando falla la lectura de bytes")
    void empaquetar_falloLecturaBytes_retornaError() {
        String pdfPath = prefijo + "contrato_signed.pdf";
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of(pdfPath));
        when(storagePort.obtenerBytes(pdfPath)).thenThrow(new RuntimeException("MinIO error"));

        EmpaquetarResponse resp = useCase.empaquetar("lote_001");

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getMensaje()).contains("MinIO error");
    }

    @Test
    @DisplayName("empaquetar: elimina PDFs individuales despues de empaquetar")
    void empaquetar_eliminaPdfsIndividualesDespuesDeZip() {
        String pdf1 = prefijo + "doc1_signed.pdf";
        String pdf2 = prefijo + "doc2_signed.pdf";
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of(pdf1, pdf2));
        when(storagePort.obtenerBytes(pdf1)).thenReturn(new byte[50]);
        when(storagePort.obtenerBytes(pdf2)).thenReturn(new byte[50]);

        useCase.empaquetar("lote_001");

        verify(storagePort).eliminarObjeto(pdf1);
        verify(storagePort).eliminarObjeto(pdf2);
    }

    @Test
    @DisplayName("empaquetar: fallo al eliminar PDF no detiene el proceso")
    void empaquetar_falloEliminarPdf_noPropagaExcepcion() {
        String pdf1 = prefijo + "doc1_signed.pdf";
        when(storagePort.listarObjetos(prefijo)).thenReturn(List.of(pdf1));
        when(storagePort.obtenerBytes(pdf1)).thenReturn(new byte[]{1, 2, 3});
        doThrow(new RuntimeException("error al eliminar"))
                .when(storagePort).eliminarObjeto(pdf1);

        EmpaquetarResponse resp = useCase.empaquetar("lote_001");

        // Debe completar igual aunque falle el eliminar
        assertThat(resp.getEstado()).isEqualTo("OK");
    }
}
