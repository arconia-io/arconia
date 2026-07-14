package io.arconia.dev.services.kafka;

import java.util.List;

import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link KafkaConnectionDetails} for connecting to a shared Kafka dev service
 * running in a container discovered from another application.
 * <p>
 * The container advertises its listener as the Docker host address and mapped port,
 * which resolve to the same values for every application on the host, so the
 * discovered bootstrap server works across applications without coordination.
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

}
