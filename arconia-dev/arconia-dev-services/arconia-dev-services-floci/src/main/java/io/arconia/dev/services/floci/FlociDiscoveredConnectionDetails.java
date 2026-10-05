package io.arconia.dev.services.floci;

import java.net.URI;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link AwsConnectionDetails} for connecting to a Floci dev service
 * running in a container discovered from another application.
 */
final class FlociDiscoveredConnectionDetails implements AwsConnectionDetails {

    private final URI endpoint;

    private final String region;

    private final String accessKey;

    private final String secretKey;

    FlociDiscoveredConnectionDetails(DiscoveredContainer container, FlociDevServicesProperties properties) {
        // The container is only asked how it would be configured, it is never started.
        ArconiaFlociContainer configuration = new ArconiaFlociContainer(properties);
        this.endpoint = URI.create("http://%s:%d".formatted(container.host(), container.mappedPort(ArconiaFlociContainer.PORT)));
        this.region = configuration.getRegion();
        this.accessKey = configuration.getAccessKey();
        this.secretKey = configuration.getSecretKey();
    }

    @Override
    public URI getEndpoint() {
        return endpoint;
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getAccessKey() {
        return accessKey;
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

}
