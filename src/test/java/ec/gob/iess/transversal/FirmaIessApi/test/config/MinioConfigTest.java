/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.MinioConfig;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MinioConfig - Pruebas Unitarias")
class MinioConfigTest {

    private MinioConfig config;

    @BeforeEach
    void setUp() {
        config = new MinioConfig();
        ReflectionTestUtils.setField(config, "url", "http://localhost:19999");
        ReflectionTestUtils.setField(config, "publicUrl", "http://192.168.12.42:19999");
        ReflectionTestUtils.setField(config, "accessKey", "minioadmin");
        ReflectionTestUtils.setField(config, "secretKey", "minioadmin");
        ReflectionTestUtils.setField(config, "bucketDefault", "mi-bucket");
    }

    @Test
    @DisplayName("publicMinioClient: construye un cliente apuntando a la URL publica")
    void publicMinioClient_construyeCliente() {
        MinioClient client = config.publicMinioClient();

        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("minioClient: lanza excepcion cuando no puede conectar para verificar el bucket")
    void minioClient_falloConexion_lanzaExcepcion() {
        assertThatThrownBy(() -> config.minioClient())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al verificar o crear el bucket de MinIO");
    }
}
