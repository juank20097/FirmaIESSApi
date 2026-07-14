/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <b> Configuración de Swagger / OpenAPI 3.
 * Define la información general de la documentación de la API
 * cargada desde las variables de entorno del archivo .env. </b>
 *
 * @author Juan Carlos Estévez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estévez Hidalgo , Date: 07 may 2026]
 *          </p>
 */
@Configuration
public class SwaggerConfig {

    /** Título de la API mostrado en Swagger UI. */
    @Value("${springdoc.info.title:base-spring-api}")
    private String title;

    /** Descripción de la API mostrada en Swagger UI. */
    @Value("${springdoc.info.description:API REST base con Spring Boot 3}")
    private String description;

    /** Versión de la API mostrada en Swagger UI. */
    @Value("${springdoc.info.version:1.0.0}")
    private String version;

    /**
     * <b> Crea el bean de configuración OpenAPI con la información de la API. </b>
     *
     * @return instancia configurada de OpenAPI
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(version));
    }
}
