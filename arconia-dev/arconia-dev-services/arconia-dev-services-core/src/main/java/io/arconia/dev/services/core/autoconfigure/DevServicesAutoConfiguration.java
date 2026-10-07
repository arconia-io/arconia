package io.arconia.dev.services.core.autoconfigure;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.testcontainers.containers.Network;

/**
 * Global auto-configuration for Dev Services.
 */
@AutoConfiguration
@ConditionalOnDevServicesEnabled
@EnableConfigurationProperties(DevServicesProperties.class)
public final class DevServicesAutoConfiguration {

    /**
     * The network dev service containers join when the shared network is enabled, so they can
     * reach each other. It is the per-application network Testcontainers provides, created on
     * first use and removed when the JVM exits. A user-defined {@link Network} bean takes
     * precedence, which is the way to supply a network with a stable id.
     * <p>
     * The bean is lazy, so the network is only created when a dev service joins it.
     */
    @Bean
    @Lazy
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(Network.class)
    Network devServicesNetwork() {
        return Network.SHARED;
    }

}
