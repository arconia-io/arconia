package io.arconia.dev.services.core.registration;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link DevServiceDynamicPropertySource}.
 */
class DevServiceDynamicPropertySourceTests {

    @Test
    void addAndResolveProperty() {
        var environment = new MockEnvironment();
        var propertySource = DevServiceDynamicPropertySource.getOrCreate(environment);

        propertySource.add("my.property", () -> "my-value");

        assertThat(environment.getProperty("my.property")).isEqualTo("my-value");
    }

    @Test
    void supplierIsResolvedLazily() {
        var environment = new MockEnvironment();
        var propertySource = DevServiceDynamicPropertySource.getOrCreate(environment);

        int[] callCount = {0};
        propertySource.add("lazy.property", () -> {
            callCount[0]++;
            return "resolved";
        });

        assertThat(callCount[0]).isZero();
        assertThat(environment.getProperty("lazy.property")).isEqualTo("resolved");
        assertThat(callCount[0]).isEqualTo(1);
    }

    @Test
    void supplierIsInvokedOnEachAccess() {
        var environment = new MockEnvironment();
        var propertySource = DevServiceDynamicPropertySource.getOrCreate(environment);

        var value = new AtomicReference<>("initial");
        propertySource.add("dynamic.property", value::get);

        assertThat(environment.getProperty("dynamic.property")).isEqualTo("initial");

        value.set("updated");
        assertThat(environment.getProperty("dynamic.property")).isEqualTo("updated");
    }

    @Test
    void addOverridesExistingProperty() {
        var environment = new MockEnvironment();
        var propertySource = DevServiceDynamicPropertySource.getOrCreate(environment);

        propertySource.add("my.property", () -> "original");
        assertThat(environment.getProperty("my.property")).isEqualTo("original");

        propertySource.add("my.property", () -> "overridden");
        assertThat(environment.getProperty("my.property")).isEqualTo("overridden");
    }

    @Test
    void getOrCreateReturnsSameInstance() {
        var environment = new MockEnvironment();

        var first = DevServiceDynamicPropertySource.getOrCreate(environment);
        var second = DevServiceDynamicPropertySource.getOrCreate(environment);

        assertThat(first).isSameAs(second);
    }

    @Test
    void multipleRegistrarsShareSamePropertySource() {
        var environment = new MockEnvironment();

        var propertySource = DevServiceDynamicPropertySource.getOrCreate(environment);
        propertySource.add("first.property", () -> "first-value");

        var samePropertySource = DevServiceDynamicPropertySource.getOrCreate(environment);
        samePropertySource.add("second.property", () -> "second-value");

        assertThat(environment.getProperty("first.property")).isEqualTo("first-value");
        assertThat(environment.getProperty("second.property")).isEqualTo("second-value");
    }

    @Test
    void getOrCreateThrowsWhenConflictingPropertySourceExists() {
        var environment = new MockEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource(DevServiceDynamicPropertySource.PROPERTY_SOURCE_NAME, Map.of()));

        assertThatIllegalStateException()
                .isThrownBy(() -> DevServiceDynamicPropertySource.getOrCreate(environment));
    }

    @Test
    void propertySourceHasHighestPrecedence() {
        var environment = new MockEnvironment();
        environment.setProperty("my.property", "from-environment");

        var propertySource = DevServiceDynamicPropertySource.getOrCreate(environment);
        propertySource.add("my.property", () -> "from-dev-service");

        assertThat(environment.getProperty("my.property")).isEqualTo("from-dev-service");
    }

}
