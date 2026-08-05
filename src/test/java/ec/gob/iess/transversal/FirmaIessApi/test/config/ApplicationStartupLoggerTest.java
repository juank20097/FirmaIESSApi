/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.ApplicationStartupLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("ApplicationStartupLogger - Pruebas Unitarias")
class ApplicationStartupLoggerTest {

    private ApplicationStartupLogger logger;

    @BeforeEach
    void setUp() {
        logger = new ApplicationStartupLogger();
        ReflectionTestUtils.setField(logger, "serverPort", "8080");
        ReflectionTestUtils.setField(logger, "contextPath", "/api");
        ReflectionTestUtils.setField(logger, "dbEngine", "postgres");
        ReflectionTestUtils.setField(logger, "postgresHost", "localhost");
        ReflectionTestUtils.setField(logger, "postgresPort", "5432");
        ReflectionTestUtils.setField(logger, "postgresName", "base_spring_db");
        ReflectionTestUtils.setField(logger, "oracleHost", "localhost");
        ReflectionTestUtils.setField(logger, "oraclePort", "1521");
        ReflectionTestUtils.setField(logger, "oracleService", "ORCLPDB1");
        ReflectionTestUtils.setField(logger, "mongoEnabled", false);
        ReflectionTestUtils.setField(logger, "mongoHost", "localhost");
        ReflectionTestUtils.setField(logger, "mongoPort", "27017");
        ReflectionTestUtils.setField(logger, "mongoDb", "auditoria_iess_db");
        ReflectionTestUtils.setField(logger, "vaultEnabled", false);
        ReflectionTestUtils.setField(logger, "vaultHost", "localhost");
        ReflectionTestUtils.setField(logger, "vaultPort", "8200");
        ReflectionTestUtils.setField(logger, "vaultAppName", "base-spring-api");
        ReflectionTestUtils.setField(logger, "minioEnabled", false);
        ReflectionTestUtils.setField(logger, "minioUrl", "http://localhost:9000");
        ReflectionTestUtils.setField(logger, "minioBucket", "base-spring-bucket");
        ReflectionTestUtils.setField(logger, "localStoragePath", "uploads");
        ReflectionTestUtils.setField(logger, "swaggerPath", "/swagger-ui.html");
    }

    @Test
    @DisplayName("logResumenArranque: no lanza excepcion con postgres local y servicios deshabilitados")
    void logResumenArranque_postgresLocalServiciosDeshabilitados_noLanzaExcepcion() {
        assertThatCode(() -> logger.logResumenArranque()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logResumenArranque: maneja motor Oracle externo")
    void logResumenArranque_oracleExterno_noLanzaExcepcion() {
        ReflectionTestUtils.setField(logger, "dbEngine", "oracle");
        ReflectionTestUtils.setField(logger, "oracleHost", "192.168.111.50");

        assertThatCode(() -> logger.logResumenArranque()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logResumenArranque: maneja motor Oracle local")
    void logResumenArranque_oracleLocal_noLanzaExcepcion() {
        ReflectionTestUtils.setField(logger, "dbEngine", "oracle");
        ReflectionTestUtils.setField(logger, "oracleHost", "localhost");

        assertThatCode(() -> logger.logResumenArranque()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logResumenArranque: maneja MongoDB, Vault y MinIO habilitados y externos")
    void logResumenArranque_serviciosHabilitadosExternos_noLanzaExcepcion() {
        ReflectionTestUtils.setField(logger, "mongoEnabled", true);
        ReflectionTestUtils.setField(logger, "mongoHost", "192.168.111.51");
        ReflectionTestUtils.setField(logger, "vaultEnabled", true);
        ReflectionTestUtils.setField(logger, "vaultHost", "192.168.111.52");
        ReflectionTestUtils.setField(logger, "minioEnabled", true);
        ReflectionTestUtils.setField(logger, "minioUrl", "http://192.168.12.42:9000");

        assertThatCode(() -> logger.logResumenArranque()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logResumenArranque: usa localhost por defecto cuando la URL de MinIO no se puede parsear")
    void logResumenArranque_minioUrlInvalida_usaLocalhostPorDefecto() {
        ReflectionTestUtils.setField(logger, "minioEnabled", true);
        ReflectionTestUtils.setField(logger, "minioUrl", null);

        assertThatCode(() -> logger.logResumenArranque()).doesNotThrowAnyException();
    }
}
