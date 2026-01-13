package io.arconia.dev.services.kafka;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link KafkaContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaKafkaContainer extends KafkaContainer {

    private final KafkaDevServicesProperties properties;

    /**
     * Kafka broker port.
     */
    protected static final int KAFKA_PORT = 9092;

    public ArconiaKafkaContainer(DockerImageName dockerImageName, KafkaDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), KAFKA_PORT);
        }
    }
}
