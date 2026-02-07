package io.arconia.dev.services.docling;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.docling.DoclingDevServicesAutoConfiguration.DoclingDevServicesRegistrar;

/**
 * Auto-configuration for Docling Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("docling")
@EnableConfigurationProperties(DoclingDevServicesProperties.class)
@Import(DoclingDevServicesRegistrar.class)
public final class DoclingDevServicesAutoConfiguration {

    static class DoclingDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(DoclingDevServicesProperties.CONFIG_PREFIX, DoclingDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("docling")
                    .description("Docling Dev Service")
                    .container(container -> container
                            .type(ArconiaDoclingServeContainer.class)
                            .supplier(() -> new ArconiaDoclingServeContainer(properties))
                    ));
        }

    }

}
