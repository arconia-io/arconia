package io.arconia.dev.services.mongodb;

import com.mongodb.ConnectionString;

import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link MongoConnectionDetails} for connecting to a MongoDB dev service
 * running in a container discovered from another application.
 */
final class MongoDbDiscoveredConnectionDetails implements MongoConnectionDetails {

    private final ConnectionString connectionString;

    MongoDbDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.connectionString = new ConnectionString("mongodb://%s:%d/test"
                .formatted(container.host(), container.mappedPort(ArconiaMongoDbContainer.MONGODB_PORT)));
    }

    @Override
    public ConnectionString getConnectionString() {
        return connectionString;
    }

}
