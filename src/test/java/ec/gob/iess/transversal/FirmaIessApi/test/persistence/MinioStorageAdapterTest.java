/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.persistence;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.MinioStorageAdapter;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinioStorageAdapter - Pruebas Unitarias")
class MinioStorageAdapterTest {

    @Mock private MinioClient minioClient;
    @Mock private MinioClient publicMinioClient;

    private MinioStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MinioStorageAdapter(minioClient, publicMinioClient);
        ReflectionTestUtils.setField(adapter, "bucket", "mi-bucket");
    }

    @Test
    @DisplayName("almacenarBytes: sube el contenido y retorna el path")
    void almacenarBytes_subeYRetornaPath() throws Exception {
        String resultado = adapter.almacenarBytes("data".getBytes(), "2026-08-05/doc.pdf", "application/pdf");

        assertThat(resultado).isEqualTo("2026-08-05/doc.pdf");
        verify(minioClient).putObject(any());
    }

    @Test
    @DisplayName("almacenarBytes: lanza excepcion cuando falla la subida")
    void almacenarBytes_fallaSubida_lanzaExcepcion() throws Exception {
        when(minioClient.putObject(any())).thenThrow(new RuntimeException("fallo minio"));
        byte[] contenido = "data".getBytes();

        assertThatThrownBy(() -> adapter.almacenarBytes(contenido, "doc.pdf", "application/pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al subir bytes a MinIO");
    }

    @Test
    @DisplayName("almacenar: sube un MultipartFile y retorna el nombre unico")
    void almacenar_subeMultipartYRetornaNombre() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "test.pdf",
                "application/pdf", "contenido".getBytes());

        String resultado = adapter.almacenar(archivo, "unico.pdf");

        assertThat(resultado).isEqualTo("unico.pdf");
        verify(minioClient).putObject(any());
    }

    @Test
    @DisplayName("almacenar: lanza excepcion cuando falla la subida del archivo")
    void almacenar_fallaSubida_lanzaExcepcion() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "test.pdf",
                "application/pdf", "contenido".getBytes());
        when(minioClient.putObject(any())).thenThrow(new RuntimeException("fallo minio"));

        assertThatThrownBy(() -> adapter.almacenar(archivo, "unico.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al subir archivo a MinIO");
    }

    @Test
    @DisplayName("eliminar: remueve el objeto del bucket")
    void eliminar_removeObjeto() throws Exception {
        adapter.eliminar("doc.pdf");

        verify(minioClient).removeObject(any());
    }

    @Test
    @DisplayName("eliminar: lanza excepcion cuando falla la eliminacion")
    void eliminar_fallaEliminacion_lanzaExcepcion() throws Exception {
        doThrow(new RuntimeException("fallo")).when(minioClient).removeObject(any());

        assertThatThrownBy(() -> adapter.eliminar("doc.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al eliminar archivo de MinIO");
    }

    @Test
    @DisplayName("eliminarObjeto: delega en eliminar")
    void eliminarObjeto_delegaEnEliminar() throws Exception {
        adapter.eliminarObjeto("doc.pdf");

        verify(minioClient).removeObject(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("listarObjetos: retorna los nombres de los objetos listados")
    void listarObjetos_retornaNombres() throws Exception {
        Item item1 = mock(Item.class);
        when(item1.objectName()).thenReturn("2026-08-05/doc1.pdf");
        Item item2 = mock(Item.class);
        when(item2.objectName()).thenReturn("2026-08-05/doc2.pdf");
        Result<Item> result1 = mock(Result.class);
        when(result1.get()).thenReturn(item1);
        Result<Item> result2 = mock(Result.class);
        when(result2.get()).thenReturn(item2);
        when(minioClient.listObjects(any())).thenReturn(List.of(result1, result2));

        List<String> resultado = adapter.listarObjetos("2026-08-05/");

        assertThat(resultado).containsExactly("2026-08-05/doc1.pdf", "2026-08-05/doc2.pdf");
    }

    @Test
    @DisplayName("listarObjetos: lanza excepcion cuando falla el listado")
    void listarObjetos_fallaListado_lanzaExcepcion() {
        when(minioClient.listObjects(any())).thenThrow(new RuntimeException("fallo"));

        assertThatThrownBy(() -> adapter.listarObjetos("prefijo/"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al listar objetos en MinIO");
    }

    @Test
    @DisplayName("obtenerBytes: retorna los bytes del objeto")
    void obtenerBytes_retornaBytes() throws Exception {
        GetObjectResponse response = new GetObjectResponse(
                Headers.of(), "mi-bucket", null, "doc.pdf",
                new ByteArrayInputStream("contenido".getBytes()));
        when(minioClient.getObject(any())).thenReturn(response);

        byte[] resultado = adapter.obtenerBytes("doc.pdf");

        assertThat(resultado).isEqualTo("contenido".getBytes());
    }

    @Test
    @DisplayName("obtenerBytes: lanza excepcion cuando falla la descarga")
    void obtenerBytes_fallaDescarga_lanzaExcepcion() throws Exception {
        when(minioClient.getObject(any())).thenThrow(new RuntimeException("fallo"));

        assertThatThrownBy(() -> adapter.obtenerBytes("doc.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al obtener bytes de MinIO");
    }

    @Test
    @DisplayName("generarUrlPresignada: usa el cliente publico y retorna la URL firmada")
    void generarUrlPresignada_usaClientePublico() throws Exception {
        when(publicMinioClient.getPresignedObjectUrl(any()))
                .thenReturn("http://192.168.12.42:9000/mi-bucket/doc.pdf?signature=xxx");

        String resultado = adapter.generarUrlPresignada("doc.pdf", 24);

        assertThat(resultado).isEqualTo("http://192.168.12.42:9000/mi-bucket/doc.pdf?signature=xxx");
        verify(publicMinioClient).getPresignedObjectUrl(any());
        verify(minioClient, never()).getPresignedObjectUrl(any());
    }

    @Test
    @DisplayName("generarUrlPresignada: lanza excepcion cuando falla la firma")
    void generarUrlPresignada_fallaFirma_lanzaExcepcion() throws Exception {
        when(publicMinioClient.getPresignedObjectUrl(any())).thenThrow(new RuntimeException("fallo"));

        assertThatThrownBy(() -> adapter.generarUrlPresignada("doc.pdf", 24))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al generar URL presignada");
    }

    @Test
    @DisplayName("getBucket: retorna el nombre del bucket configurado")
    void getBucket_retornaBucket() {
        assertThat(adapter.getBucket()).isEqualTo("mi-bucket");
    }
}
