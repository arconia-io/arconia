package io.arconia.dev.services.mongodb.atlas;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaMongoDbAtlasLocalContainer}.
 */
class ArconiaMongoDbAtlasLocalContainerTests extends BaseDevServiceContainerTests<MongoDbAtlasDevServicesProperties, ArconiaMongoDbAtlasLocalContainer> {

    @Override
    protected MongoDbAtlasDevServicesProperties createProperties() {
        return new MongoDbAtlasDevServicesProperties();
    }

    @Override
    protected ArconiaMongoDbAtlasLocalContainer createContainer(MongoDbAtlasDevServicesProperties properties) {
        return new ArconiaMongoDbAtlasLocalContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(MongoDbAtlasDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, ArconiaMongoDbAtlasLocalContainer.MONGODB_PORT);
    }

}
