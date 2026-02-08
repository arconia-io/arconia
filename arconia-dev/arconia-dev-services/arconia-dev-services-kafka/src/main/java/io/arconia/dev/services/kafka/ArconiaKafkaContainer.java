package io.arconia.dev.services.kafka;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link KafkaContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaKafkaContainer extends KafkaContainer {

    private final KafkaDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "apache/kafka-native";

    static final int KAFKA_PORT = 9092;

    public ArconiaKafkaContainer(KafkaDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), KAFKA_PORT);
        }
    }

}
