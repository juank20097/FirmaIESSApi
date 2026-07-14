/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.application.port;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <b> Puerto de salida que define el contrato de almacenamiento de archivos.
 * Tiene dos implementaciones: LocalStorageAdapter (disco local) y MinioStorageAdapter (MinIO),
 * activadas condicionalmente segun la variable MINIO_ENABLED del entorno. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 2.0
 */
public interface StoragePort {

    /**
     * Almacena un arreglo de bytes directamente en el almacenamiento.
     *
     * @param bytes       contenido del archivo en bytes
     * @param path        ruta completa del objeto en el bucket
     * @param contentType tipo MIME del archivo (ej. application/pdf)
     * @return path del archivo almacenado
     */
    String almacenarBytes(byte[] bytes, String path, String contentType);

    /**
     * Almacena el archivo fisicamente y retorna la ruta donde quedo guardado.
     *
     * @param archivo     archivo recibido desde el controller
     * @param nombreUnico nombre unico generado (UUID + extension)
     * @return ruta o path del archivo almacenado
     */
    String almacenar(MultipartFile archivo, String nombreUnico);

    /**
     * Elimina fisicamente el archivo del almacenamiento.
     *
     * @param nombreAlmacenado nombre unico del archivo a eliminar
     */
    void eliminar(String nombreAlmacenado);

    /**
     * Elimina un objeto individual del almacenamiento.
     *
     * @param path ruta completa del objeto a eliminar
     */
    void eliminarObjeto(String path);

    /**
     * Lista todos los objetos bajo un prefijo/carpeta en el almacenamiento.
     *
     * @param prefijo prefijo o carpeta a listar (ej. 2026-05-14/temp_idLote/)
     * @return lista de paths de objetos encontrados
     */
    List<String> listarObjetos(String prefijo);

    /**
     * Obtiene el contenido de un objeto como byte[].
     *
     * @param path ruta completa del objeto
     * @return contenido del objeto en bytes
     */
    byte[] obtenerBytes(String path);

    /**
     * Genera una URL presignada con expiracion para descarga directa.
     *
     * @param path            ruta completa del objeto
     * @param horasExpiracion horas hasta que expira el link
     * @return URL presignada
     */
    String generarUrlPresignada(String path, int horasExpiracion);

    /**
     * Retorna el nombre del bucket activo en MinIO.
     * Retorna null si el almacenamiento configurado es local.
     *
     * @return nombre del bucket o null
     */
    String getBucket();
}
