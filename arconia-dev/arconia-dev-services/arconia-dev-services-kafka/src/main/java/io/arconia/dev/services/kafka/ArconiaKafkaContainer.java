package io.arconia.dev.services.kafka;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link KafkaContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaKafkaContainer extends KafkaContainer {

    public ArconiaKafkaContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
