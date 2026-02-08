package io.arconia.dev.services.mongodb;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

/**
 * Unit tests for {@link MongoDbDevServicesProperties}.
 */
class MongoDbDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<MongoDbDevServicesProperties> {

    @Override
    protected MongoDbDevServicesProperties createProperties() {
        return new MongoDbDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaMongoDbContainer.COMPATIBLE_IMAGE_NAME)
                .build();
    }

}
