/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;

/**
 * <b> Dialecto personalizado para Oracle 11g compatible con Hibernate 6.
 * Oracle 11g no soporta FETCH FIRST n ROWS ONLY (disponible desde Oracle 12c).
 * Al pasar DatabaseVersion con version 11, Hibernate genera SQL con ROWNUM
 * que es la sintaxis correcta para Oracle 11g. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estevez Hidalgo , Date: 04 jun 2026]
 *          </p>
 */
public class Oracle11gDialect extends OracleDialect {

    /**
     * <b> Constructor que fuerza la version de Oracle a 11.2
     * para que Hibernate genere SQL compatible con Oracle 11g. </b>
     */
    public Oracle11gDialect() {
        super(DatabaseVersion.make(11, 2));
    }
}
