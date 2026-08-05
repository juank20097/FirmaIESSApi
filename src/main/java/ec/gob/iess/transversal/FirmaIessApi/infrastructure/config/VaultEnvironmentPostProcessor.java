/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponseSupport;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * <b> EnvironmentPostProcessor que carga los secretos de Vault
 * en la fase mas temprana del arranque de Spring Boot,
 * antes de que cualquier bean sea construido.
 * Solo se activa cuando VAULT_ENABLED=true en el .env.
 *
 * Autenticacion: USERPASS cuando VAULT_USERNAME esta definido —
 * se llama a la API REST de Vault (/v1/auth/userpass/login/{user})
 * para obtener el client_token, que luego se usa con TokenAuthentication.
 * Fallback a TOKEN directo si VAULT_USERNAME no esta configurado.
 *
 * Puerto: si VAULT_HOST contiene un punto (dominio institucional),
 * el puerto NO se concatena en la URI. </b>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 */
@Slf4j
public class VaultEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DB_ENGINE_POSTGRES = "postgres";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String DEFAULT_LOCALHOST = "localhost";
    private static final String PROP_RSA_PUBLIC_KEY = "RSA_PUBLIC_KEY";
    private static final String PROP_RSA_PRIVATE_KEY = "RSA_PRIVATE_KEY";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {

        String vaultEnabled = environment.getProperty("VAULT_ENABLED", "false");
        if (!"true".equalsIgnoreCase(vaultEnabled)) {
            return;
        }

        String vaultHost     = environment.getProperty("VAULT_HOST", "svc-vault");
        String vaultPort     = environment.getProperty("VAULT_PORT", "8200");
        String vaultScheme   = environment.getProperty("VAULT_SCHEME", "http");
        String vaultUsername = environment.getProperty("VAULT_USERNAME", "");
        String vaultPassword = environment.getProperty("VAULT_PASSWORD", "");
        String vaultToken    = environment.getProperty("VAULT_TOKEN", "");
        String vaultAppName  = environment.getProperty("VAULT_APP_NAME", "base-spring-api");
        String dbEngine      = environment.getProperty("DB_ENGINE", DB_ENGINE_POSTGRES);
        String rsaSwitch     = environment.getProperty("RSA_ENABLED", "false");
        String basePath      = "secret/data/iess/apps/transversal/" + vaultAppName;

        log.info("Vault: cargando secretos en fase de bootstrap...");

        try {
            boolean esDominio = vaultHost.contains(".");
            String baseUrl = esDominio
                    ? vaultScheme + "://" + vaultHost
                    : vaultScheme + "://" + vaultHost + ":" + vaultPort;

            VaultEndpoint endpoint = VaultEndpoint.from(new URI(baseUrl));

            // Autenticacion: USERPASS via REST -> obtener client_token
            String tokenEfectivo = vaultToken;
            if (!vaultUsername.isBlank() && !vaultPassword.isBlank()) {
                tokenEfectivo = loginUserPass(baseUrl, vaultUsername, vaultPassword);
                log.info("Vault: autenticado con USERPASS (usuario: {}).", vaultUsername);
            } else {
                log.warn("Vault: VAULT_USERNAME no configurado, usando TOKEN como fallback.");
            }

            VaultTemplate vaultTemplate = new VaultTemplate(endpoint,
                    new TokenAuthentication(tokenEfectivo));

            Map<String, Object> secretos = new HashMap<>();

            // ── PostgreSQL ──────────────────────────────────────────
            if (DB_ENGINE_POSTGRES.equalsIgnoreCase(dbEngine)) {
                leerSecreto(vaultTemplate, basePath + "/db/postgres",
                        Map.of("host","DB_POSTGRES_HOST","port","DB_POSTGRES_PORT",
                               KEY_USERNAME,"DB_POSTGRES_USERNAME",KEY_PASSWORD,"DB_POSTGRES_PASSWORD",
                               "bdd","DB_POSTGRES_NAME"), secretos);

                secretos.put("spring.datasource.url",
                        "jdbc:postgresql://" + val(secretos,"DB_POSTGRES_HOST",DEFAULT_LOCALHOST) + ":" +
                        val(secretos,"DB_POSTGRES_PORT","5432") + "/" +
                        val(secretos,"DB_POSTGRES_NAME","base_spring_db"));
                secretos.put("spring.datasource.username", val(secretos,"DB_POSTGRES_USERNAME",DB_ENGINE_POSTGRES));
                secretos.put("spring.datasource.password", val(secretos,"DB_POSTGRES_PASSWORD",DB_ENGINE_POSTGRES));
            }

            // ── Oracle ──────────────────────────────────────────────
            if ("oracle".equalsIgnoreCase(dbEngine)) {
                leerSecreto(vaultTemplate, basePath + "/db/oracle",
                        Map.of("host","DB_ORACLE_HOST","port","DB_ORACLE_PORT",
                               KEY_USERNAME,"DB_ORACLE_USERNAME",KEY_PASSWORD,"DB_ORACLE_PASSWORD",
                               "service","DB_ORACLE_SERVICE"), secretos);

                secretos.put("spring.datasource.url",
                        "jdbc:oracle:thin:@//" + val(secretos,"DB_ORACLE_HOST",DEFAULT_LOCALHOST) + ":" +
                        val(secretos,"DB_ORACLE_PORT","1521") + "/" +
                        val(secretos,"DB_ORACLE_SERVICE","ORCLPDB1"));
                secretos.put("spring.datasource.username", val(secretos,"DB_ORACLE_USERNAME","oracle_user"));
                secretos.put("spring.datasource.password", val(secretos,"DB_ORACLE_PASSWORD","oracle_password"));
            }

            // ── MongoDB ─────────────────────────────────────────────
            leerSecreto(vaultTemplate, basePath + "/db/mongo",
                    Map.of("host","DB_MONGO_HOST","port","DB_MONGO_PORT","bdd","DB_MONGO_NAME",
                           KEY_USERNAME,"DB_MONGO_USERNAME",KEY_PASSWORD,"DB_MONGO_PASSWORD",
                           "auth_db","DB_MONGO_AUTH_DB"), secretos);

            secretos.put("spring.data.mongodb.uri",
                    "mongodb://" + val(secretos,"DB_MONGO_USERNAME","mongo_user") + ":" +
                    val(secretos,"DB_MONGO_PASSWORD","mongo_password") + "@" +
                    val(secretos,"DB_MONGO_HOST",DEFAULT_LOCALHOST) + ":" +
                    val(secretos,"DB_MONGO_PORT","27017") +
                    "/auditoria_iess_db?authSource=" + val(secretos,"DB_MONGO_AUTH_DB","admin"));

            // ── MinIO ────────────────────────────────────────────────
            leerSecreto(vaultTemplate, basePath + "/minio",
                    Map.of("url","MINIO_URL","accessKey","MINIO_ACCESS_KEY","secretKey","MINIO_SECRET_KEY"),
                    secretos);

            secretos.put("minio.url",        val(secretos,"MINIO_URL","http://localhost:9000"));
            secretos.put("minio.access-key", val(secretos,"MINIO_ACCESS_KEY","minioadmin"));
            secretos.put("minio.secret-key", val(secretos,"MINIO_SECRET_KEY","minioadmin"));

            // ── RSA ──────────────────────────────────────────────────
            leerSecreto(vaultTemplate, basePath + "/rsa",
                    Map.of("publicKey",PROP_RSA_PUBLIC_KEY,"privateKey",PROP_RSA_PRIVATE_KEY), secretos);

            secretos.put("rsa.enabled", rsaSwitch);
            if (secretos.containsKey(PROP_RSA_PUBLIC_KEY))  secretos.put("rsa.public-key",  secretos.get(PROP_RSA_PUBLIC_KEY));
            if (secretos.containsKey(PROP_RSA_PRIVATE_KEY)) secretos.put("rsa.private-key", secretos.get(PROP_RSA_PRIVATE_KEY));

            environment.getPropertySources().addFirst(new MapPropertySource("vault-secrets", secretos));
            log.info("Vault: {} propiedades cargadas exitosamente.", secretos.size());

        } catch (Exception e) {
            log.warn("Vault: error al conectar. Usando valores del .env. Error: {}", e.getMessage());
        }
    }

    /**
     * <b> Llama a la API REST de Vault para autenticarse con usuario/clave
     * y retorna el client_token obtenido. </b>
     */
    @SuppressWarnings("unchecked")
    private String loginUserPass(String baseUrl, String username, String password) {
        try {
            String loginUrl = baseUrl + "/v1/auth/userpass/login/" + username;
            RestTemplate rest = new RestTemplate();
            Map<String, String> body = Map.of(KEY_PASSWORD, password);
            Map<String, Object> response = rest.postForObject(loginUrl, body, Map.class);
            if (response != null && response.containsKey("auth")) {
                Map<String, Object> auth = (Map<String, Object>) response.get("auth");
                return (String) auth.get("client_token");
            }
        } catch (Exception e) {
            log.warn("Vault: fallo al autenticar con USERPASS. Error: {}", e.getMessage());
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private void leerSecreto(VaultTemplate vaultTemplate, String path,
                              Map<String, String> mapaClave, Map<String, Object> secretos) {
        try {
            VaultResponseSupport<Map> response = vaultTemplate.read(path, Map.class);
            if (response == null || response.getData() == null) {
                log.warn("Vault: path '{}' no encontrado.", path);
                return;
            }
            Object dataAnidado = response.getData().get("data");
            final Map<String, Object> valores = (dataAnidado instanceof Map)
                    ? (Map<String, Object>) dataAnidado : response.getData();

            mapaClave.forEach((claveVault, propSistema) -> {
                Object valor = valores.get(claveVault);
                if (valor != null) {
                    secretos.put(propSistema, valor.toString());
                    log.info("Vault: [OK] {}/{} -> {}", path, claveVault, propSistema);
                } else {
                    log.warn("Vault: clave '{}' no encontrada en '{}'.", claveVault, path);
                }
            });
        } catch (Exception e) {
            log.warn("Vault: error al leer '{}'. Error: {}", path, e.getMessage());
        }
    }

    private String val(Map<String, Object> secretos, String clave, String defecto) {
        Object v = secretos.get(clave);
        return (v != null && !v.toString().isBlank()) ? v.toString() : defecto;
    }
}
