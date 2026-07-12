package io.arconia.dev.services.elasticsearch;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;

import io.arconia.dev.services.core.registration.DevServicesRegistrar;

import io.arconia.dev.services.core.registration.DevServicesRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("elasticsearch")
@EnableConfigurationProperties(ElasticsearchDevServicesProperties.class)
@Import(ElasticsearchServicesAutoConfiguration.ElasticsearchDevServicesRegistrar.class)
public class ElasticsearchServicesAutoConfiguration {

    static class ElasticsearchDevServicesRegistrar extends DevServicesRegistrar {
        @Override
        protected void registerDevServices(DevServicesRegistry registry, org.springframework.core.env.Environment environment) {
            var properties = bindProperties(ElasticsearchDevServicesProperties.CONFIG_PREFIX, ElasticsearchDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("elasticsearch")
                    .description("Elasticsearch Dev Service")
                    .container(container -> container
                            .type(ArconiaElasticsearchContainer.class)
                            .supplier(() -> new ArconiaElasticsearchContainer(properties))
                    ));
        }
    }
}
