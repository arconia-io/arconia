package io.arconia.dev.services.oracle;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for Oracle XE Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.oracle-xe", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(OracleXeDevServicesProperties.class)
public final class OracleXeDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "gvenzl/oracle-xe";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    OracleContainer oracleXeContainer(OracleXeDevServicesProperties properties) {
        return new ArconiaOracleXeContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME), properties)
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean())
                .withUsername(properties.getUsername())
                .withPassword(properties.getPassword())
                .withDatabaseName(properties.getDbName())
                .withInitScripts(properties.getInitScriptPaths());
    }

    @Bean
    static BeanFactoryPostProcessor oracleXeContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(OracleContainer.class);
    }

}
