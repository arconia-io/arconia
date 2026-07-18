package io.arconia.dev.services.core.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.testcontainers.containers.Network;

import io.arconia.dev.services.api.provider.DevServiceProvider;
import io.arconia.dev.services.core.container.DevServicesNetworkFactory;

/**
 * Global auto-configuration for Dev Services.
 */
@AutoConfiguration
@EnableConfigurationProperties(DevServicesProperties.class)
public final class DevServicesAutoConfiguration {

    /**
     * The shared network dev service containers join when they opt in, so they can
     * communicate with each other over an OCI network. A user-defined {@link Network}
     * bean takes precedence and disables the configured network name.
     * <p>
     * It is an infrastructure bean, internal to the framework and not meant to be consumed
     * directly by applications. It is also lazy: it is resolved only when a dev service
     * actually joins the network (via {@code ObjectProvider} in the registry), so a configured
     * named network is not created against the container runtime unless it is needed.
     */
    @Bean
    @Lazy
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnDevServicesEnabled
    @ConditionalOnMissingBean(Network.class)
    Network devServicesNetwork(DevServicesProperties properties) {
        return DevServicesNetworkFactory.resolve(properties.getNetwork().getName());
    }

    /**
     * Backstop validation of mutually exclusive dev services after all singletons are created.
     * The main validation happens earlier, before any dev service container is created,
     * via the validator bean registered by the dev services registry.
     */
    @Bean
    SmartInitializingSingleton devServicesConflictValidator(ObjectProvider<DevServiceProvider> providers) {
        return () -> new DevServicesConflictValidator().validate(providers.orderedStream().toList());
    }

}
