/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.mongo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <b> Servicio de persistencia MongoDB para registros de auditoría.
 * Utiliza MongoTemplate directamente con colección dinámica para soportar
 * el esquema de colecciones espejo (ej. AUD_GEN.PERSONA, AUD_GEN.DOCUMENTO).
 * Solo se activa cuando MONGO_ENABLED=true en el .env. </b>
 *
 * @author Juan Carlos Estévez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estévez Hidalgo , Date: 07 may 2026]
 *          </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "MONGO_ENABLED", havingValue = "true") // Sirve para habilitar o deshabilitar el servicio según variable de entorno, el servicio hace referencia a MongoTemplate que solo está disponible si MONGO_ENABLED=true
public class AuditoriaMongoService {

    /** Template de MongoDB para operaciones de persistencia. */
    @Autowired
    private MongoTemplate mongoTemplate; 

    /**
     * <b> Inserta un documento de auditoría en la colección espejo correspondiente.
     * Si MongoDB no está disponible, falla silenciosamente con log de advertencia. </b>
     *
     * @param doc       documento de auditoría a persistir
     * @param coleccion nombre de la colección espejo (ej. AUD_GEN.PERSONA)
     */
    public void insertarAuditoria(AuditoriaDocument doc, String coleccion) {
        try {
            mongoTemplate.insert(doc, coleccion);
            log.debug("Auditoría MongoDB: operación {} registrada en {}",
                    doc.getAudOperation(), coleccion);
        } catch (Exception e) {
            log.warn("Auditoría MongoDB: error al escribir en {}. Error: {}",
                    coleccion, e.getMessage());
        }
    }

    /**
     * <b> Busca registros de auditoría por usuario de aplicación. </b>
     *
     * @param coleccion  nombre de la colección espejo
     * @param audAppUser usuario de aplicación a buscar
     * @return lista de documentos de auditoría del usuario
     */
    public List<AuditoriaDocument> buscarPorUsuario(String coleccion, String audAppUser) {
        return mongoTemplate.find(
                Query.query(Criteria.where("aud_app_user").is(audAppUser)),
                AuditoriaDocument.class, coleccion);
    }

    /**
     * <b> Busca registros de auditoría por tipo de operación. </b>
     *
     * @param coleccion     nombre de la colección espejo
     * @param audOperation  tipo de operación: I, U o D
     * @return lista de documentos de auditoría con esa operación
     */
    public List<AuditoriaDocument> buscarPorEntidadYOperacion(String coleccion, String audOperation) {
        return mongoTemplate.find(
                Query.query(Criteria.where("aud_operation").is(audOperation)),
                AuditoriaDocument.class, coleccion);
    }

    /**
     * <b> Busca registros de auditoría en un rango de fechas. </b>
     *
     * @param coleccion nombre de la colección espejo
     * @param desde     fecha y hora de inicio del rango
     * @param hasta     fecha y hora de fin del rango
     * @return lista de documentos de auditoría en el rango
     */
    public List<AuditoriaDocument> buscarPorRangoFechas(String coleccion, LocalDateTime desde, LocalDateTime hasta) {
        return mongoTemplate.find(
                Query.query(Criteria.where("aud_timestamp").gte(desde).lte(hasta)),
                AuditoriaDocument.class, coleccion);
    }
}
