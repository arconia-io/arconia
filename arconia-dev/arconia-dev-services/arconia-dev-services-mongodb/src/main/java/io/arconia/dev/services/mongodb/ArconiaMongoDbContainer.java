package io.arconia.dev.services.mongodb;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link MongoDBContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaMongoDbContainer extends MongoDBContainer {

    private final MongoDbDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "mongo";

    static final int MONGODB_PORT = 27017;

    public ArconiaMongoDbContainer(MongoDbDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        // Testcontainers uses a shared wait strategy instance across all containers.
        // MongoDBContainer doesn't set a wait strategy of its own, so when we customize
        // the startup timeout, it will be applied to all containers. Hence, we must
        // provide an explicit wait strategy.
        this.waitingFor(Wait.defaultWaitStrategy());
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), MONGODB_PORT);
        }
    }

}
