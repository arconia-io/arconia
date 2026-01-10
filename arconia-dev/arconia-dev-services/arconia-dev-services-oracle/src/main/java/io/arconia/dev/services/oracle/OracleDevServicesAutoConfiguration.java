package io.arconia.dev.services.oracle;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for Oracle Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.oracle", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(OracleDevServicesProperties.class)
public final class OracleDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "gvenzl/oracle-free";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    OracleContainer oracleContainer(OracleDevServicesProperties properties) {
        return new ArconiaOracleContainer(DockerImageName.parse(properties.getImageName())
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
    static BeanFactoryPostProcessor oracleContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(OracleContainer.class);
    }

}
