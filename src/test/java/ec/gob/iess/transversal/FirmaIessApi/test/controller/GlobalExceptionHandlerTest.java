/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.controller;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler - Pruebas Unitarias")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleIllegalArgument: retorna 400 con el mensaje de la excepcion")
    void handleIllegalArgument_retorna400() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleIllegalArgument(new IllegalArgumentException("dato invalido"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("mensaje", "dato invalido");
        assertThat(resp.getBody()).containsEntry("status", 400);
    }

    @Test
    @DisplayName("handleValidation: retorna 400 con los campos invalidos concatenados")
    void handleValidation_retorna400ConCampos() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("request", "cedula", "no debe estar vacio");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> resp = handler.handleValidation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("mensaje", "cedula: no debe estar vacio");
    }

    @Test
    @DisplayName("handleGeneral: retorna 500 con mensaje generico")
    void handleGeneral_retorna500() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleGeneral(new RuntimeException("fallo inesperado"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).containsEntry("mensaje", "Error interno: fallo inesperado");
        assertThat(resp.getBody()).containsKey("timestamp");
    }
}
