package io.arconia.dev.services.kafka;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link KafkaContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaKafkaContainer extends KafkaContainer {

    private final KafkaDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "apache/kafka-native";

    static final int KAFKA_PORT = 9092;

    static final String READY_REGEX = ".*Transitioning from RECOVERY to RUNNING.*";

    public ArconiaKafkaContainer(KafkaDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        // KafkaContainer waits on a wait strategy instance shared across all Kafka containers,
        // so when we customize the startup timeout, it will be applied to all of them.
        // Hence, we must provide an equivalent wait strategy of our own.
        this.waitingFor(Wait.forLogMessage(READY_REGEX, 1));
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), KAFKA_PORT);
        }
    }

}
