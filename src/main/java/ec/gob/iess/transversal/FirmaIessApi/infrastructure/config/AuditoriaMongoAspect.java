/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.mongo.AuditoriaDocument;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.mongo.AuditoriaMongoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b> Aspecto de auditoria MongoDB para Spring Boot.
 * Intercepta los metodos de los casos de uso que ejecutan operaciones de escritura
 * y escribe el documento de auditoria en MongoDB despues del commit exitoso
 * de la transaccion de negocio. Solo se activa cuando MONGO_ENABLED=true en el .env.
 * Si MongoDB no esta disponible, la auditoria falla silenciosamente con log de advertencia.
 * La transaccion de negocio NO se revierte. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 2.0
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "MONGO_ENABLED", havingValue = "true")
public class AuditoriaMongoAspect {

    @Autowired
    private AuditoriaMongoService auditoriaMongoService;

    @Value("${DB_MONGO_USERNAME:system}")
    private String mongoDbUser;

    @Value("${spring.application.name:FirmaIessApi}")
    private String applicationName;

    // ─────────────────────────────────────────────────────────
    // INSERT
    // ─────────────────────────────────────────────────────────

    @AfterReturning(
            pointcut = "execution(* ec.gob.iess.transversal.FirmaIessApi.application.usecase.*.crear(..))"
                     + " || execution(* ec.gob.iess.transversal.FirmaIessApi.application.usecase.*.subir(..))",
            returning = "resultado"
    )
    public void auditarCrear(JoinPoint jp, Object resultado) {
        registrarPostCommit(jp, resultado, "I", null, null);
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────

    @Around("execution(* ec.gob.iess.transversal.FirmaIessApi.application.usecase.*.actualizar(..))")
    public Object auditarActualizar(ProceedingJoinPoint pjp) throws Throwable {
        Object resultado = pjp.proceed();
        registrarPostCommit(pjp, resultado, "U", null, null);
        return resultado;
    }

    // ─────────────────────────────────────────────────────────
    // DELETE LOGICO
    // ─────────────────────────────────────────────────────────

    @AfterReturning(
            pointcut = "execution(* ec.gob.iess.transversal.FirmaIessApi.application.usecase.*.eliminar(..))"
                     + " || execution(* ec.gob.iess.transversal.FirmaIessApi.application.usecase.*.cambiarStatus(..))"
    )
    public void auditarEliminar(JoinPoint jp) {
        registrarPostCommit(jp, null, "D", null, null);
    }

    // ─────────────────────────────────────────────────────────
    // Logica interna
    // ─────────────────────────────────────────────────────────

    private void registrarPostCommit(JoinPoint jp, Object newValues,
                                     String operacion, Map<String, Object> oldValues,
                                     List<String> changedFields) {
        String entidad   = jp.getTarget().getClass().getSimpleName().replace("UseCase", "").toUpperCase();
        String coleccion = "AUD_GEN." + entidad;

        Map<String, Object> newValuesMap = newValues != null ? convertirAMapa(newValues) : null;

        List<String> camposFinal = changedFields;
        if (camposFinal == null && "U".equals(operacion) && oldValues != null && newValuesMap != null) {
            camposFinal = calcularCamposCambiados(oldValues, newValuesMap);
        }
        if (camposFinal == null) {
            camposFinal = obtenerCamposArgs(jp);
        }

        final List<String>      camposF  = camposFinal;
        final Map<String,Object> oldF    = oldValues;
        final Map<String,Object> newF    = newValuesMap;

        AuditoriaDocument doc = AuditoriaDocument.builder()
                .audOperation(operacion)
                .audTimestamp(LocalDateTime.now())
                .audDbUser(mongoDbUser)
                .audAppUser(obtenerUsuarioActual())
                .audIpAddress(obtenerIpAddress())
                .audSessionId(obtenerSessionId())
                .audProgram(applicationName + "." + jp.getSignature().getName())
                .audNewValues(newF)
                .audOldValues(oldF)
                .audChangedFields(camposF)
                .build();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    escribirAuditoria(doc, coleccion);
                }
            });
        } else {
            escribirAuditoria(doc, coleccion);
        }
    }

    private List<String> calcularCamposCambiados(Map<String, Object> oldValues,
                                                  Map<String, Object> newValues) {
        return oldValues.entrySet().stream()
                .filter(e -> {
                    Object nv = newValues.get(e.getKey());
                    Object ov = e.getValue();
                    if (ov == null && nv == null) return false;
                    if (ov == null || nv == null) return true;
                    return !ov.toString().equals(nv.toString());
                })
                .map(Map.Entry::getKey)
                .toList();
    }

    private void escribirAuditoria(AuditoriaDocument doc, String coleccion) {
        auditoriaMongoService.insertarAuditoria(doc, coleccion);
    }

    private String obtenerUsuarioActual() {
        return "system";
    }

    private List<String> obtenerCamposArgs(JoinPoint jp) {
        return Arrays.stream(jp.getArgs())
                .filter(arg -> arg != null)
                .map(arg -> arg.getClass().getSimpleName())
                .toList();
    }

    private String obtenerIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String obtenerSessionId() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            var session = request.getSession(false);
            return session != null ? session.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertirAMapa(Object objeto) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.convertValue(objeto, Map.class);
        } catch (Exception e) {
            return Map.of("value", objeto.toString());
        }
    }
}
