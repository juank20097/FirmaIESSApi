/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import org.hibernate.cfg.JdbcSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * <b> Configuracion de compatibilidad con Oracle 11g.
 * Registra el Oracle11gStatementInspector directamente en las propiedades
 * de Hibernate para interceptar queries con FETCH FIRST n ROWS ONLY
 * y reemplazarlas por ROWNUM compatible con Oracle 11g.
 * Solo se activa cuando DB_ENGINE=oracle. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estevez Hidalgo , Date: 04 jun 2026]
 *          </p>
 */
@Configuration
@Profile("oracle")
public class Oracle11gConfig {

    /**
     * <b> Registra el StatementInspector de Oracle 11g directamente
     * en las propiedades de Hibernate via HibernatePropertiesCustomizer.
     * Este mecanismo garantiza que el inspector se aplique antes de
     * cualquier ejecucion de SQL. </b>
     *
     * @return HibernatePropertiesCustomizer con el inspector registrado
     */
    @Bean
    public HibernatePropertiesCustomizer oracle11gInspectorCustomizer() {
        return hibernateProperties -> hibernateProperties.put(
                JdbcSettings.STATEMENT_INSPECTOR,
                new Oracle11gStatementInspector()
        );
    }
}
