package io.arconia.multitenancy.core.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;

import org.junit.jupiter.api.Test;

import io.arconia.multitenancy.core.context.TenantContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantObservationFilter}.
 */
class TenantObservationFilterTests {

    @Test
    void whenEmptyTenantKeyThenThrow() {
        assertThatThrownBy(() -> new TenantObservationFilter("", Cardinality.HIGH))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenNullTenantKeyThenThrow() {
        assertThatThrownBy(() -> new TenantObservationFilter(null, Cardinality.HIGH))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenNullCardinalityThenThrow() {
        assertThatThrownBy(() -> new TenantObservationFilter("tenant.id", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cardinality cannot be null");
    }

    @Test
    void whenTenantContextSetThenObservationEnrichedInTracesOnly() {
        var registry = TestObservationRegistry.create();
        registry.observationConfig().observationFilter(new TenantObservationFilter());

        TenantContext.where("acme").run(() -> {
            Observation.start("test.observation", registry).stop();
        });

        TestObservationRegistryAssert.assertThat(registry)
            .hasObservationWithNameEqualTo("test.observation")
            .that()
            .hasHighCardinalityKeyValue(TenantObservationFilter.DEFAULT_TENANT_IDENTIFIER_KEY, "acme");
    }

    @Test
    void whenLowCardinalityThenObservationEnrichedInTracesAndMetrics() {
        var registry = TestObservationRegistry.create();
        registry.observationConfig()
            .observationFilter(
                    new TenantObservationFilter(TenantObservationFilter.DEFAULT_TENANT_IDENTIFIER_KEY, Cardinality.LOW));

        TenantContext.where("acme").run(() -> {
            Observation.start("test.observation", registry).stop();
        });

        TestObservationRegistryAssert.assertThat(registry)
            .hasObservationWithNameEqualTo("test.observation")
            .that()
            .hasLowCardinalityKeyValue(TenantObservationFilter.DEFAULT_TENANT_IDENTIFIER_KEY, "acme");
    }

    @Test
    void whenCustomKeyThenObservationUsesCustomKey() {
        var customKey = "tenant.identifier";
        var registry = TestObservationRegistry.create();
        registry.observationConfig().observationFilter(new TenantObservationFilter(customKey, Cardinality.HIGH));

        TenantContext.where("acme").run(() -> {
            Observation.start("test.observation", registry).stop();
        });

        TestObservationRegistryAssert.assertThat(registry)
            .hasObservationWithNameEqualTo("test.observation")
            .that()
            .hasHighCardinalityKeyValue(customKey, "acme");
    }

    @Test
    void whenNoTenantContextThenObservationNotEnriched() {
        var registry = TestObservationRegistry.create();
        registry.observationConfig().observationFilter(new TenantObservationFilter());

        Observation.start("test.observation", registry).stop();

        TestObservationRegistryAssert.assertThat(registry)
            .hasObservationWithNameEqualTo("test.observation")
            .that()
            .doesNotHaveHighCardinalityKeyValueWithKey(TenantObservationFilter.DEFAULT_TENANT_IDENTIFIER_KEY);
    }

}
