/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * <b> Componente que imprime en el log un resumen del estado de todos los servicios
 * integrados al momento de levantar la aplicación.
 * Se ejecuta una vez que el contexto de Spring está completamente inicializado. </b>
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
public class ApplicationStartupLogger {

    // --- Servidor --------------------------------------------
    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    // --- Motor de base de datos ------------------------------
    @Value("${DB_ENGINE:postgres}")
    private String dbEngine;

    @Value("${DB_POSTGRES_HOST:localhost}")
    private String postgresHost;

    @Value("${DB_POSTGRES_PORT:5432}")
    private String postgresPort;

    @Value("${DB_POSTGRES_NAME:base_spring_db}")
    private String postgresName;

    @Value("${DB_ORACLE_HOST:localhost}")
    private String oracleHost;

    @Value("${DB_ORACLE_PORT:1521}")
    private String oraclePort;

    @Value("${DB_ORACLE_SERVICE:ORCLPDB1}")
    private String oracleService;

    // --- MongoDB ---------------------------------------------
    @Value("${MONGO_ENABLED:false}")
    private boolean mongoEnabled;

    @Value("${DB_MONGO_HOST:localhost}")
    private String mongoHost;

    @Value("${DB_MONGO_PORT:27017}")
    private String mongoPort;

    @Value("${spring.data.mongodb.database:auditoria_iess_db}")
    private String mongoDb;

    // --- Vault -----------------------------------------------
    @Value("${VAULT_ENABLED:false}")
    private boolean vaultEnabled;

    @Value("${VAULT_HOST:localhost}")
    private String vaultHost;

    @Value("${VAULT_PORT:8200}")
    private String vaultPort;

    @Value("${VAULT_APP_NAME:base-spring-api}")
    private String vaultAppName;

    // --- MinIO -----------------------------------------------
    @Value("${MINIO_ENABLED:false}")
    private boolean minioEnabled;

    @Value("${MINIO_URL:http://localhost:9000}")
    private String minioUrl;

    @Value("${MINIO_BUCKET_DEFAULT:base-spring-bucket}")
    private String minioBucket;

    @Value("${storage.local.path:uploads}")
    private String localStoragePath;

    // --- Swagger ---------------------------------------------
    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerPath;

    /**
     * <b> Imprime el resumen de servicios en el log al completarse el arranque de la aplicación. </b>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logResumenArranque() {

        String sep = "----------------------------------------------------------";

        String lineaDb     = resolverLineaDb();
        String lineaMongo  = resolverLineaServicio("MongoDB      ", mongoEnabled, mongoHost, mongoPort, mongoDb);
        String lineaVault  = resolverLineaServicio("Vault        ", vaultEnabled, vaultHost, vaultPort, vaultAppName + "/");
        String lineaMinio  = resolverLineaMinio();
        String swaggerUrl  = "http://localhost:" + serverPort + contextPath + swaggerPath;

        log.info("\n{}\n" +
                "  API - IESS | Arranque completado\n" +
                "{}\n" +
                "  {}\n" +
                "  {}\n" +
                "  {}\n" +
                "  {}\n" +
                "{}\n" +
                "  Swagger UI    : {}\n" +
                "{}",
                sep, sep,
                lineaDb,
                lineaMongo,
                lineaVault,
                lineaMinio,
                sep,
                swaggerUrl,
                sep
        );
    }

    /**
     * <b> Construye la línea de estado del motor de base de datos relacional activo. </b>
     *
     * @return cadena descriptiva del motor y su estado
     */
    private String resolverLineaDb() {
        if ("oracle".equalsIgnoreCase(dbEngine)) {
            String detalle = oracleHost + ":" + oraclePort + "/" + oracleService;
            boolean esLocal = esLocal(oracleHost);
            return "BD Relacional : ORACLE    | " + (esLocal ? "Habilitado/Interno" : "Habilitado/Externo") + "  | " + detalle;
        } else {
            String detalle = postgresHost + ":" + postgresPort + "/" + postgresName;
            boolean esLocal = esLocal(postgresHost);
            return "BD Relacional : POSTGRES  | " + (esLocal ? "Habilitado/Interno" : "Habilitado/Externo") + "  | " + detalle;
        }
    }

    /**
     * <b> Construye la línea de estado de un servicio opcional genérico. </b>
     *
     * @param etiqueta   nombre del servicio a mostrar
     * @param habilitado flag de habilitación
     * @param host       host configurado
     * @param puerto     puerto configurado
     * @param nombre     nombre de base de datos o recurso (puede ser null)
     * @return cadena descriptiva del estado del servicio
     */
    private String resolverLineaServicio(String etiqueta, boolean habilitado, String host, String puerto, String nombre) {
        if (!habilitado) return etiqueta + ": Deshabilitado";
        String detalle = host + ":" + puerto + (nombre != null ? "/" + nombre : "");
        return etiqueta + ": " + (esLocal(host) ? "Habilitado/Interno" : "Habilitado/Externo") + "  | " + detalle;
    }

    /**
     * <b> Construye la línea de estado de MinIO incluyendo el tipo de storage activo. </b>
     *
     * @return cadena descriptiva del estado de MinIO y el motor de almacenamiento
     */
    private String resolverLineaMinio() {
        if (!minioEnabled) {
            return "MinIO        : Deshabilitado  | Storage: Disco local (" + localStoragePath + "/)";
        }
        String host = extraerHost(minioUrl);
        String estado = esLocal(host) ? "Habilitado/Interno" : "Habilitado/Externo";
        return "MinIO        : " + estado + "  | " + minioUrl + " | Bucket: " + minioBucket;
    }

    /**
     * <b> Determina si un host apunta a la máquina local. </b>
     *
     * @param host host a evaluar
     * @return true si es local, false si es externo
     */
    private boolean esLocal(String host) {
        return host.equals("localhost") || host.equals("127.0.0.1");
    }

    /**
     * <b> Extrae el host de una URL completa. </b>
     *
     * @param url URL completa (ej. http://localhost:9000)
     * @return host extraído
     */
    private String extraerHost(String url) {
        try {
            return url.replaceAll("https?://", "").split(":")[0];
        } catch (Exception e) {
            return "localhost";
        }
    }
}
