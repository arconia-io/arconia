package io.arconia.dev.services.mongodb;

import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link MongoDBContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaMongoDbContainer extends MongoDBContainer {

    public ArconiaMongoDbContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
