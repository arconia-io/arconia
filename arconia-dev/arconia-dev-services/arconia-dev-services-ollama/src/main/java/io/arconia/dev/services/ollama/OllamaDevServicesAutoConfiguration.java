package io.arconia.dev.services.ollama;

import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionDetails;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

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

        private static final String OLLAMA_CONNECTION_DETAILS_CLASS =
                "org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionDetails";

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(OllamaDevServicesProperties.CONFIG_PREFIX, OllamaDevServicesProperties.class);

            // Only enable @ServiceConnection when the Spring AI Ollama module is on the classpath,
            // since it provides the ContainerConnectionDetailsFactory needed to handle the annotation.
            boolean ollamaModulePresent = ClassUtils.isPresent(OLLAMA_CONNECTION_DETAILS_CLASS, null);

            registry.registerDevService(service -> {
                service
                        .name("ollama")
                        .description("Ollama Dev Service")
                        .container(container -> {
                            container
                                .type(ArconiaOllamaContainer.class)
                                .supplier(() -> new ArconiaOllamaContainer(properties));
                            if (!ollamaModulePresent) {
                                container.serviceConnectionName(null);
                            }
                        });
                if (ollamaModulePresent) {
                    configureSharing(service, properties);
                }
            });
        }

        /**
         * Sharing builds typed {@code OllamaConnectionDetails} for a discovered container,
         * so it's only available when the Spring AI Ollama module is on the classpath.
         * Kept in a separate method so the Spring AI Ollama types are only loaded when present.
         */
        private static void configureSharing(DevServicesRegistry.ServiceSpec service, OllamaDevServicesProperties properties) {
            service.discovery(discovery -> discovery
                    .shared(properties.isShared())
                    .connectionDetails(OllamaConnectionDetails.class, OllamaDiscoveredConnectionDetails::new));
        }

    }

}
