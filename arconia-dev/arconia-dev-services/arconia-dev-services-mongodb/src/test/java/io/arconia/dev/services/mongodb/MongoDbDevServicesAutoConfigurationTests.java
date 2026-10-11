package io.arconia.dev.services.mongodb;

import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mongodb.MongoDBContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoDbDevServicesAutoConfiguration}.
 */
class MongoDbDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    MongoDbDevServicesAutoConfigurationTests() {
        super(MongoDbDevServicesAutoConfiguration.class, "mongodb", MongoDBContainer.class, MongoConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new MongoDbDevServicesProperties();
        return asDiscoverableContainer(MongoDbDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        MongoConnectionDetails connectionDetails = context.getBean(MongoConnectionDetails.class);
        assertThat(connectionDetails.getConnectionString().getConnectionString())
                .isEqualTo(((MongoDBContainer) discoveredContainer).getReplicaSetUrl());
    }

}
