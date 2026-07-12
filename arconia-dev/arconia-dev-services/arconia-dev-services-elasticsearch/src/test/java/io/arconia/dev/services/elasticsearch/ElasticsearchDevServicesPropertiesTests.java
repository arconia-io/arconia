package io.arconia.dev.services.elasticsearch;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

public class ElasticsearchDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<ElasticsearchDevServicesProperties>  {
    @Override
    protected ElasticsearchDevServicesProperties createProperties() {
        return new ElasticsearchDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaElasticsearchContainer.COMPATIBLE_IMAGE_NAME)
                .build();
    }
}
