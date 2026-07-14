/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <b> Manejador global de excepciones para todos los controladores REST.
 * Centraliza el manejo de errores y retorna respuestas JSON estructuradas
 * en lugar de stacktraces al cliente. </b>
 *
 * @author Juan Carlos Estévez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estévez Hidalgo , Date: 07 may 2026]
 *          </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * <b> Maneja excepciones de validación de negocio. </b>
     *
     * @param ex excepción de argumento ilegal lanzada por los casos de uso
     * @return respuesta con código HTTP 400 y mensaje del error
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * <b> Maneja excepciones de validación de campos del request. </b>
     *
     * @param ex excepción lanzada por @Valid en los controllers
     * @return respuesta con código HTTP 400 y detalle de los campos inválidos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, errores);
    }

    /**
     * <b> Maneja cualquier excepción no controlada del sistema. </b>
     *
     * @param ex excepción genérica no capturada por otros handlers
     * @return respuesta con código HTTP 500 y mensaje genérico
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno: " + ex.getMessage());
    }

    /**
     * <b> Construye la estructura JSON estándar de respuesta de error. </b>
     *
     * @param status  código de estado HTTP
     * @param mensaje descripción del error
     * @return ResponseEntity con el mapa de error estructurado
     */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
