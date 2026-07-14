/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * <b> Inicializador de indices TTL para colecciones espejo de auditoria.
 * Se ejecuta al arrancar la aplicacion cuando MONGO_ENABLED=true.
 * Detecta automaticamente las entidades JPA registradas y genera
 * las colecciones AUD_GEN.{ENTIDAD} sin valores quemados.
 * Anade el indice TTL de retencion de 7 anyos sobre el campo aud_timestamp
 * segun el estandar PAS-EST-002 seccion 6.5.8. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 2.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "MONGO_ENABLED", havingValue = "true")
public class MongoIndexInitializer {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /**
     * Inicializa los indices TTL en todas las colecciones de auditoria
     * derivadas automaticamente de las entidades JPA del proyecto.
     * Nomenclatura: AUD_GEN.{NOMBRE_ENTIDAD_EN_MAYUSCULAS}
     * 7 anyos = 2555 dias segun seccion 6.5.8 del estandar PAS-EST-002.
     */
    @PostConstruct
    public void inicializarIndices() {
        Set<EntityType<?>> entidades = entityManagerFactory.getMetamodel().getEntities();

        if (entidades.isEmpty()) {
            log.warn("MongoIndexInitializer: no se encontraron entidades JPA registradas.");
            return;
        }

        for (EntityType<?> entidad : entidades) {
            String nombreEntidad = entidad.getName().toUpperCase();
            String coleccion = "AUD_GEN." + nombreEntidad;

            try {
                if (!mongoTemplate.collectionExists(coleccion)) {
                    mongoTemplate.createCollection(coleccion);
                    log.info("MongoDB: coleccion '{}' creada.", coleccion);
                }

                mongoTemplate.indexOps(coleccion).ensureIndex(
                        new Index()
                                .on("aud_timestamp", Sort.Direction.ASC)
                                .expire(Duration.ofDays(2555))
                                .named("aud_ttl_7anyos")
                );
                log.info("MongoDB: indice TTL creado en coleccion '{}'.", coleccion);

            } catch (Exception e) {
                log.warn("MongoDB: no se pudo inicializar coleccion '{}'. Error: {}", coleccion, e.getMessage());
            }
        }
    }
}
