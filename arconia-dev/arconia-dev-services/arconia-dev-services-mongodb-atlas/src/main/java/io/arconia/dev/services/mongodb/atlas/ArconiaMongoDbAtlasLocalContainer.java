package io.arconia.dev.services.mongodb.atlas;

import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link MongoDBAtlasLocalContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaMongoDbAtlasLocalContainer extends MongoDBAtlasLocalContainer {

    private final MongoDbAtlasDevServicesProperties properties;

    /**
     * Atlas-compatible MongoDB protocol port.
     */
    private static final int MONGODB_ATLAS_PORT = 27017;

    public ArconiaMongoDbAtlasLocalContainer(DockerImageName dockerImageName, MongoDbAtlasDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), MONGODB_ATLAS_PORT);
        }
    }
}
