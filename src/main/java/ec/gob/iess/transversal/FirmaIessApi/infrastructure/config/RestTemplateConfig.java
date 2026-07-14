/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * <b> Configuracion del RestTemplate usado por FirLoteUseCase
 * para llamar a firmadigital-servicio (WildFly) via HTTP. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 *          [Author: Juan Carlos Estevez Hidalgo, Date: 14 jul 2026]
 */
@Configuration
public class RestTemplateConfig {

    /**
     * <b> Crea el bean RestTemplate con configuracion por defecto.
     * Usado exclusivamente por FirLoteUseCase para las llamadas
     * a los endpoints de firmadigital-servicio en WildFly. </b>
     *
     * @return instancia de RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
