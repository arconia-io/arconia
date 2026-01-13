package io.arconia.dev.services.mongodb;

import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link MongoDBContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaMongoDbContainer extends MongoDBContainer {

    private final MongoDbDevServicesProperties properties;

    /**
     * MongoDB wire protocol port.
     */
    private static final int MONGODB_PORT = 27017;

    public ArconiaMongoDbContainer(DockerImageName dockerImageName, MongoDbDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), MONGODB_PORT);
        }
    }
}
