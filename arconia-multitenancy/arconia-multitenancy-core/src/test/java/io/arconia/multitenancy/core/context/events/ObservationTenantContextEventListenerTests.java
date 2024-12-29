package io.arconia.multitenancy.core.context.events;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ObservationTenantContextEventListener}.
 */
class ObservationTenantContextEventListenerTests {

    @Test
    void whenNullCardinalityThenThrow() {
        assertThatThrownBy(() -> new ObservationTenantContextEventListener("tenant.id", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cardinality cannot be null");
    }

    @Test
    void whenEmptyTenantKeyThenThrow() {
        assertThatThrownBy(() -> new ObservationTenantContextEventListener("",
                ObservationTenantContextEventListener.Cardinality.HIGH))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenNullTenantKeyThenThrow() {
        assertThatThrownBy(() -> new ObservationTenantContextEventListener(null,
                ObservationTenantContextEventListener.Cardinality.HIGH))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenDefaultValueIsUsedAsKey() {
        var tenantIdentifier = "acme";
        var listener = new ObservationTenantContextEventListener();
        var observationContext = new Observation.Context();
        var event = new TenantContextAttachedEvent(tenantIdentifier, this);
        event.setObservationContext(observationContext);

        listener.onApplicationEvent(event);

        assertThat(observationContext
            .getHighCardinalityKeyValue(ObservationTenantContextEventListener.DEFAULT_TENANT_IDENTIFIER_KEY)).isEqualTo(
                    KeyValue.of(ObservationTenantContextEventListener.DEFAULT_TENANT_IDENTIFIER_KEY, tenantIdentifier));
    }

    @Test
    void whenCustomValueIsUsedAsKey() {
        var tenantKey = "tenant.identifier";
        var tenantIdentifier = "acme";
        var listener = new ObservationTenantContextEventListener(tenantKey,
                ObservationTenantContextEventListener.DEFAULT_CARDINALITY);
        var observationContext = new Observation.Context();
        var event = new TenantContextAttachedEvent(tenantIdentifier, this);
        event.setObservationContext(observationContext);

        listener.onApplicationEvent(event);

        assertThat(observationContext.getHighCardinalityKeyValue(tenantKey))
            .isEqualTo(KeyValue.of(tenantKey, tenantIdentifier));
    }

    @Test
    void whenCustomCardinalityIsUsed() {
        var tenantIdentifier = "acme";
        var listener = new ObservationTenantContextEventListener(
                ObservationTenantContextEventListener.DEFAULT_TENANT_IDENTIFIER_KEY,
                ObservationTenantContextEventListener.Cardinality.LOW);
        var observationContext = new Observation.Context();
        var event = new TenantContextAttachedEvent(tenantIdentifier, this);
        event.setObservationContext(observationContext);

        listener.onApplicationEvent(event);

        assertThat(observationContext
            .getLowCardinalityKeyValue(ObservationTenantContextEventListener.DEFAULT_TENANT_IDENTIFIER_KEY)).isEqualTo(
                    KeyValue.of(ObservationTenantContextEventListener.DEFAULT_TENANT_IDENTIFIER_KEY, tenantIdentifier));
    }

}
