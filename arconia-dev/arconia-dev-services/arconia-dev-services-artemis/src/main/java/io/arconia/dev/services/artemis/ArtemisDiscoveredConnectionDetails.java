package io.arconia.dev.services.artemis;

import org.springframework.boot.artemis.autoconfigure.ArtemisConnectionDetails;
import org.springframework.boot.artemis.autoconfigure.ArtemisMode;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link ArtemisConnectionDetails} for connecting to a shared ActiveMQ Artemis dev service
 * running in a container discovered from another application.
 * <p>
 * The credentials come from the local configuration properties: they must match
 * the ones used by the application that started the shared container.
 */
final class ArtemisDiscoveredConnectionDetails implements ArtemisConnectionDetails {

    private final String brokerUrl;

    private final String user;

    private final String password;

    ArtemisDiscoveredConnectionDetails(DiscoveredContainer container, ArtemisDevServicesProperties properties) {
        this.brokerUrl = "tcp://%s:%d".formatted(container.host(), container.mappedPort(ArconiaArtemisContainer.TCP_PORT));
        this.user = properties.getUsername();
        this.password = properties.getPassword();
    }

    @Override
    public ArtemisMode getMode() {
        return ArtemisMode.NATIVE;
    }

    @Override
    public String getBrokerUrl() {
        return brokerUrl;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

}
