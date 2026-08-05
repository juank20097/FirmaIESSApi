/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.Oracle11gConfig;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.Oracle11gStatementInspector;
import org.hibernate.cfg.JdbcSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Oracle11gConfig - Pruebas Unitarias")
class Oracle11gConfigTest {

    @Test
    @DisplayName("oracle11gInspectorCustomizer: registra el StatementInspector en las propiedades de Hibernate")
    void oracle11gInspectorCustomizer_registraInspector() {
        Oracle11gConfig config = new Oracle11gConfig();
        HibernatePropertiesCustomizer customizer = config.oracle11gInspectorCustomizer();
        Map<String, Object> propiedades = new HashMap<>();

        customizer.customize(propiedades);

        assertThat(propiedades).containsKey(JdbcSettings.STATEMENT_INSPECTOR);
        assertThat(propiedades.get(JdbcSettings.STATEMENT_INSPECTOR))
                .isInstanceOf(Oracle11gStatementInspector.class);
    }
}
