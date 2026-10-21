package io.arconia.dev.services.ollama;

import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionDetails;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceSpec;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.ollama.OllamaDevServicesAutoConfiguration.OllamaDevServicesRegistrar;

/**
 * Auto-configuration for Ollama Dev Services.
 * <p>
 * If the application is running in dev mode and a native Ollama connection is detected,
 * the auto-configuration will be skipped.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("ollama")
@ConditionalOnOllamaNativeUnavailable
@EnableConfigurationProperties(OllamaDevServicesProperties.class)
@Import(OllamaDevServicesRegistrar.class)
public final class OllamaDevServicesAutoConfiguration {

    /**
     * The base URL of an Ollama service running natively on the host, on its default port.
     */
    static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";

    /**
     * The connection details of the Spring AI Ollama module, which provides the service
     * connection for the Ollama container when it is on the classpath.
     */
    static final String OLLAMA_CONNECTION_DETAILS_CLASS = "org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionDetails";

    static class OllamaDevServicesRegistrar extends DevServicesRegistrar {

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
                        .properties(properties)
                        .container(ArconiaOllamaContainer.class, () -> new ArconiaOllamaContainer(properties))
                        .port(ArconiaOllamaContainer.OLLAMA_PORT);
                if (ollamaModulePresent) {
                    configureDiscovery(service);
                } else {
                    service.serviceConnection(false);
                }
            });
        }

        /**
         * Discovery builds typed {@code OllamaConnectionDetails} for a discovered container,
         * so it's only available when the Spring AI Ollama module is on the classpath.
         * Kept in a separate method so the Spring AI Ollama types are only loaded when present.
         */
        private static void configureDiscovery(DevServiceSpec service) {
            service.discovery(OllamaConnectionDetails.class, OllamaDiscoveredConnectionDetails::new);
        }

    }

}
