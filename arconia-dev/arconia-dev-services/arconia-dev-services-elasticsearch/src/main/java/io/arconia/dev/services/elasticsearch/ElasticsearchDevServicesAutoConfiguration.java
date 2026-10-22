package io.arconia.dev.services.elasticsearch;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.ssl.SslBundleFiles;
import io.arconia.dev.services.core.ssl.SslKeyStoreRequirement;
import io.arconia.dev.services.elasticsearch.ElasticsearchDevServicesAutoConfiguration.ElasticsearchDevServicesRegistrar;

/**
 * Auto-configuration for Elasticsearch Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("elasticsearch")
@EnableConfigurationProperties(ElasticsearchDevServicesProperties.class)
@Import(ElasticsearchDevServicesRegistrar.class)
public final class ElasticsearchDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "docker.elastic.co/elasticsearch/elasticsearch";

    static final int ELASTICSEARCH_DEFAULT_PORT = 9200;

    /**
     * The setting enabling SSL on the HTTP layer of Elasticsearch, which its security
     * auto-configuration turns on with a generated certificate unless set explicitly.
     */
    static final String HTTP_SSL_ENABLED = "xpack.security.http.ssl.enabled";

    /**
     * Where the SSL files are copied in the container.
     */
    static final String SSL_DIRECTORY = "/usr/share/elasticsearch/config/arconia-ssl/";

    static final String JAVA_OPTS = "ES_JAVA_OPTS";

    static final String DEFAULT_JAVA_OPTS = "-Xms512m -Xmx1g";

    static ElasticsearchContainer createContainer(ElasticsearchDevServicesProperties properties) {
        var container = new ElasticsearchContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        // A heap sized for development, instead of the fixed 2 GB Testcontainers configures.
        // The environment property of the dev service can override it.
        container.withEnv(JAVA_OPTS, DEFAULT_JAVA_OPTS);
        // Serves plain HTTP unless SSL is enabled through its properties.
        if (!properties.getSsl().isEnabled()) {
            container.withEnv(HTTP_SSL_ENABLED, "false");
        }
        return container;
    }

    /**
     * Configure Elasticsearch to serve HTTPS with the certificate and key of the bundle, in
     * place of the certificate it would generate.
     */
    static void configureSsl(ElasticsearchContainer container, SslBundle bundle) {
        container.withCopyToContainer(SslBundleFiles.certificateChain(bundle), SSL_DIRECTORY + "server.crt");
        container.withCopyToContainer(SslBundleFiles.privateKey(bundle), SSL_DIRECTORY + "server.key");
        container
                .withEnv(HTTP_SSL_ENABLED, "true")
                .withEnv("xpack.security.http.ssl.certificate", SSL_DIRECTORY + "server.crt")
                .withEnv("xpack.security.http.ssl.key", SSL_DIRECTORY + "server.key");
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
                    // With no key store configured, Elasticsearch generates its own certificate,
                    // which Spring Boot extracts from the container to trust it.
                    .ssl(ElasticsearchDevServicesAutoConfiguration::configureSsl, SslKeyStoreRequirement.OPTIONAL)
                    .discovery(ElasticsearchConnectionDetails.class,
                            container -> new ElasticsearchDiscoveredConnectionDetails(container, properties)));
        }

    }

}
