package io.arconia.dev.services.mongodb.atlas;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

/**
 * Unit tests for {@link MongoDbAtlasDevServicesProperties}.
 */
class MongoDbAtlasDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<MongoDbAtlasDevServicesProperties> {

    @Override
    protected MongoDbAtlasDevServicesProperties createProperties() {
        return new MongoDbAtlasDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaMongoDbAtlasLocalContainer.COMPATIBLE_IMAGE_NAME)
                .build();
    }

}
