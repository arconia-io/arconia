package io.arconia.core.multitenancy.context.events;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObservationTenantContextEventListenerTests {

    @Test
    void whenNullCardinalityThenThrow() {
        assertThatThrownBy(() -> new ObservationTenantContextEventListener(null, "tenant.id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cardinality cannot be null");
    }

    @Test
    void whenEmptyTenantKeyThenThrow() {
        assertThatThrownBy(
                () -> new ObservationTenantContextEventListener(ObservationTenantContextEventListener.Cardinality.HIGH,
                        ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdKey cannot be empty");
    }

    @Test
    void whenNullTenantKeyThenThrow() {
        assertThatThrownBy(
                () -> new ObservationTenantContextEventListener(ObservationTenantContextEventListener.Cardinality.HIGH,
                        null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdKey cannot be empty");
    }

    @Test
    void whenDefaultValueIsUsedAsKey() {
        var tenantId = "acme";
        var listener = new ObservationTenantContextEventListener();
        var observationContext = new Observation.Context();
        var event = new TenantContextAttachedEvent(tenantId, this);
        event.setObservationContext(observationContext);

        listener.onApplicationEvent(event);

        assertThat(observationContext
            .getHighCardinalityKeyValue(ObservationTenantContextEventListener.DEFAULT_TENANT_ID_KEY))
            .isEqualTo(KeyValue.of(ObservationTenantContextEventListener.DEFAULT_TENANT_ID_KEY, tenantId));
    }

    @Test
    void whenCustomValueIsUsedAsKey() {
        var tenantKey = "tenant.identifier";
        var tenantId = "acme";
        var listener = new ObservationTenantContextEventListener(
                ObservationTenantContextEventListener.DEFAULT_CARDINALITY, tenantKey);
        var observationContext = new Observation.Context();
        var event = new TenantContextAttachedEvent(tenantId, this);
        event.setObservationContext(observationContext);

        listener.onApplicationEvent(event);

        assertThat(observationContext.getHighCardinalityKeyValue(tenantKey))
            .isEqualTo(KeyValue.of(tenantKey, tenantId));
    }

    @Test
    void whenCustomCardinalityIsUsed() {
        var tenantId = "acme";
        var listener = new ObservationTenantContextEventListener(ObservationTenantContextEventListener.Cardinality.LOW,
                ObservationTenantContextEventListener.DEFAULT_TENANT_ID_KEY);
        var observationContext = new Observation.Context();
        var event = new TenantContextAttachedEvent(tenantId, this);
        event.setObservationContext(observationContext);

        listener.onApplicationEvent(event);

        assertThat(observationContext
            .getLowCardinalityKeyValue(ObservationTenantContextEventListener.DEFAULT_TENANT_ID_KEY))
            .isEqualTo(KeyValue.of(ObservationTenantContextEventListener.DEFAULT_TENANT_ID_KEY, tenantId));
    }

}
