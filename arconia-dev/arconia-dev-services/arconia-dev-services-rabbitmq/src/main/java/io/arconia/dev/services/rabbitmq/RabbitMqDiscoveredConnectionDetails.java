package io.arconia.dev.services.rabbitmq;

import java.util.List;

import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link RabbitConnectionDetails} for connecting to a shared RabbitMQ dev service
 * running in a container discovered from another application.
 * <p>
 * The credentials come from the local configuration properties: they must match
 * the ones used by the application that started the shared container.
 */
final class RabbitMqDiscoveredConnectionDetails implements RabbitConnectionDetails {

    private final Address address;

    private final String username;

    private final String password;

    RabbitMqDiscoveredConnectionDetails(DiscoveredContainer container, RabbitMqDevServicesProperties properties) {
        this.address = new Address(container.host(), container.mappedPort(ArconiaRabbitMqContainer.AMQP_PORT));
        this.username = properties.getUsername();
        this.password = properties.getPassword();
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public List<Address> getAddresses() {
        return List.of(address);
    }

}
