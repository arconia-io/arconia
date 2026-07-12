package io.arconia.dev.services.elasticsearch;

import io.arconia.dev.services.core.container.ContainerConfigurer;

import io.arconia.dev.services.core.util.ContainerUtils;

import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

public class ArconiaElasticsearchContainer extends ElasticsearchContainer {
    private final ElasticsearchDevServicesProperties properties;

    public static final String COMPATIBLE_IMAGE_NAME = "elasticsearch";
    static final int DEFAULT_ES_PORT = 9200;

    public ArconiaElasticsearchContainer(ElasticsearchDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), DEFAULT_ES_PORT);
        }
    }
}
