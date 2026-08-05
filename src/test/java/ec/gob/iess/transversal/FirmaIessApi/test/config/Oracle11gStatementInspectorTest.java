/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.Oracle11gStatementInspector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Oracle11gStatementInspector - Pruebas Unitarias")
class Oracle11gStatementInspectorTest {

    private final Oracle11gStatementInspector inspector = new Oracle11gStatementInspector();

    @Test
    @DisplayName("inspect: retorna null cuando el sql es null")
    void inspect_sqlNull_retornaNull() {
        assertThat(inspector.inspect(null)).isNull();
    }

    @Test
    @DisplayName("inspect: transforma offset+fetch next en sintaxis ROWNUM con paginacion")
    void inspect_offsetFetchNext_transformaARownum() {
        String sql = "select id, nombre from persona offset ? rows fetch next ? rows only";

        String resultado = inspector.inspect(sql);

        assertThat(resultado)
                .startsWith("select * from ( select row_.*, rownum rownum_ from (")
                .contains("select id, nombre from persona")
                .endsWith(") row_ where rownum <= ?) where rownum_ > ?");
    }

    @Test
    @DisplayName("inspect: transforma fetch first sin offset en sintaxis ROWNUM simple")
    void inspect_fetchFirstSinOffset_transformaARownumSimple() {
        String sql = "select id, nombre from persona fetch first ? rows only";

        String resultado = inspector.inspect(sql);

        assertThat(resultado).isEqualTo(
                "select * from ( select id, nombre from persona ) where rownum <= ?");
    }

    @Test
    @DisplayName("inspect: retorna el sql sin cambios cuando no aplica ningun patron")
    void inspect_sinPatron_retornaSqlOriginal() {
        String sql = "select id, nombre from persona where id = ?";

        String resultado = inspector.inspect(sql);

        assertThat(resultado).isEqualTo(sql);
    }
}
