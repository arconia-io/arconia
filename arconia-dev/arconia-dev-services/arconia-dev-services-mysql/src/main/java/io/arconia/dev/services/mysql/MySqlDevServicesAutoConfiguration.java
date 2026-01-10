package io.arconia.dev.services.mysql;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for MySQL Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.mysql", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MySqlDevServicesProperties.class)
public final class MySqlDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "mysql";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    MySQLContainer mysqlContainer(MySqlDevServicesProperties properties) {
        return new ArconiaMySqlContainer(DockerImageName.parse(properties.getImageName())
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
    static BeanFactoryPostProcessor mysqlContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(MySQLContainer.class);
    }

}
