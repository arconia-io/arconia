package io.arconia.dev.services.elasticsearch;

import java.util.List;

import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
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

    private final Node node;

    ElasticsearchDiscoveredConnectionDetails(DiscoveredContainer container, ElasticsearchDevServicesProperties properties) {
        // The container is only asked how it would be configured, it is never started.
        ElasticsearchContainer configuration = ElasticsearchDevServicesAutoConfiguration.createContainer(properties);
        this.node = new Node(container.host(), container.mappedPort(ElasticsearchDevServicesAutoConfiguration.ELASTICSEARCH_DEFAULT_PORT),
                Node.Protocol.HTTP, USERNAME, configuration.getEnvMap().get(PASSWORD_ENVIRONMENT_VARIABLE));
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

}
