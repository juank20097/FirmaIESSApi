/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import com.mongodb.client.MongoClient;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.MongoConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MongoConfig - Pruebas Unitarias")
class MongoConfigTest {

    private MongoConfig config;

    @BeforeEach
    void setUp() {
        config = new MongoConfig();
        ReflectionTestUtils.setField(config, "mongoUri", "mongodb://user:pass@localhost:27099/auditoria_iess_db");
    }

    @Test
    @DisplayName("mongoClient: crea el cliente MongoDB con la URI configurada")
    void mongoClient_creaCliente() {
        MongoClient client = config.mongoClient();

        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("mongoDatabaseFactory: crea el factory apuntando a la base de auditoria")
    void mongoDatabaseFactory_creaFactory() {
        MongoDatabaseFactory factory = config.mongoDatabaseFactory();

        assertThat(factory).isNotNull();
    }

    @Test
    @DisplayName("mongoTemplate: crea el template sin propagar excepcion aunque no haya conexion real")
    void mongoTemplate_creaTemplateSinPropagarExcepcion() {
        MongoTemplate template = config.mongoTemplate();

        assertThat(template).isNotNull();
    }
}
