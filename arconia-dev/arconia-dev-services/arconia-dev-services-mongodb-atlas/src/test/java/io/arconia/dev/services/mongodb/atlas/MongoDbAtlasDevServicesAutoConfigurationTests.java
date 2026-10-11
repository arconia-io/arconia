package io.arconia.dev.services.mongodb.atlas;

import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoDbAtlasDevServicesAutoConfiguration}.
 */
class MongoDbAtlasDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    MongoDbAtlasDevServicesAutoConfigurationTests() {
        super(MongoDbAtlasDevServicesAutoConfiguration.class, "mongodb-atlas", MongoDBAtlasLocalContainer.class, MongoConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new MongoDbAtlasDevServicesProperties();
        return asDiscoverableContainer(MongoDbAtlasDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        MongoConnectionDetails connectionDetails = context.getBean(MongoConnectionDetails.class);
        assertThat(connectionDetails.getConnectionString().getConnectionString())
                .isEqualTo(((MongoDBAtlasLocalContainer) discoveredContainer).getDatabaseConnectionString());
    }

}
