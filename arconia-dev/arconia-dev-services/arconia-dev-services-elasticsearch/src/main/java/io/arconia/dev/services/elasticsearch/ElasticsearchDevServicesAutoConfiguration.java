package io.arconia.dev.services.elasticsearch;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.elasticsearch.ElasticsearchDevServicesAutoConfiguration.ElasticsearchDevServicesRegistrar;

/**
 * Auto-configuration for Elasticsearch Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("elasticsearch")
@EnableConfigurationProperties(ElasticsearchDevServicesProperties.class)
@Import(ElasticsearchDevServicesRegistrar.class)
public final class ElasticsearchDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "docker.elastic.co/elasticsearch/elasticsearch";

    static final int ELASTICSEARCH_DEFAULT_PORT = 9200;

    static ElasticsearchContainer createContainer(ElasticsearchDevServicesProperties properties) {
        return new ElasticsearchContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
    }

    static class ElasticsearchDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(ElasticsearchDevServicesProperties.CONFIG_PREFIX, ElasticsearchDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("elasticsearch")
                    .description("Elasticsearch Dev Service")
                    .properties(properties)
                    .container(ElasticsearchContainer.class, () -> createContainer(properties))
                    .port(ELASTICSEARCH_DEFAULT_PORT)
                    .discovery(ElasticsearchConnectionDetails.class,
                            container -> new ElasticsearchDiscoveredConnectionDetails(container, properties)));
        }

    }

}
