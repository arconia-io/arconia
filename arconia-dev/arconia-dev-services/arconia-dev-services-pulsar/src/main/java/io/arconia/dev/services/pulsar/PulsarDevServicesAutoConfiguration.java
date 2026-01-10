package io.arconia.dev.services.pulsar;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Autoconfiguration for Pulsar Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.pulsar", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(PulsarDevServicesProperties.class)
public final class PulsarDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "apachepulsar/pulsar";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    PulsarContainer pulsarContainer(PulsarDevServicesProperties properties) {
        return new ArconiaPulsarContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor pulsarContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(PulsarContainer.class);
    }


}
