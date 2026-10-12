package io.arconia.dev.services.mongodb;

import com.mongodb.ConnectionString;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link MongoConnectionDetails} for connecting to a MongoDB dev service
 * running in a container discovered from another application.
 */
final class MongoDbDiscoveredConnectionDetails implements MongoConnectionDetails {

    private final ConnectionString connectionString;

    @Nullable
    private final SslBundle sslBundle;

    MongoDbDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.connectionString = new ConnectionString("mongodb://%s:%d/test"
                .formatted(container.host(), container.mappedPort(MongoDbDevServicesAutoConfiguration.MONGODB_PORT)));
        this.sslBundle = container.sslBundle();
    }

    @Override
    public ConnectionString getConnectionString() {
        return connectionString;
    }

    @Override
    @Nullable
    public SslBundle getSslBundle() {
        return sslBundle;
    }

}
