/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence;

import ec.gob.iess.transversal.FirmaIessApi.application.port.StoragePort;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <b> Adaptador de almacenamiento en MinIO (Object Storage).
 * Implementa el puerto StoragePort y se activa cuando MINIO_ENABLED=true. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 2.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
@RequiredArgsConstructor
public class MinioStorageAdapter implements StoragePort {

    private final MinioClient minioClient;

    /**
     * Cliente MinIO configurado con la URL publica del servidor.
     * Se usa exclusivamente para generar URLs presignadas de descarga.
     */
    @Qualifier("publicMinioClient")
    private final MinioClient publicMinioClient;

    @Value("${minio.bucket-default}")
    private String bucket;

    @Override
    public String almacenarBytes(byte[] bytes, String path, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType)
                    .build());
            return path;
        } catch (Exception e) {
            throw new RuntimeException("Error al subir bytes a MinIO: " + path, e);
        }
    }

    @Override
    public String almacenar(MultipartFile archivo, String nombreUnico) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(nombreUnico)
                    .stream(archivo.getInputStream(), archivo.getSize(), -1)
                    .contentType(archivo.getContentType())
                    .build());
            return nombreUnico;
        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo a MinIO: " + nombreUnico, e);
        }
    }

    @Override
    public void eliminar(String nombreAlmacenado) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(nombreAlmacenado)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar archivo de MinIO: " + nombreAlmacenado, e);
        }
    }

    @Override
    public void eliminarObjeto(String path) {
        eliminar(path);
    }

    @Override
    public List<String> listarObjetos(String prefijo) {
        List<String> resultado = new ArrayList<>();
        try {
            Iterable<Result<Item>> objetos = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).prefix(prefijo).recursive(true).build());
            for (Result<Item> resultado1 : objetos) {
                resultado.add(resultado1.get().objectName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al listar objetos en MinIO con prefijo: " + prefijo, e);
        }
        return resultado;
    }

    @Override
    public byte[] obtenerBytes(String path) {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(path).build())) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener bytes de MinIO: " + path, e);
        }
    }

    @Override
    public String generarUrlPresignada(String path, int horasExpiracion) {
        try {
            return publicMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(path)
                            .expiry(horasExpiracion, TimeUnit.HOURS)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar URL presignada para: " + path, e);
        }
    }

    @Override
    public String getBucket() {
        return bucket;
    }
}
