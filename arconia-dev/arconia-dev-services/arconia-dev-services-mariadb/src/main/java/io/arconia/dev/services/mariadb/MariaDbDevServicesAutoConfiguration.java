package io.arconia.dev.services.mariadb;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for MariaDB Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.mariadb", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MariaDbDevServicesProperties.class)
public final class MariaDbDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "mariadb";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    MariaDBContainer mariaDbContainer(MariaDbDevServicesProperties properties) {
        return new ArconiaMariaDbContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean())
                .withUsername(properties.getUsername())
                .withPassword(properties.getPassword())
                .withDatabaseName(properties.getDbName())
                .withInitScripts(properties.getInitScriptPaths());
    }

    @Bean
    static BeanFactoryPostProcessor mariaDbContainerContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(MariaDBContainer.class);
    }

}
