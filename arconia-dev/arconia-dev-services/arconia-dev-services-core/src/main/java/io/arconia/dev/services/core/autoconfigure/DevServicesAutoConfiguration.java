package io.arconia.dev.services.core.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.arconia.dev.services.api.provider.DevServiceProvider;

/**
 * Global auto-configuration for Dev Services.
 */
@AutoConfiguration
@EnableConfigurationProperties(DevServicesProperties.class)
public final class DevServicesAutoConfiguration {

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
