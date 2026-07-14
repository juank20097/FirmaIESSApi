/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.mongo;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <b> POJO que representa un documento de auditoría para MongoDB.
 * No tiene anotación @Document porque se inserta dinámicamente
 * en la colección espejo correspondiente (ej. AUD_GEN.PERSONA)
 * mediante MongoTemplate.insert(doc, coleccion). </b>
 *
 * @author Juan Carlos Estévez Hidalgo
 *
 * @version Revision: 1.0
 *          <p>
 *          [Author: Juan Carlos Estévez Hidalgo , Date: 07 may 2026]
 *          </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaDocument {

    /**
     * Tipo de operación según estándar PAS-EST-002 sección 6.3.1:
     * I = INSERT, U = UPDATE, D = DELETE.
     */
    @Field("aud_operation")
    private String audOperation;

    /**
     * Fecha y hora exacta del evento.
     * Usado como base para el índice TTL de retención 7 años (sección 6.5.8).
     */
    @Field("aud_timestamp")
    private LocalDateTime audTimestamp;

    /** Usuario de base de datos que ejecutó la operación. */
    @Field("aud_db_user")
    private String audDbUser;

    /**
     * Usuario de aplicación propagado vía JWT.
     * NULL si la operación fue directa por DBA (alerta de seguridad según PS010).
     */
    @Field("aud_app_user")
    private String audAppUser;

    /** IP desde donde se originó la conexión. Soporta IPv4 e IPv6. */
    @Field("aud_ip_address")
    private String audIpAddress;

    /** Identificador de sesión HTTP para correlacionar eventos de la misma transacción. */
    @Field("aud_session_id")
    private String audSessionId;

    /** Nombre del módulo o servicio que ejecutó la operación. */
    @Field("aud_program")
    private String audProgram;

    /**
     * Snapshot del registro ANTES del cambio.
     * Presente en UPDATE y DELETE. NULL en INSERT.
     */
    @Field("aud_old_values")
    private Map<String, Object> audOldValues;

    /**
     * Snapshot del registro DESPUÉS del cambio.
     * Presente en INSERT y UPDATE. NULL en DELETE.
     */
    @Field("aud_new_values")
    private Map<String, Object> audNewValues;

    /**
     * Lista de campos que cambiaron en un UPDATE.
     * Facilita consultas del tipo '¿quién modificó el campo X?'
     */
    @Field("aud_changed_fields")
    private List<String> audChangedFields;
}
