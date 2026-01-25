package io.arconia.dev.services.mongodb;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MongoDbDevServicesProperties}.
 */
class MongoDbDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        MongoDbDevServicesProperties properties = new MongoDbDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("mongo");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldUpdateValues() {
        MongoDbDevServicesProperties properties = new MongoDbDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("mongo:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(ArconiaMongoDbContainer.MONGODB_PORT);
        properties.setShared(true);
        properties.setStartupTimeout(Duration.ofMinutes(1));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("mongo:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(ArconiaMongoDbContainer.MONGODB_PORT);
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));
    }

}
