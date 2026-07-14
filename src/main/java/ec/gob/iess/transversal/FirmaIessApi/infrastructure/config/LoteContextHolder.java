/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Componente que mantiene en memoria el mapeo cedula --> idLote.</b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 */
@Slf4j
@Component
public class LoteContextHolder {

    private final ConcurrentHashMap<String, String> contexto = new ConcurrentHashMap<>();

    public void registrar(String cedula, String idLote) {
        contexto.put(cedula, idLote);
        log.debug("LoteContextHolder: registrado cedula={} --> idLote={}", cedula, idLote);
    }

    public String obtener(String cedula) {
        return contexto.get(cedula);
    }

    public void limpiar(String cedula) {
        contexto.remove(cedula);
        log.debug("LoteContextHolder: limpiado registro para cedula={}", cedula);
    }
}
