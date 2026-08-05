/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.persistence;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.LocalStorageAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalStorageAdapter - Pruebas Unitarias")
class LocalStorageAdapterTest {

    private LocalStorageAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        adapter = new LocalStorageAdapter();
        ReflectionTestUtils.setField(adapter, "uploadPath", tempDir.toString());
    }

    @Test
    @DisplayName("almacenarBytes: guarda el archivo y retorna la ruta absoluta")
    void almacenarBytes_guardaArchivo() {
        String ruta = adapter.almacenarBytes("contenido".getBytes(), "2026-08-05/doc.pdf", "application/pdf");

        assertThat(ruta).endsWith("doc.pdf");
        assertThat(adapter.obtenerBytes("2026-08-05/doc.pdf")).isEqualTo("contenido".getBytes());
    }

    @Test
    @DisplayName("almacenar: guarda un MultipartFile y retorna la ruta absoluta")
    void almacenar_guardaMultipartFile() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "test.pdf",
                "application/pdf", "contenido pdf".getBytes());

        String ruta = adapter.almacenar(archivo, "unico.pdf");

        assertThat(ruta).endsWith("unico.pdf");
        assertThat(adapter.obtenerBytes("unico.pdf")).isEqualTo("contenido pdf".getBytes());
    }

    @Test
    @DisplayName("eliminar: borra el archivo almacenado")
    void eliminar_borraArchivo() {
        adapter.almacenarBytes("data".getBytes(), "borrar.pdf", "application/pdf");

        adapter.eliminar("borrar.pdf");

        assertThatThrownBy(() -> adapter.obtenerBytes("borrar.pdf"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("eliminarObjeto: delega en eliminar")
    void eliminarObjeto_delegaEnEliminar() {
        adapter.almacenarBytes("data".getBytes(), "borrar2.pdf", "application/pdf");

        adapter.eliminarObjeto("borrar2.pdf");

        assertThatThrownBy(() -> adapter.obtenerBytes("borrar2.pdf"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("listarObjetos: retorna lista vacia cuando el prefijo no existe")
    void listarObjetos_prefijoInexistente_retornaVacio() {
        assertThat(adapter.listarObjetos("no-existe/")).isEmpty();
    }

    @Test
    @DisplayName("listarObjetos: retorna las rutas relativas de los archivos bajo el prefijo")
    void listarObjetos_retornaArchivosBajoPrefijo() {
        adapter.almacenarBytes("a".getBytes(), "2026-08-05/temp_lote/doc1.pdf", "application/pdf");
        adapter.almacenarBytes("b".getBytes(), "2026-08-05/temp_lote/doc2.pdf", "application/pdf");

        List<String> objetos = adapter.listarObjetos("2026-08-05/temp_lote");

        assertThat(objetos)
                .hasSize(2)
                .allMatch(o -> o.contains("doc1.pdf") || o.contains("doc2.pdf"));
    }

    @Test
    @DisplayName("obtenerBytes: lanza excepcion cuando el archivo no existe")
    void obtenerBytes_archivoInexistente_lanzaExcepcion() {
        assertThatThrownBy(() -> adapter.obtenerBytes("no-existe.pdf"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("generarUrlPresignada: retorna la ruta relativa con prefijo /uploads/")
    void generarUrlPresignada_retornaRutaRelativa() {
        String url = adapter.generarUrlPresignada("2026-08-05/doc.pdf", 24);

        assertThat(url).isEqualTo("/uploads/2026-08-05/doc.pdf");
    }

    @Test
    @DisplayName("getBucket: retorna null porque no aplica en almacenamiento local")
    void getBucket_retornaNull() {
        assertThat(adapter.getBucket()).isNull();
    }

    @Test
    @DisplayName("almacenarBytes: lanza excepcion cuando no puede crear el directorio destino")
    void almacenarBytes_falloCreacionDirectorio_lanzaExcepcion() throws IOException {
        java.nio.file.Files.writeString(tempDir.resolve("bloqueo"), "no es un directorio");
        byte[] contenido = "data".getBytes();

        assertThatThrownBy(() -> adapter.almacenarBytes(contenido, "bloqueo/doc.pdf", "application/pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al almacenar bytes localmente");
    }

    @Test
    @DisplayName("almacenar: lanza excepcion cuando no puede crear el directorio destino")
    void almacenar_falloCreacionDirectorio_lanzaExcepcion() throws IOException {
        Path archivoComoUploadPath = tempDir.resolve("bloqueo-archivo");
        java.nio.file.Files.writeString(archivoComoUploadPath, "no es un directorio");
        LocalStorageAdapter adapterInvalido = new LocalStorageAdapter();
        ReflectionTestUtils.setField(adapterInvalido, "uploadPath", archivoComoUploadPath.toString());
        MockMultipartFile archivo = new MockMultipartFile("archivo", "test.pdf",
                "application/pdf", "contenido".getBytes());

        assertThatThrownBy(() -> adapterInvalido.almacenar(archivo, "unico.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al almacenar el archivo localmente");
    }

    @Test
    @DisplayName("eliminar: lanza excepcion cuando el directorio no esta vacio")
    void eliminar_directorioNoVacio_lanzaExcepcion() {
        adapter.almacenarBytes("a".getBytes(), "carpeta/doc.pdf", "application/pdf");

        assertThatThrownBy(() -> adapter.eliminar("carpeta"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al eliminar el archivo");
    }

    @Test
    @DisplayName("listarObjetos: lanza excepcion cuando falla el recorrido del directorio")
    void listarObjetos_falloRecorrido_lanzaExcepcion() {
        adapter.almacenarBytes("a".getBytes(), "carpeta2/doc.pdf", "application/pdf");

        try (org.mockito.MockedStatic<java.nio.file.Files> filesMock =
                     org.mockito.Mockito.mockStatic(java.nio.file.Files.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            filesMock.when(() -> java.nio.file.Files.walk(org.mockito.ArgumentMatchers.any(Path.class)))
                    .thenThrow(new IOException("fallo simulado de recorrido"));

            assertThatThrownBy(() -> adapter.listarObjetos("carpeta2"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al listar objetos locales");
        }
    }
}
