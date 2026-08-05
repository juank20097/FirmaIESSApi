/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.application.usecase;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoJpaRepository;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.mapper.FirDocfirmadoMapper;
import ec.gob.iess.transversal.FirmaIessApi.model.FirDocfirmado;
import ec.gob.iess.transversal.FirmaIessApi.application.port.StoragePort;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.LoteContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Caso de uso que orquesta las operaciones de negocio sobre FirDocfirmado.
 * Gestiona la creacion del registro en BD y el almacenamiento temporal del PDF firmado en MinIO.</b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirDocfirmadoUseCase {

    private final FirDocfirmadoJpaRepository repository;
    private final FirDocfirmadoMapper mapper;
    private final StoragePort storagePort;
    private final LoteContextHolder loteContextHolder;

    @Transactional
    public FirDocfirmado crear(FirDocfirmado documento) {
        String idLote = loteContextHolder.obtener(documento.getCedula());
        return crearInterno(documento, idLote);
    }

    @Transactional
    public FirDocfirmado crear(FirDocfirmado documento, String idLote) {
        return crearInterno(documento, idLote);
    }

    private FirDocfirmado crearInterno(FirDocfirmado documento, String idLote) {
        documento.setStatus("A");
        FirDocfirmado guardado = mapper.toDomain(repository.save(mapper.toEntity(documento)));

        if (documento.getArchivo() != null && documento.getArchivo().length > 0) {
            try {
                String fecha = java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String carpeta = idLote != null ? "temp_" + idLote : "temp_" + guardado.getIdDoc();
                String nombreSinExtension = documento.getNombreDocumento()
                        .replaceAll("(?i)\\.pdf$", "");
                String path = fecha + "/" + carpeta + "/" + nombreSinExtension + "_signed.pdf";
                storagePort.almacenarBytes(documento.getArchivo(), path, "application/pdf");
                log.info("FirDocfirmadoUseCase: PDF subido a MinIO en path: {}", path);
            } catch (Exception e) {
                log.warn("FirDocfirmadoUseCase: error al subir PDF a MinIO: {}", e.getMessage());
            }
        }
        return guardado;
    }
}
