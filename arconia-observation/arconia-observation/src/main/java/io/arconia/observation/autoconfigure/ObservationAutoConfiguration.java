package io.arconia.observation.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.arconia.observation.conventions.ObservationConventionsProvider;

/**
 * Auto-configuration for observations.
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservationProperties.class)
public class ObservationAutoConfiguration {

    @Bean
    SmartInitializingSingleton observationConventionsValidator(
            ObjectProvider<ObservationConventionsProvider> providers,
            ObservationProperties properties
    ) {
        return () -> {
            List<String> names = providers.orderedStream().map(ObservationConventionsProvider::name).sorted().toList();
            if (names.size() > 1 && properties.getConventions().getType() == null) {
                throw new MultipleObservationConventionsException(names);
            }
        };
    }

}
