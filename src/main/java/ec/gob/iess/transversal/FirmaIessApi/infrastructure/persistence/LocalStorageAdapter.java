/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence;

import ec.gob.iess.transversal.FirmaIessApi.application.port.StoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <b> Adaptador de almacenamiento local en disco.
 * Implementa el puerto StoragePort y se activa cuando MINIO_ENABLED=false (valor por defecto).
 * Los archivos se guardan en el directorio configurado por STORAGE_LOCAL_PATH.
 * Los metodos de URL presignada y listado retornan valores de fallback — este adaptador
 * es solo para desarrollo local sin MinIO. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 2.0
 */
@Component
@ConditionalOnProperty(name = "minio.enabled", havingValue = "false", matchIfMissing = true)
public class LocalStorageAdapter implements StoragePort {

    @Value("${storage.local.path:uploads}")
    private String uploadPath;

    @Override
    public String almacenarBytes(byte[] bytes, String path, String contentType) {
        try {
            Path destino = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(path);
            Files.createDirectories(destino.getParent());
            Files.write(destino, bytes);
            return destino.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error al almacenar bytes localmente: " + path, e);
        }
    }

    @Override
    public String almacenar(MultipartFile archivo, String nombreUnico) {
        try {
            Path directorio = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(nombreUnico);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return destino.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error al almacenar el archivo localmente: " + nombreUnico, e);
        }
    }

    @Override
    public void eliminar(String nombreAlmacenado) {
        try {
            Path archivo = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(nombreAlmacenado);
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar el archivo: " + nombreAlmacenado, e);
        }
    }

    @Override
    public void eliminarObjeto(String path) {
        eliminar(path);
    }

    @Override
    public List<String> listarObjetos(String prefijo) {
        try {
            Path base = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(prefijo);
            if (!Files.exists(base)) return List.of();
            return Files.walk(base)
                    .filter(Files::isRegularFile)
                    .map(p -> Paths.get(uploadPath).toAbsolutePath().normalize().relativize(p).toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error al listar objetos locales con prefijo: " + prefijo, e);
        }
    }

    @Override
    public byte[] obtenerBytes(String path) {
        try {
            Path archivo = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(path);
            return Files.readAllBytes(archivo);
        } catch (IOException e) {
            throw new RuntimeException("Error al obtener bytes del archivo local: " + path, e);
        }
    }

    @Override
    public String generarUrlPresignada(String path, int horasExpiracion) {
        // En almacenamiento local no hay URLs presignadas -- retorna la ruta relativa
        return "/uploads/" + path;
    }

    @Override
    public String getBucket() {
        return null;
    }
}
