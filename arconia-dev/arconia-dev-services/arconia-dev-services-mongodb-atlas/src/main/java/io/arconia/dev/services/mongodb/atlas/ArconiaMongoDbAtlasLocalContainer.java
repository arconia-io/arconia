package io.arconia.dev.services.mongodb.atlas;

import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link MongoDBAtlasLocalContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaMongoDbAtlasLocalContainer extends MongoDBAtlasLocalContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "mongodb/mongodb-atlas-local";

    private final MongoDbAtlasDevServicesProperties properties;

    static final int MONGODB_PORT = 27017;

    public ArconiaMongoDbAtlasLocalContainer(MongoDbAtlasDevServicesProperties properties) {
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
