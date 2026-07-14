/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * <b> StatementInspector que intercepta todas las queries SQL antes de
 * ejecutarlas en Oracle 11g y reemplaza la sintaxis FETCH FIRST n ROWS ONLY
 * (disponible solo desde Oracle 12c) por la sintaxis ROWNUM compatible
 * con Oracle 11g. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estevez Hidalgo , Date: 04 jun 2026]
 *          </p>
 */
public class Oracle11gStatementInspector implements StatementInspector {

    /**
     * <b> Intercepta el SQL generado por Hibernate y lo transforma
     * para que sea compatible con Oracle 11g usando ROWNUM. </b>
     *
     * @param sql query SQL generada por Hibernate
     * @return query SQL compatible con Oracle 11g
     */
    @Override
    public String inspect(String sql) {
        if (sql == null) {
            return null;
        }

        String lower = sql.toLowerCase();

        // ── Caso 1: offset + fetch next ──────────────────────────────────
        // Patron: ... offset ? rows fetch next ? rows only
        // Resultado: SELECT * FROM (SELECT row_.*, ROWNUM rownum_ FROM (...) WHERE ROWNUM <= ?) WHERE rownum_ > ?
        int offsetIdx = lower.lastIndexOf(" offset ? rows fetch next ? rows only");
        if (offsetIdx > 0) {
            String inner = sql.substring(0, offsetIdx).trim();
            return "select * from ( select row_.*, rownum rownum_ from ( "
                    + inner
                    + " ) row_ where rownum <= ?) where rownum_ > ?";
        }

        // ── Caso 2: fetch first ? rows only (sin offset) ─────────────────
        // Patron: ... fetch first ? rows only
        // Resultado: SELECT * FROM (...) WHERE ROWNUM <= ?
        int fetchIdx = lower.lastIndexOf(" fetch first ? rows only");
        if (fetchIdx > 0) {
            String inner = sql.substring(0, fetchIdx).trim();
            return "select * from ( " + inner + " ) where rownum <= ?";
        }

        return sql;
    }
}
