package io.arconia.dev.services.docling;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.docling.DoclingDevServicesAutoConfiguration.DoclingDevServicesRegistrar;
import io.arconia.docling.autoconfigure.DoclingServeConnectionDetails;

/**
 * Auto-configuration for Docling Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("docling")
@EnableConfigurationProperties(DoclingDevServicesProperties.class)
@Import(DoclingDevServicesRegistrar.class)
public final class DoclingDevServicesAutoConfiguration {

    static DoclingServeContainer createContainer(DoclingDevServicesProperties properties) {
        return new DoclingServeContainer(DoclingServeContainerConfig.builder()
                .image(properties.getImageName())
                .enableUi(properties.isUiEnabled())
                .apiKey(properties.getApiKey())
                .build());
    }

    static class DoclingDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(DoclingDevServicesProperties.CONFIG_PREFIX, DoclingDevServicesProperties.class);

            registry.registerDevService(service -> {
                service.name("docling")
                        .description("Docling Dev Service")
                        .properties(properties)
                        .container(DoclingServeContainer.class, () -> createContainer(properties))
                        .port(DoclingServeContainer.DEFAULT_DOCLING_PORT)
                        .discovery(DoclingServeConnectionDetails.class,
                                container -> new DoclingDiscoveredConnectionDetails(container, properties));
                if (properties.isUiEnabled()) {
                    service.link("Docling UI", DoclingServeContainer.DEFAULT_DOCLING_PORT, "/ui");
                }
                service.link("Docling OpenAPI", DoclingServeContainer.DEFAULT_DOCLING_PORT, "/docs")
                        .link("Docling Scalar", DoclingServeContainer.DEFAULT_DOCLING_PORT, "/scalar");
            });
        }

    }

}
