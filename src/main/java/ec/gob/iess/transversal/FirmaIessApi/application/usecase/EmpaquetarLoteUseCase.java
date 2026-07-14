/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.application.usecase;

import ec.gob.iess.transversal.FirmaIessApi.application.port.StoragePort;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.EmpaquetarResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * <b>Caso de uso que empaqueta los PDFs firmados de un lote en ZIPs de maximo 2GB
 * y los sube a MinIO eliminando los PDFs individuales ya empaquetados.</b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmpaquetarLoteUseCase {

    private static final long MAX_ZIP_BYTES = 2L * 1024 * 1024 * 1024;

    private final StoragePort storagePort;

    @Value("${firmadigital.url-expiracion-horas:24}")
    private int urlExpiracionHoras;

    @Value("${firmadigital.zip-nombre:documentos_firmados_iess}")
    private String zipNombre;

    public EmpaquetarResponse empaquetar(String idLote) {
        try {
            String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String prefijo = fecha + "/temp_" + idLote + "/";

            List<String> objetos = storagePort.listarObjetos(prefijo);
            List<String> pdfs = objetos.stream().filter(o -> o.endsWith("_signed.pdf")).toList();

            if (pdfs.isEmpty()) {
                return EmpaquetarResponse.builder().idLote(idLote).estado("ERROR")
                        .totalZips(0).totalDocumentos(0)
                        .mensaje("No se encontraron documentos firmados para el lote: " + idLote).build();
            }

            log.info("EmpaquetarLoteUseCase: empaquetando {} PDFs para lote {}", pdfs.size(), idLote);

            int zipNumero = 1;
            int totalEmpaquetados = 0;
            long tamanoActual = 0;
            List<String> pdfsEnZipActual = new ArrayList<>();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            for (String pdfPath : pdfs) {
                byte[] pdfBytes = storagePort.obtenerBytes(pdfPath);
                if (tamanoActual + pdfBytes.length > MAX_ZIP_BYTES && !pdfsEnZipActual.isEmpty()) {
                    zos.close();
                    subirZip(baos.toByteArray(), prefijo, zipNumero, false);
                    eliminarPdfs(pdfsEnZipActual);
                    totalEmpaquetados += pdfsEnZipActual.size();
                    zipNumero++;
                    pdfsEnZipActual.clear();
                    baos = new ByteArrayOutputStream();
                    zos = new ZipOutputStream(baos);
                    tamanoActual = 0;
                }
                String nombreArchivo = pdfPath.substring(pdfPath.lastIndexOf("/") + 1);
                zos.putNextEntry(new ZipEntry(nombreArchivo));
                zos.write(pdfBytes);
                zos.closeEntry();
                tamanoActual += pdfBytes.length;
                pdfsEnZipActual.add(pdfPath);
            }

            if (!pdfsEnZipActual.isEmpty()) {
                zos.close();
                subirZip(baos.toByteArray(), prefijo, zipNumero,
                        pdfs.size() <= pdfsEnZipActual.size() && zipNumero == 1);
                eliminarPdfs(pdfsEnZipActual);
                totalEmpaquetados += pdfsEnZipActual.size();
            }

            log.info("EmpaquetarLoteUseCase: {} PDFs empaquetados en {} ZIP(s) para lote {}",
                    totalEmpaquetados, zipNumero, idLote);

            return EmpaquetarResponse.builder().idLote(idLote).estado("OK")
                    .totalZips(zipNumero).totalDocumentos(totalEmpaquetados)
                    .mensaje(totalEmpaquetados + " documento(s) empaquetado(s) en " + zipNumero + " ZIP(s).").build();

        } catch (Exception e) {
            log.error("EmpaquetarLoteUseCase: error al empaquetar lote {}: {}", idLote, e.getMessage());
            return EmpaquetarResponse.builder().idLote(idLote).estado("ERROR")
                    .totalZips(0).totalDocumentos(0)
                    .mensaje("Error al empaquetar: " + e.getMessage()).build();
        }
    }

    private void subirZip(byte[] zipBytes, String prefijo, int numero, boolean esUnico) {
        String nombreZip = esUnico ? zipNombre + ".zip" : zipNombre + "_" + numero + ".zip";
        storagePort.almacenarBytes(zipBytes, prefijo + nombreZip, "application/zip");
        log.debug("EmpaquetarLoteUseCase: ZIP subido a MinIO: {}", prefijo + nombreZip);
    }

    private void eliminarPdfs(List<String> paths) {
        for (String path : paths) {
            try {
                storagePort.eliminarObjeto(path);
            } catch (Exception e) {
                log.warn("EmpaquetarLoteUseCase: no se pudo eliminar PDF {}: {}", path, e.getMessage());
            }
        }
    }
}
