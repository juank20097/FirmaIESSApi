/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * <b> Configuración de auditoría automática para JPA.
 * Habilita el llenado automático de los campos fecha_creacion, fecha_actualizacion,
 * creado_por y actualizado_por en cada operación INSERT y UPDATE. </b>
 *
 * @author Juan Carlos Estévez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estévez Hidalgo , Date: 07 may 2026]
 *          </p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * <b> Provee el usuario actual para los campos de auditoría creado_por y actualizado_por.
     * Cuando se integre Spring Security, reemplazar el valor estático "system"
     * por SecurityContextHolder.getContext().getAuthentication().getName(). </b>
     *
     * @return AuditorAware con el nombre del usuario actual
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system");
    }
}
