package io.arconia.dev.services.lldap;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.ldap.LLdapContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for LLDAP Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.lldap", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(LldapDevServicesProperties.class)
public final class LldapDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "lldap/lldap";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    LLdapContainer lldapContainer(LldapDevServicesProperties properties) {
        return new ArconiaLldapContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor lldapContainerContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(LLdapContainer.class);
    }

}
