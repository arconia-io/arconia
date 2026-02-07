package io.arconia.dev.services.ollama;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.ollama.OllamaDevServicesAutoConfiguration.OllamaDevServicesRegistrar;

/**
 * Auto-configuration for Ollama Dev Services.
 * <p>
 * If the application is running in dev mode and a native Ollama connection is detected,
 * the auto-configuration will be skipped.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("ollama")
@ConditionalOnOllamaNativeUnavailable
@EnableConfigurationProperties(OllamaDevServicesProperties.class)
@Import(OllamaDevServicesRegistrar.class)
public final class OllamaDevServicesAutoConfiguration {

    static class OllamaDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(OllamaDevServicesProperties.CONFIG_PREFIX, OllamaDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("ollama")
                    .description("Ollama Dev Service")
                    .container(container -> container
                            .type(ArconiaOllamaContainer.class)
                            .supplier(() -> new ArconiaOllamaContainer(properties))
                    ));
        }

    }

}
