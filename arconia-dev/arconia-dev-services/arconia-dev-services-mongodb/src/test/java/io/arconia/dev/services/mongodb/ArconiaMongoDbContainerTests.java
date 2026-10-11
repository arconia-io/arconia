package io.arconia.dev.services.mongodb;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaMongoDbContainer}.
 */
class ArconiaMongoDbContainerTests extends BaseDevServiceContainerTests<MongoDbDevServicesProperties, ArconiaMongoDbContainer> {

    @Override
    protected MongoDbDevServicesProperties createProperties() {
        return new MongoDbDevServicesProperties();
    }

    @Override
    protected ArconiaMongoDbContainer createContainer(MongoDbDevServicesProperties properties) {
        return new ArconiaMongoDbContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(MongoDbDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, ArconiaMongoDbContainer.MONGODB_PORT);
    }

}
