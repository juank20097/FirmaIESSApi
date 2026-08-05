/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.controller;

import ec.gob.iess.transversal.FirmaIessApi.application.usecase.EmpaquetarLoteUseCase;
import ec.gob.iess.transversal.FirmaIessApi.application.usecase.EnlacesLoteUseCase;
import ec.gob.iess.transversal.FirmaIessApi.application.usecase.FirDocfirmadoUseCase;
import ec.gob.iess.transversal.FirmaIessApi.application.usecase.FirLoteUseCase;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.FirDocfirmadoController;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.EmpaquetarResponse;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.EnlacesResponse;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarLoteRequest;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarLoteResponse;
import ec.gob.iess.transversal.FirmaIessApi.model.FirDocfirmado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FirDocfirmadoController - Pruebas Unitarias")
class FirDocfirmadoControllerTest {

    @Mock private FirDocfirmadoUseCase useCase;
    @Mock private FirLoteUseCase loteUseCase;
    @Mock private EmpaquetarLoteUseCase empaquetarUseCase;
    @Mock private EnlacesLoteUseCase enlacesUseCase;
    @InjectMocks private FirDocfirmadoController controller;

    @Test
    @DisplayName("insertar: delega en el use case y retorna OK")
    void insertar_delegaYRetornaOk() {
        FirDocfirmado request = new FirDocfirmado();

        String resultado = controller.insertar(request);

        assertThat(resultado).isEqualTo("OK");
        verify(useCase).crear(request);
    }

    @Test
    @DisplayName("firmar: delega en FirLoteUseCase y retorna su respuesta")
    void firmar_delegaEnLoteUseCase() {
        FirmarLoteRequest request = new FirmarLoteRequest();
        FirmarLoteResponse response = FirmarLoteResponse.builder().idLote("lote_001").build();
        when(loteUseCase.firmar(request)).thenReturn(response);

        FirmarLoteResponse resultado = controller.firmar(request);

        assertThat(resultado).isEqualTo(response);
    }

    @Test
    @DisplayName("empaquetar: delega en EmpaquetarLoteUseCase y retorna su respuesta")
    void empaquetar_delegaEnEmpaquetarUseCase() {
        EmpaquetarResponse response = EmpaquetarResponse.builder().idLote("lote_001").estado("OK").build();
        when(empaquetarUseCase.empaquetar("lote_001")).thenReturn(response);

        EmpaquetarResponse resultado = controller.empaquetar("lote_001");

        assertThat(resultado).isEqualTo(response);
    }

    @Test
    @DisplayName("obtenerEnlaces: delega en EnlacesLoteUseCase y retorna su respuesta")
    void obtenerEnlaces_delegaEnEnlacesUseCase() {
        EnlacesResponse response = EnlacesResponse.builder().idLote("lote_001").estado("OK").build();
        when(enlacesUseCase.obtenerEnlaces("lote_001")).thenReturn(response);

        EnlacesResponse resultado = controller.obtenerEnlaces("lote_001");

        assertThat(resultado).isEqualTo(response);
    }
}
