/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * <b> Configuración condicional del cliente MinIO.
 * Solo se activa cuando MINIO_ENABLED=true en el .env.
 * Si MINIO_ENABLED=false (default), el bean MinioClient no se crea
 * y el servicio levanta sin necesitar el servidor MinIO disponible.
 * Al iniciar verifica si el bucket configurado existe y lo crea automáticamente
 * en caso de que no exista. </b>
 *
 * @author Juan Carlos Estévez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estévez Hidalgo , Date: 07 may 2026]
 *          </p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
public class MinioConfig {

    /** URL del servidor MinIO. */
    @Value("${minio.url}")
    private String url;

    /** URL publica del servidor MinIO, usada para firmar URLs de descarga. */
    @Value("${minio.public-url}")
    private String publicUrl;

    /** Clave de acceso (access key) de MinIO. */
    @Value("${minio.access-key}")
    private String accessKey;

    /** Clave secreta (secret key) de MinIO. */
    @Value("${minio.secret-key}")
    private String secretKey;

    /** Nombre del bucket por defecto a crear si no existe. */
    @Value("${minio.bucket-default}")
    private String bucketDefault;

    /**
     * <b> Crea y configura el bean MinioClient con las credenciales del .env.
     * Verifica si el bucket por defecto existe y lo crea automáticamente si no existe. </b>
     *
     * @return instancia configurada de MinioClient
     * @throws RuntimeException si ocurre un error al conectar o crear el bucket
     */
    @Bean
    public MinioClient minioClient() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        MinioClient client = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .httpClient(httpClient)
                .build();

        crearBucketSiNoExiste(client);

        return client;
    }

    /**
     * <b> Crea un cliente MinIO configurado con la URL publica del servidor,
     * usado exclusivamente para generar URLs presignadas de descarga. </b>
     *
     * @return instancia de MinioClient apuntando a la URL publica
     */
    @Bean("publicMinioClient")
    public MinioClient publicMinioClient() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        return MinioClient.builder()
                .endpoint(publicUrl)
                .credentials(accessKey, secretKey)
                .httpClient(httpClient)
                .build();
    }

    /**
     * <b> Verifica si el bucket configurado existe en MinIO y lo crea si no existe. </b>
     *
     * @param client instancia del cliente MinIO ya configurado
     */
    private void crearBucketSiNoExiste(MinioClient client) {
        try {
            boolean existe = client.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketDefault)
                            .build()
            );

            if (!existe) {
                client.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketDefault)
                                .build()
                );
                log.info("MinIO: bucket '{}' creado automáticamente.", bucketDefault);
            } else {
                log.info("MinIO: bucket '{}' ya existe.", bucketDefault);
            }
        } catch (Exception e) {
            log.error("MinIO: fallo al verificar/crear el bucket '{}'. URL configurada: '{}'. Causa: {}",
                    bucketDefault, url, e.toString(), e);
            throw new RuntimeException(
                    "Error al verificar o crear el bucket de MinIO: " + bucketDefault
                            + " (url=" + url + "). Causa real: " + e.getMessage(), e
            );
        }
    }
}
