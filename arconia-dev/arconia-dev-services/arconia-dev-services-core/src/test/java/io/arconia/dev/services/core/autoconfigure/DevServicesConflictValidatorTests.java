package io.arconia.dev.services.core.autoconfigure;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.provider.DevServiceProvider;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DevServicesConflictValidator}.
 */
class DevServicesConflictValidatorTests {

    private final DevServicesConflictValidator validator = new DevServicesConflictValidator();

    @Test
    void doesNotFailWhenNoConflict() {
        assertThatCode(() -> validator.validate(List.of(
                DevServiceProvider.of("postgresql", "jdbc"),
                DevServiceProvider.of("lgtm", "opentelemetry"))))
                .doesNotThrowAnyException();
    }

    @Test
    void failsWhenTwoProvidersShareCategory() {
        assertThatThrownBy(() -> validator.validate(List.of(
                DevServiceProvider.of("lgtm", "opentelemetry"),
                DevServiceProvider.of("openlit", "opentelemetry"))))
                .isInstanceOf(MultipleDevServicesException.class)
                .hasMessageContaining("opentelemetry")
                .hasMessageContaining("lgtm")
                .hasMessageContaining("openlit");
    }

    @Test
    void failsWhenProvidersCollectionIsNull() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void failsWhenProvidersContainNullElement() {
        assertThatThrownBy(() -> validator.validate(Arrays.asList(
                DevServiceProvider.of("lgtm", "opentelemetry"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null elements");
    }

}
