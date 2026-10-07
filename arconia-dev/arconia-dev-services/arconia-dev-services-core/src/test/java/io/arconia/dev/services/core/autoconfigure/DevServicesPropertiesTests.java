package io.arconia.dev.services.core.autoconfigure;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesProperties}.
 */
class DevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        DevServicesProperties properties = new DevServicesProperties();
        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    void shouldUpdateValues() {
        DevServicesProperties properties = new DevServicesProperties();
        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    void shouldHaveNetworkDisabledByDefault() {
        DevServicesProperties properties = new DevServicesProperties();
        assertThat(properties.getNetwork().isEnabled()).isFalse();
    }

    @Test
    void shouldBindNetworkEnabled() {
        DevServicesProperties properties = new Binder(new MapConfigurationPropertySource(
                Map.of(DevServicesProperties.CONFIG_PREFIX + ".network.enabled", "true")))
                .bind(DevServicesProperties.CONFIG_PREFIX, Bindable.ofInstance(new DevServicesProperties()))
                .orElseGet(DevServicesProperties::new);

        assertThat(properties.getNetwork().isEnabled()).isTrue();
    }

}
