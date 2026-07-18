package io.arconia.dev.services.elasticsearch;

import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * An {@link ElasticsearchContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaElasticsearchContainer extends ElasticsearchContainer {

    private final ElasticsearchDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "docker.elastic.co/elasticsearch/elasticsearch";

    static final int ELASTICSEARCH_DEFAULT_PORT = 9200;

    public ArconiaElasticsearchContainer(ElasticsearchDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), ELASTICSEARCH_DEFAULT_PORT);
        }
    }

}
