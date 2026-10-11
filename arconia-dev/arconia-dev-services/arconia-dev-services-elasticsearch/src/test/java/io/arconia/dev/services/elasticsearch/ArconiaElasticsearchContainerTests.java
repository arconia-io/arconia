package io.arconia.dev.services.elasticsearch;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaElasticsearchContainer}.
 */
class ArconiaElasticsearchContainerTests extends BaseDevServiceContainerTests<ElasticsearchDevServicesProperties, ArconiaElasticsearchContainer> {

    @Override
    protected ElasticsearchDevServicesProperties createProperties() {
        return new ElasticsearchDevServicesProperties();
    }

    @Override
    protected ArconiaElasticsearchContainer createContainer(ElasticsearchDevServicesProperties properties) {
        return new ArconiaElasticsearchContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(ElasticsearchDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, ArconiaElasticsearchContainer.ELASTICSEARCH_DEFAULT_PORT);
    }

}
