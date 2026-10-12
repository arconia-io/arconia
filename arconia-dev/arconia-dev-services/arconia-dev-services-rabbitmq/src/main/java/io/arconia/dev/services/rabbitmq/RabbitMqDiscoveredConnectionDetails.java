package io.arconia.dev.services.rabbitmq;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link RabbitConnectionDetails} for connecting to a RabbitMQ dev service
 * running in a container discovered from another application.
 */
final class RabbitMqDiscoveredConnectionDetails implements RabbitConnectionDetails {

    private final Address address;

    private final String username;

    private final String password;

    @Nullable
    private final SslBundle sslBundle;

    RabbitMqDiscoveredConnectionDetails(DiscoveredContainer container, RabbitMqDevServicesProperties properties) {
        this.sslBundle = container.sslBundle();
        int port = (sslBundle != null) ? RabbitMqDevServicesAutoConfiguration.AMQPS_PORT : RabbitMqDevServicesAutoConfiguration.AMQP_PORT;
        this.address = new Address(container.host(), container.mappedPort(port));
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

    @Override
    @Nullable
    public SslBundle getSslBundle() {
        return sslBundle;
    }

}
