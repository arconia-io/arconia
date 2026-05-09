package io.arconia.dev.services.core.autoconfigure;

import java.util.List;
import java.util.stream.Collectors;

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

    @Bean
    SmartInitializingSingleton devServicesConflictValidator(ObjectProvider<DevServiceProvider> providers) {
        return () -> providers.orderedStream()
                .collect(Collectors.groupingBy(DevServiceProvider::category))
                .forEach((category, group) -> {
                    if (group.size() > 1) {
                        List<String> names = group.stream().map(DevServiceProvider::name).sorted().toList();
                        throw new MultipleDevServicesException(category, names);
                    }
                });
    }

}
