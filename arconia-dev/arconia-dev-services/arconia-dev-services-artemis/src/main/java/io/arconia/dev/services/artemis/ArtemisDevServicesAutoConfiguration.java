package io.arconia.dev.services.artemis;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for ActiveMQ Artemis Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.artemis", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ArtemisDevServicesProperties.class)
public final class ArtemisDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "apache/activemq-artemis";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    ArtemisContainer artemisContainer(ArtemisDevServicesProperties properties) {
        return new ArconiaArtemisContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean())
                .withUser(StringUtils.hasText(properties.getUsername()) ? properties.getUsername() : ArtemisDevServicesProperties.DEFAULT_USERNAME)
                .withPassword(StringUtils.hasText(properties.getPassword()) ? properties.getPassword() : ArtemisDevServicesProperties.DEFAULT_PASSWORD);
    }

    @Bean
    static BeanFactoryPostProcessor artemisContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(ArtemisContainer.class);
    }

}
