package io.arconia.dev.services.mongodb.atlas;

import com.mongodb.ConnectionString;

import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link MongoConnectionDetails} for connecting to a MongoDB dev service
 * running in a container discovered from another application.
 */
final class MongoDbAtlasDiscoveredConnectionDetails implements MongoConnectionDetails {

    private final ConnectionString connectionString;

    MongoDbAtlasDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.connectionString = new ConnectionString("mongodb://%s:%d/test?directConnection=true"
                .formatted(container.host(), container.mappedPort(ArconiaMongoDbAtlasLocalContainer.MONGODB_PORT)));
    }

    @Override
    public ConnectionString getConnectionString() {
        return connectionString;
    }

}
