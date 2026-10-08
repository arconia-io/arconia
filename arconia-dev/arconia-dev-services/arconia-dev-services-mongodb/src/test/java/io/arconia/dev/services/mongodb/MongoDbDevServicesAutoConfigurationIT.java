package io.arconia.dev.services.mongodb;

import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mongodb.MongoDBContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MongoDbDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class MongoDbDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(MongoDbDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return MongoDbDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return MongoDBContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "mongodb";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return MongoConnectionDetails.class;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new MongoDbDevServicesProperties();
        return asDiscoverableContainer(new ArconiaMongoDbContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        MongoConnectionDetails connectionDetails = context.getBean(MongoConnectionDetails.class);
        assertThat(connectionDetails.getConnectionString().getConnectionString())
                .isEqualTo(((MongoDBContainer) discoveredContainer).getReplicaSetUrl());
    }

}
