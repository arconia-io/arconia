package io.arconia.dev.services.pulsar;

import org.springframework.boot.pulsar.autoconfigure.PulsarConnectionDetails;
import org.testcontainers.pulsar.PulsarContainer;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link PulsarConnectionDetails} for connecting to a shared Pulsar dev service
 * running in a container discovered from another application.
 */
final class PulsarDiscoveredConnectionDetails implements PulsarConnectionDetails {

    private final String brokerUrl;

    private final String adminUrl;

    PulsarDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.brokerUrl = "pulsar://%s:%d".formatted(container.host(),
                container.mappedPort(PulsarContainer.BROKER_PORT));
        this.adminUrl = "http://%s:%d".formatted(container.host(),
                container.mappedPort(PulsarContainer.BROKER_HTTP_PORT));
    }

    @Override
    public String getBrokerUrl() {
        return brokerUrl;
    }

    @Override
    public String getAdminUrl() {
        return adminUrl;
    }

}
