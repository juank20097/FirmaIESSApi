/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.usecase;

import ec.gob.iess.transversal.FirmaIessApi.application.usecase.FirLoteUseCase;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.HybridEncryptionService;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.LoteContextHolder;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarDocumentoItem;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarLoteRequest;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarLoteResponse;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarParametros;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoEntity;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para FirLoteUseCase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FirLoteUseCase - Pruebas Unitarias")
class FirLoteUseCaseTest {

    @Mock private FirDocfirmadoJpaRepository firmadosRepo;
    @Mock private RestTemplate restTemplate;
    @Mock private LoteContextHolder loteContextHolder;
    @Mock private HybridEncryptionService encryptionService;
    @InjectMocks private FirLoteUseCase useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "maxDocumentos",       10);
        ReflectionTestUtils.setField(useCase, "firmadigitalUrl",     "http://localhost:8080");
        ReflectionTestUtils.setField(useCase, "razon",               "Firma Digital IESS");
        ReflectionTestUtils.setField(useCase, "firmadigitalApiKey",  "apikey-test");
        ReflectionTestUtils.setField(useCase, "version",             "5.1.0");
        ReflectionTestUtils.setField(useCase, "sistema",             "iess");
    }

    // ── VALIDACIONES BASICAS ──────────────────────────────────────────────────

    @Test
    @DisplayName("firmar: retorna ERROR cuando documentos es null")
    void firmar_documentosNull_retornaError() {
        FirmarLoteRequest req = FirmarLoteRequest.builder()
                .idLote("lote_001").cedula("1003422365").build();

        FirmarLoteResponse resp = useCase.firmar(req);

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getMensaje()).contains("al menos un documento");
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("firmar: retorna ERROR cuando documentos esta vacio")
    void firmar_documentosVacio_retornaError() {
        FirmarLoteRequest req = FirmarLoteRequest.builder()
                .idLote("lote_001").cedula("1003422365")
                .documentos(List.of()).build();

        FirmarLoteResponse resp = useCase.firmar(req);

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("firmar: retorna ERROR cuando se excede el maximo de documentos")
    void firmar_excedeLimite_retornaError() {
        List<FirmarDocumentoItem> docs = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            docs.add(FirmarDocumentoItem.builder()
                    .nombre("doc" + i + ".pdf").documento("base64").build());
        }
        FirmarLoteRequest req = FirmarLoteRequest.builder()
                .idLote("lote_001").cedula("1003422365")
                .documentos(docs).build();

        FirmarLoteResponse resp = useCase.firmar(req);

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getMensaje()).contains("Maximo");
        verifyNoInteractions(restTemplate);
    }

    // ── CIFRADO ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("firmar: desencripta payload cuando claveAesCifrada esta presente")
    void firmar_payloadCifrado_desencripta() throws Exception {
        FirmarLoteRequest req = FirmarLoteRequest.builder()
                .idLote("lote_001").cedula("1003422365")
                .pkcs12Cifrado("pkcs12enc").passwordCifrado("passenc")
                .claveAesCifrada("claveAesEnc")
                .documentos(List.of(doc("contrato.pdf")))
                .parametros(params()).build();

        when(encryptionService.desencriptarPayload("pkcs12enc", "passenc", "claveAesEnc"))
                .thenReturn(new String[]{"pkcs12real", "passreal"});
        when(restTemplate.postForObject(contains("/servicio/documentos"), any(), eq(String.class)))
                .thenReturn("tokenJwtTest");
        when(restTemplate.postForObject(contains("/appfirmardocumentotransversal"), any(), eq(String.class)))
                .thenReturn(null);
        when(firmadosRepo.findByCedulaAndNombreDocumentoInAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(List.of(entity()));

        FirmarLoteResponse resp = useCase.firmar(req);

        verify(encryptionService).desencriptarPayload("pkcs12enc", "passenc", "claveAesEnc");
        assertThat(req.getPkcs12()).isEqualTo("pkcs12real");
        assertThat(req.getPassword()).isEqualTo("passreal");
        assertThat(resp.getEstado()).isEqualTo("OK");
    }

    @Test
    @DisplayName("firmar: retorna ERROR cuando falla el desencriptado")
    void firmar_falloDesencriptado_retornaError() throws Exception {
        FirmarLoteRequest req = FirmarLoteRequest.builder()
                .idLote("lote_001").cedula("1003422365")
                .pkcs12Cifrado("malo").passwordCifrado("malo").claveAesCifrada("malo")
                .documentos(List.of(doc("contrato.pdf")))
                .parametros(params()).build();

        when(encryptionService.desencriptarPayload(any(), any(), any()))
                .thenThrow(new Exception("Padding error"));

        FirmarLoteResponse resp = useCase.firmar(req);

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getMensaje()).contains("desencriptar");
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("firmar: no llama a encryptionService cuando no hay claveAesCifrada")
    void firmar_sinCifrado_noLlamaEncryptionService() throws Exception {
        FirmarLoteRequest req = FirmarLoteRequest.builder()
                .idLote("lote_001").cedula("1003422365")
                .pkcs12("pkcs12real").password("passreal")
                .documentos(List.of(doc("contrato.pdf")))
                .parametros(params()).build();

        when(restTemplate.postForObject(contains("/servicio/documentos"), any(), eq(String.class)))
                .thenReturn("tokenJwtTest");
        when(restTemplate.postForObject(contains("/appfirmardocumentotransversal"), any(), eq(String.class)))
                .thenReturn(null);
        when(firmadosRepo.findByCedulaAndNombreDocumentoInAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(List.of(entity()));

        useCase.firmar(req);

        verifyNoInteractions(encryptionService);
    }

    // ── FLUJO COMPLETO ────────────────────────────────────────────────────────

    @Test
    @DisplayName("firmar: retorna OK cuando todos los documentos son firmados exitosamente")
    void firmar_todosLosDocsFirmados_retornaOK() throws Exception {
        FirmarLoteRequest req = requestSinCifrado(List.of(
                doc("doc1.pdf"), doc("doc2.pdf")));

        when(restTemplate.postForObject(contains("/servicio/documentos"), any(), eq(String.class)))
                .thenReturn("tokenJwt");
        when(restTemplate.postForObject(contains("/appfirmardocumentotransversal"), any(), eq(String.class)))
                .thenReturn(null);
        when(firmadosRepo.findByCedulaAndNombreDocumentoInAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(List.of(entity(), entity()));

        FirmarLoteResponse resp = useCase.firmar(req);

        assertThat(resp.getEstado()).isEqualTo("OK");
        assertThat(resp.getTotalEnviados()).isEqualTo(2);
        assertThat(resp.getTotalFirmados()).isEqualTo(2);
        assertThat(resp.getTotalErrores()).isEqualTo(0);
        verify(loteContextHolder).registrar("1003422365", "lote_001");
        verify(loteContextHolder).limpiar("1003422365");
    }

    @Test
    @DisplayName("firmar: retorna PARCIAL cuando solo algunos documentos se firman")
    void firmar_algunosDocsFirmados_retornaParcial() throws Exception {
        FirmarLoteRequest req = requestSinCifrado(List.of(
                doc("doc1.pdf"), doc("doc2.pdf"), doc("doc3.pdf")));

        when(restTemplate.postForObject(contains("/servicio/documentos"), any(), eq(String.class)))
                .thenReturn("tokenJwt");
        when(restTemplate.postForObject(contains("/appfirmardocumentotransversal"), any(), eq(String.class)))
                .thenReturn(null);
        when(firmadosRepo.findByCedulaAndNombreDocumentoInAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(List.of(entity(), entity())); // solo 2 de 3

        FirmarLoteResponse resp = useCase.firmar(req);

        assertThat(resp.getEstado()).isEqualTo("PARCIAL");
        assertThat(resp.getTotalFirmados()).isEqualTo(2);
        assertThat(resp.getTotalErrores()).isEqualTo(1);
    }

    @Test
    @DisplayName("firmar: retorna ERROR cuando ningun documento se firma")
    void firmar_ningunDocFirmado_retornaError() throws Exception {
        FirmarLoteRequest req = requestSinCifrado(List.of(doc("doc1.pdf")));

        when(restTemplate.postForObject(contains("/servicio/documentos"), any(), eq(String.class)))
                .thenReturn("tokenJwt");
        when(restTemplate.postForObject(contains("/appfirmardocumentotransversal"), any(), eq(String.class)))
                .thenReturn(null);
        when(firmadosRepo.findByCedulaAndNombreDocumentoInAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(List.of());

        FirmarLoteResponse resp = useCase.firmar(req);

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getTotalFirmados()).isEqualTo(0);
    }

    @Test
    @DisplayName("firmar: retorna ERROR y limpia contexto cuando falla la llamada a WildFly")
    void firmar_falloWildFly_retornaErrorYLimpiaContexto() {
        FirmarLoteRequest req = requestSinCifrado(List.of(doc("doc1.pdf")));

        when(restTemplate.postForObject(contains("/servicio/documentos"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        FirmarLoteResponse resp = useCase.firmar(req);

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getMensaje()).contains("Error inesperado");
        verify(loteContextHolder).limpiar("1003422365");
    }

    @Test
    @DisplayName("firmar: registra y limpia el contexto de lote correctamente")
    void firmar_registraYLimpiaContexto() throws Exception {
        FirmarLoteRequest req = requestSinCifrado(List.of(doc("doc1.pdf")));

        when(restTemplate.postForObject(contains("/servicio/documentos"), any(), eq(String.class)))
                .thenReturn("tokenJwt");
        when(restTemplate.postForObject(contains("/appfirmardocumentotransversal"), any(), eq(String.class)))
                .thenReturn(null);
        when(firmadosRepo.findByCedulaAndNombreDocumentoInAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(List.of(entity()));

        useCase.firmar(req);

        verify(loteContextHolder).registrar("1003422365", "lote_001");
        verify(loteContextHolder).limpiar("1003422365");
    }

    @Test
    @DisplayName("firmar: usa parametros por defecto cuando el request no trae parametros")
    void firmar_sinParametros_usaValoresPorDefecto() throws Exception {
        FirmarLoteRequest req = FirmarLoteRequest.builder()
                .idLote("lote_001").cedula("1003422365")
                .pkcs12("pkcs12real").password("passreal")
                .documentos(List.of(doc("doc1.pdf")))
                .parametros(null).build();

        when(restTemplate.postForObject(contains("/servicio/documentos"), any(), eq(String.class)))
                .thenReturn("tokenJwt");
        when(restTemplate.postForObject(contains("/appfirmardocumentotransversal"), any(), eq(String.class)))
                .thenReturn(null);
        when(firmadosRepo.findByCedulaAndNombreDocumentoInAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(List.of(entity()));

        FirmarLoteResponse resp = useCase.firmar(req);

        assertThat(resp.getEstado()).isEqualTo("OK");
    }

    @Test
    @DisplayName("firmar: incluye el mensaje de WildFly cuando ningun documento se firma y hay respuesta")
    void firmar_ningunDocFirmadoConRespuestaWildFly_incluyeMensajeWildFly() throws Exception {
        FirmarLoteRequest req = requestSinCifrado(List.of(doc("doc1.pdf")));

        when(restTemplate.postForObject(contains("/servicio/documentos"), any(), eq(String.class)))
                .thenReturn("tokenJwt");
        when(restTemplate.postForObject(contains("/appfirmardocumentotransversal"), any(), eq(String.class)))
                .thenReturn("codigo de error retornado por wildfly");
        when(firmadosRepo.findByCedulaAndNombreDocumentoInAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(List.of());

        FirmarLoteResponse resp = useCase.firmar(req);

        assertThat(resp.getEstado()).isEqualTo("ERROR");
        assertThat(resp.getMensaje()).contains("Error en WildFly");
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private FirmarDocumentoItem doc(String nombre) {
        return FirmarDocumentoItem.builder().nombre(nombre).documento("base64content").build();
    }

    private FirmarParametros params() {
        return FirmarParametros.builder().llx("100").lly("700").pagina("1").tipoEstampado("QR").build();
    }

    private FirmarLoteRequest requestSinCifrado(List<FirmarDocumentoItem> docs) {
        return FirmarLoteRequest.builder()
                .idLote("lote_001").cedula("1003422365")
                .pkcs12("pkcs12real").password("passreal")
                .documentos(docs).parametros(params()).build();
    }

    private FirDocfirmadoEntity entity() {
        return FirDocfirmadoEntity.builder()
                .idDoc(UUID.randomUUID()).cedula("1003422365")
                .nombreDocumento("doc.pdf").status("A").build();
    }
}
