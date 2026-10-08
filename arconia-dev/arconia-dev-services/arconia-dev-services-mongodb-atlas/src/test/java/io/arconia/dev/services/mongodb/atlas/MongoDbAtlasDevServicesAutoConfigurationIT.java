package io.arconia.dev.services.mongodb.atlas;

import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MongoDbAtlasDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class MongoDbAtlasDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(MongoDbAtlasDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return MongoDbAtlasDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return MongoDBAtlasLocalContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "mongodb-atlas";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return MongoConnectionDetails.class;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new MongoDbAtlasDevServicesProperties();
        return asDiscoverableContainer(new ArconiaMongoDbAtlasLocalContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        MongoConnectionDetails connectionDetails = context.getBean(MongoConnectionDetails.class);
        assertThat(connectionDetails.getConnectionString().getConnectionString())
                .isEqualTo(((MongoDBAtlasLocalContainer) discoveredContainer).getDatabaseConnectionString());
    }

}
