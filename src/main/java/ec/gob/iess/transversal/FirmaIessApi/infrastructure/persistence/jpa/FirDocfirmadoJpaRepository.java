/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * <b> Repositorio JPA para la entidad FirDocfirmadoEntity.
 * Provee operaciones de acceso a la base de datos para la gestion
 * de documentos firmados digitalmente. Incluye consultas especificas
 * para el mecanismo de polling del proceso de firma. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 *          [Author: Juan Carlos Estevez Hidalgo, Date: 14 jul 2026]
 */
@Repository
public interface FirDocfirmadoJpaRepository extends JpaRepository<FirDocfirmadoEntity, UUID> {

    /**
     * <b> Busca documentos firmados por cedula y lista de nombres. </b>
     *
     * @param cedula  cedula del firmante
     * @param nombres lista de nombres de documentos a filtrar
     * @return lista de documentos firmados encontrados
     */
    List<FirDocfirmadoEntity> findByCedulaAndNombreDocumentoIn(
            String cedula, List<String> nombres);

    /**
     * <b> Busca documentos firmados por cedula, nombres y fecha de creacion
     * posterior a la fecha dada. Usado por el polling para filtrar solo los
     * documentos del proceso actual y evitar colisiones con firmas anteriores. </b>
     *
     * @param cedula      cedula del firmante
     * @param nombres     lista de nombres de documentos a filtrar
     * @param fechaInicio fecha minima de creacion del registro
     * @return lista de documentos firmados encontrados en el proceso actual
     */
    List<FirDocfirmadoEntity> findByCedulaAndNombreDocumentoInAndCreatedAtAfter(
            String cedula, List<String> nombres, java.time.LocalDateTime fechaInicio);

    /**
     * <b> Busca documentos firmados por lista de IDs y cedula del firmante. </b>
     *
     * @param ids    lista de identificadores UUID de documentos
     * @param cedula cedula del firmante
     * @return lista de documentos firmados encontrados
     */
    List<FirDocfirmadoEntity> findByIdDocInAndCedula(List<UUID> ids, String cedula);
}
