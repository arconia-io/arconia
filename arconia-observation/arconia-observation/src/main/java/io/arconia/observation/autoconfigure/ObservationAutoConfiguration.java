package io.arconia.observation.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import io.arconia.observation.conventions.AiObservationConventionsProvider;

/**
 * Auto-configuration for observations.
 */
@AutoConfiguration
public class ObservationAutoConfiguration {

    @Bean
    SmartInitializingSingleton observationConventionsValidator(
            ObjectProvider<AiObservationConventionsProvider> providers) {
        return () -> {
            List<String> names = providers.orderedStream().map(AiObservationConventionsProvider::name).sorted().toList();
            if (names.size() > 1) {
                throw new MultipleAiObservationConventionsException(names);
            }
        };
    }

}
