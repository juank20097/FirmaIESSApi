/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <b> Repositorio JPA para la entidad CertificadoEntity.
 * Provee acceso a los certificados digitales (.p12) registrados
 * por sistema para uso exclusivo de la aplicacion movil FirmaDoc. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 *          [Author: Juan Carlos Estevez Hidalgo, Date: 14 jul 2026]
 */
@Repository
public interface CertificadoJpaRepository extends JpaRepository<CertificadoEntity, Long> {

    /**
     * <b> Busca el certificado registrado para un sistema especifico. </b>
     *
     * @param sistema nombre del sistema consumidor
     * @return Optional con el certificado si existe, empty si no
     */
    Optional<CertificadoEntity> findBySistema(String sistema);
}
