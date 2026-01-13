package io.arconia.dev.services.mongodb.atlas;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MongoDbAtlasDevServicesProperties}.
 */
class MongoDbAtlasDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        MongoDbAtlasDevServicesProperties properties = new MongoDbAtlasDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("mongodb/mongodb-atlas-local");
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.NEVER);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldUpdateValues() {
        MongoDbAtlasDevServicesProperties properties = new MongoDbAtlasDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("mongodb/mongodb-atlas-local");
        properties.setPort(27017);
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("mongodb/mongodb-atlas-local");
        assertThat(properties.getPort()).isEqualTo(27017);
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));

    }

}
