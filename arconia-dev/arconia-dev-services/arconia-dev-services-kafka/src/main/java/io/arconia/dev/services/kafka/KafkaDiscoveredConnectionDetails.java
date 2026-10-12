package io.arconia.dev.services.kafka;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link KafkaConnectionDetails} for connecting to a Kafka dev service
 * running in a container discovered from another application.
 */
final class KafkaDiscoveredConnectionDetails implements KafkaConnectionDetails {

    private final List<String> bootstrapServers;

    @Nullable
    private final SslBundle sslBundle;

    KafkaDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.bootstrapServers = List.of("%s:%d".formatted(container.host(),
                container.mappedPort(KafkaDevServicesAutoConfiguration.KAFKA_PORT)));
        this.sslBundle = container.sslBundle();
    }

    @Override
    public List<String> getBootstrapServers() {
        return bootstrapServers;
    }

    @Override
    public String getSecurityProtocol() {
        // The listener the dev service container publishes is in plaintext, or over SSL when
        // the dev service is secured with it, as Spring Boot reports for a container this
        // application started itself.
        return (sslBundle != null) ? "SSL" : "PLAINTEXT";
    }

    @Override
    @Nullable
    public SslBundle getSslBundle() {
        return sslBundle;
    }

}
