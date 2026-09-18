package io.arconia.dev.services.keycloak;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KeycloakResourceServerDevServicesProperties}.
 */
class KeycloakResourceServerDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        assertThat(new KeycloakResourceServerDevServicesProperties().isEnabled()).isTrue();
    }

    @Test
    void shouldBindProperties() {
        KeycloakResourceServerDevServicesProperties properties = new KeycloakResourceServerDevServicesProperties();

        new Binder(new MapConfigurationPropertySource(Map.of("enabled", "false")))
                .bind("", Bindable.ofInstance(properties));

        assertThat(properties.isEnabled()).isFalse();
    }

}
