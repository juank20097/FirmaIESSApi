/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.Oracle11gDialect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Oracle11gDialect - Pruebas Unitarias")
class Oracle11gDialectTest {

    @Test
    @DisplayName("constructor: fuerza la version de Oracle a 11.2")
    void constructor_fuerzaVersion11_2() {
        Oracle11gDialect dialect = new Oracle11gDialect();

        assertThat(dialect.getVersion().getMajor()).isEqualTo(11);
        assertThat(dialect.getVersion().getMinor()).isEqualTo(2);
    }
}
