package io.arconia.dev.services.mongodb;

import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link MongoDBContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaMongoDbContainer extends MongoDBContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "mongo";

    private final MongoDbDevServicesProperties properties;

    static final int MONGODB_PORT = 27017;

    public ArconiaMongoDbContainer(MongoDbDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), MONGODB_PORT);
        }
    }

}
