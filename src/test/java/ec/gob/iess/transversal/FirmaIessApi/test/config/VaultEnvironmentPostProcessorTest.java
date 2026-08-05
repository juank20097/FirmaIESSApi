/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.test.config;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.VaultEnvironmentPostProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VaultEnvironmentPostProcessor - Pruebas Unitarias")
class VaultEnvironmentPostProcessorTest {

    @Mock private ConfigurableEnvironment environment;
    @Mock private SpringApplication application;

    private final VaultEnvironmentPostProcessor processor = new VaultEnvironmentPostProcessor();

    @Test
    @DisplayName("postProcessEnvironment: no hace nada cuando VAULT_ENABLED no es true")
    void postProcessEnvironment_vaultDeshabilitado_noHaceNada() {
        when(environment.getProperty("VAULT_ENABLED", "false")).thenReturn("false");

        processor.postProcessEnvironment(environment, application);

        verify(environment, never()).getPropertySources();
    }

    @Test
    @DisplayName("postProcessEnvironment: usa fallback TOKEN cuando no hay VAULT_USERNAME, host sin dominio")
    void postProcessEnvironment_sinUsername_hostSinDominio_noPropagaExcepcion() {
        configurarPropiedadesBase("localhost", "");

        assertThatCode(() -> processor.postProcessEnvironment(environment, application))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("postProcessEnvironment: intenta USERPASS cuando hay VAULT_USERNAME, host con dominio")
    void postProcessEnvironment_conUsername_hostConDominio_noPropagaExcepcion() {
        configurarPropiedadesBase("127.0.0.1", "admin");
        when(environment.getProperty("VAULT_PASSWORD", "")).thenReturn("clave-secreta");

        assertThatCode(() -> processor.postProcessEnvironment(environment, application))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("postProcessEnvironment: procesa la rama Oracle sin propagar excepcion")
    void postProcessEnvironment_dbEngineOracle_noPropagaExcepcion() {
        configurarPropiedadesBase("localhost", "");
        when(environment.getProperty("DB_ENGINE", "postgres")).thenReturn("oracle");

        assertThatCode(() -> processor.postProcessEnvironment(environment, application))
                .doesNotThrowAnyException();
    }

    private void configurarPropiedadesBase(String vaultHost, String vaultUsername) {
        when(environment.getProperty("VAULT_ENABLED", "false")).thenReturn("true");
        when(environment.getProperty("VAULT_HOST", "svc-vault")).thenReturn(vaultHost);
        when(environment.getProperty("VAULT_PORT", "8200")).thenReturn("1");
        when(environment.getProperty("VAULT_SCHEME", "http")).thenReturn("http");
        when(environment.getProperty("VAULT_USERNAME", "")).thenReturn(vaultUsername);
        if (vaultUsername.isBlank()) {
            when(environment.getProperty("VAULT_PASSWORD", "")).thenReturn("");
        }
        when(environment.getProperty("VAULT_TOKEN", "")).thenReturn("token-dummy-para-pruebas");
        when(environment.getProperty("VAULT_APP_NAME", "base-spring-api")).thenReturn("firmaec");
        when(environment.getProperty("DB_ENGINE", "postgres")).thenReturn("postgres");
        when(environment.getProperty("RSA_ENABLED", "false")).thenReturn("false");
        lenient().when(environment.getPropertySources()).thenReturn(new MutablePropertySources());
    }
}
