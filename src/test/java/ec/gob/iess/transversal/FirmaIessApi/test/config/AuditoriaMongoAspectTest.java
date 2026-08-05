/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.AuditoriaMongoAspect;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.mongo.AuditoriaDocument;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.mongo.AuditoriaMongoService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaMongoAspect - Pruebas Unitarias")
class AuditoriaMongoAspectTest {

    @Mock private AuditoriaMongoService auditoriaMongoService;
    @Mock private JoinPoint joinPoint;
    @Mock private ProceedingJoinPoint proceedingJoinPoint;
    @Mock private Signature signature;

    private AuditoriaMongoAspect aspect;

    static class FirLoteUseCase {
    }

    static class DummyResultado {
        public String getNombre() {
            return "documento.pdf";
        }
    }

    @BeforeEach
    void setUp() {
        aspect = new AuditoriaMongoAspect();
        ReflectionTestUtils.setField(aspect, "auditoriaMongoService", auditoriaMongoService);
        ReflectionTestUtils.setField(aspect, "mongoDbUser", "system");
        ReflectionTestUtils.setField(aspect, "applicationName", "FirmaIessApi");
    }

    @Test
    @DisplayName("auditarCrear: registra auditoria de insercion con la coleccion derivada del target")
    void auditarCrear_registraInsercion() {
        when(joinPoint.getTarget()).thenReturn(new FirLoteUseCase());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("crear");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        aspect.auditarCrear(joinPoint, new DummyResultado());

        ArgumentMatcher<String> coleccionMatcher = "AUD_GEN.FIRLOTE"::equals;
        verify(auditoriaMongoService).insertarAuditoria(any(AuditoriaDocument.class), argThat(coleccionMatcher));
    }

    @Test
    @DisplayName("auditarActualizar: procede la invocacion y registra auditoria de actualizacion")
    void auditarActualizar_procedeYRegistraActualizacion() throws Throwable {
        when(proceedingJoinPoint.proceed()).thenReturn(new DummyResultado());
        when(proceedingJoinPoint.getTarget()).thenReturn(new FirLoteUseCase());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("actualizar");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});

        Object resultado = aspect.auditarActualizar(proceedingJoinPoint);

        assertThat(resultado).isInstanceOf(DummyResultado.class);
        verify(proceedingJoinPoint).proceed();
        verify(auditoriaMongoService).insertarAuditoria(any(AuditoriaDocument.class), anyString());
    }

    @Test
    @DisplayName("auditarEliminar: registra auditoria de eliminacion sin valores nuevos")
    void auditarEliminar_registraEliminacion() {
        when(joinPoint.getTarget()).thenReturn(new FirLoteUseCase());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("eliminar");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"1003422365"});

        aspect.auditarEliminar(joinPoint);

        verify(auditoriaMongoService).insertarAuditoria(any(AuditoriaDocument.class), anyString());
    }
}
