/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.LoteContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoteContextHolder - Pruebas Unitarias")
class LoteContextHolderTest {

    private LoteContextHolder holder;

    @BeforeEach
    void setUp() {
        holder = new LoteContextHolder();
    }

    @Test
    @DisplayName("registrar y obtener: retorna el idLote registrado para la cedula")
    void registrarYObtener_retornaIdLote() {
        holder.registrar("1003422365", "lote_001");

        assertThat(holder.obtener("1003422365")).isEqualTo("lote_001");
    }

    @Test
    @DisplayName("obtener: retorna null cuando no hay registro para la cedula")
    void obtener_sinRegistro_retornaNull() {
        assertThat(holder.obtener("inexistente")).isNull();
    }

    @Test
    @DisplayName("limpiar: elimina el registro de la cedula")
    void limpiar_eliminaRegistro() {
        holder.registrar("1003422365", "lote_001");

        holder.limpiar("1003422365");

        assertThat(holder.obtener("1003422365")).isNull();
    }

    @Test
    @DisplayName("registrar: sobrescribe el idLote si la cedula ya tenia uno")
    void registrar_sobrescribeRegistroExistente() {
        holder.registrar("1003422365", "lote_001");
        holder.registrar("1003422365", "lote_002");

        assertThat(holder.obtener("1003422365")).isEqualTo("lote_002");
    }
}
