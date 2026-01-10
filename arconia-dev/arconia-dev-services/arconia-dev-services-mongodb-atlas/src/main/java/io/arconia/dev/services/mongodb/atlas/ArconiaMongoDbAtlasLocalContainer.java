package io.arconia.dev.services.mongodb.atlas;

import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link MongoDBAtlasLocalContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaMongoDbAtlasLocalContainer extends MongoDBAtlasLocalContainer {

    public ArconiaMongoDbAtlasLocalContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
