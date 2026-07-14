/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * <b> Repositorio JPA para la entidad FirDocfirmadoDetalleEntity.
 * Provee acceso a los detalles de las firmas digitales (informacion
 * del certificado, firmante, fechas de validez) asociadas a cada
 * documento firmado. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 *          [Author: Juan Carlos Estevez Hidalgo, Date: 14 jul 2026]
 */
@Repository
public interface FirDocfirmadoDetalleJpaRepository extends JpaRepository<FirDocfirmadoDetalleEntity, UUID> {
}
