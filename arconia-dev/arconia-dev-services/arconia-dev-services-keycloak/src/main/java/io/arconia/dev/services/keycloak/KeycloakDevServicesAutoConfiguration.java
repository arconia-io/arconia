package io.arconia.dev.services.keycloak;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for Keycloak Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.keycloak", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(KeycloakDevServicesProperties.class)
public final class KeycloakDevServicesAutoConfiguration {
    @Bean
    static BeanDefinitionRegistryPostProcessor keycloakContainerRegistrar() {
        return new KeycloakContainerRegistrar();
    }

    @Bean
    static BeanFactoryPostProcessor keycloakContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(KeycloakContainer.class);
    }

    @Bean
    @ServiceConnection("keycloak")
    @ConditionalOnBean(KeycloakContainer.class)
    @ConditionalOnMissingBean
    KeycloakContainer keycloakServiceConnection(ObjectProvider<KeycloakContainer> provider) {
        return provider.getIfAvailable();
    }
}

