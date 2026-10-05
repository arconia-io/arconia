package io.arconia.dev.services.kafka;

import java.util.List;

import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link KafkaConnectionDetails} for connecting to a Kafka dev service
 * running in a container discovered from another application.
 */
final class KafkaDiscoveredConnectionDetails implements KafkaConnectionDetails {

    private final List<String> bootstrapServers;

    KafkaDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.bootstrapServers = List.of("%s:%d".formatted(container.host(),
                container.mappedPort(ArconiaKafkaContainer.KAFKA_PORT)));
    }

    @Override
    public List<String> getBootstrapServers() {
        return bootstrapServers;
    }

    @Override
    public String getSecurityProtocol() {
        // The dev service container exposes a plaintext listener, as Spring Boot reports
        // for a container this application started itself.
        return "PLAINTEXT";
    }

}
