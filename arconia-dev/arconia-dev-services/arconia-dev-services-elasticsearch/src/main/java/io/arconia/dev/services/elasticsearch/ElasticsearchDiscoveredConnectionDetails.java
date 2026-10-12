package io.arconia.dev.services.elasticsearch;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link ElasticsearchConnectionDetails} for connecting to an Elasticsearch dev service
 * running in a container discovered from another application.
 */
final class ElasticsearchDiscoveredConnectionDetails implements ElasticsearchConnectionDetails {

    /**
     * The built-in superuser of Elasticsearch, which the password applies to.
     */
    private static final String USERNAME = "elastic";

    private static final String PASSWORD_ENVIRONMENT_VARIABLE = "ELASTIC_PASSWORD";

    /**
     * Where Elasticsearch writes the certificate authority of the certificate it generates,
     * which Testcontainers extracts for a container the application starts itself.
     */
    static final String GENERATED_CA_CERTIFICATE_PATH = "/usr/share/elasticsearch/config/certs/http_ca.crt";

    private final Node node;

    @Nullable
    private final SslBundle sslBundle;

    ElasticsearchDiscoveredConnectionDetails(DiscoveredContainer container, ElasticsearchDevServicesProperties properties) {
        // The container is only asked how it would be configured, it is never started.
        ElasticsearchContainer configuration = ElasticsearchDevServicesAutoConfiguration.createContainer(properties);
        this.sslBundle = resolveSslBundle(container, properties);
        this.node = new Node(container.host(), container.mappedPort(ElasticsearchDevServicesAutoConfiguration.ELASTICSEARCH_DEFAULT_PORT),
                (sslBundle != null) ? Node.Protocol.HTTPS : Node.Protocol.HTTP, USERNAME, configuration.getEnvMap().get(PASSWORD_ENVIRONMENT_VARIABLE));
    }

    /**
     * The bundle to connect to the discovered container with: the one trusting the certificate
     * the dev service is configured to present or, when SSL is enabled and no key store
     * is configured, one trusting the certificate authority the discovered container generated,
     * as Spring Boot does for a container the application starts itself.
     */
    @Nullable
    private static SslBundle resolveSslBundle(DiscoveredContainer container, ElasticsearchDevServicesProperties properties) {
        if (container.sslBundle() != null || !properties.getSsl().isEnabled()) {
            return container.sslBundle();
        }
        String caCertificate = new String(container.copyFileFromContainer(GENERATED_CA_CERTIFICATE_PATH), StandardCharsets.US_ASCII);
        return SslBundle.of(new PemSslStoreBundle(null, PemSslStoreDetails.forCertificate(caCertificate)));
    }

    @Override
    public List<Node> getNodes() {
        return List.of(node);
    }

    @Override
    public String getUsername() {
        return node.username();
    }

    @Override
    public String getPassword() {
        return node.password();
    }

    @Override
    @Nullable
    public SslBundle getSslBundle() {
        return sslBundle;
    }

}
