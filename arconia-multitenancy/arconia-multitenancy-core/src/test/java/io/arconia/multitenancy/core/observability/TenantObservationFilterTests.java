package io.arconia.multitenancy.core.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;

import org.junit.jupiter.api.Test;

import io.arconia.multitenancy.core.context.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantObservationFilter}.
 */
class TenantObservationFilterTests {

    @Test
    void whenEmptyTenantKeyThenThrow() {
        assertThatThrownBy(() -> TenantObservationFilter.builder().tenantIdentifierKey("").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenNullTenantKeyThenThrow() {
        assertThatThrownBy(() -> TenantObservationFilter.builder().tenantIdentifierKey(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenNullCardinalityThenThrow() {
        assertThatThrownBy(() -> TenantObservationFilter.builder().cardinality(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cardinality cannot be null");
    }

    @Test
    void whenDefaultsThenTenantIdKeyAndHighCardinality() {
        var filter = TenantObservationFilter.builder().build();

        assertThat(filter.getTenantIdentifierKey()).isEqualTo("tenant.id");
        assertThat(filter.getCardinality()).isEqualTo(Cardinality.HIGH);
    }

    @Test
    void whenTenantContextSetThenObservationEnrichedInTracesOnly() {
        var registry = TestObservationRegistry.create();
        var filter = TenantObservationFilter.builder().build();
        registry.observationConfig().observationFilter(filter);

        TenantContext.where("acme").run(() -> {
            Observation.start("test.observation", registry).stop();
        });

        TestObservationRegistryAssert.assertThat(registry)
            .hasObservationWithNameEqualTo("test.observation")
            .that()
            .hasHighCardinalityKeyValue(filter.getTenantIdentifierKey(), "acme");
    }

    @Test
    void whenLowCardinalityThenObservationEnrichedInTracesAndMetrics() {
        var registry = TestObservationRegistry.create();
        var filter = TenantObservationFilter.builder().cardinality(Cardinality.LOW).build();
        registry.observationConfig().observationFilter(filter);

        TenantContext.where("acme").run(() -> {
            Observation.start("test.observation", registry).stop();
        });

        TestObservationRegistryAssert.assertThat(registry)
            .hasObservationWithNameEqualTo("test.observation")
            .that()
            .hasLowCardinalityKeyValue(filter.getTenantIdentifierKey(), "acme");
    }

    @Test
    void whenCustomKeyThenObservationUsesCustomKey() {
        var customKey = "tenant.identifier";
        var registry = TestObservationRegistry.create();
        registry.observationConfig()
            .observationFilter(TenantObservationFilter.builder().tenantIdentifierKey(customKey).build());

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
        var filter = TenantObservationFilter.builder().build();
        registry.observationConfig().observationFilter(filter);

        Observation.start("test.observation", registry).stop();

        TestObservationRegistryAssert.assertThat(registry)
            .hasObservationWithNameEqualTo("test.observation")
            .that()
            .doesNotHaveHighCardinalityKeyValueWithKey(filter.getTenantIdentifierKey());
    }

}
