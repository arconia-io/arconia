package io.arconia.multitenancy.core.autoconfigure;

import org.junit.jupiter.api.Test;

import io.arconia.multitenancy.core.context.resolvers.FixedTenantResolver;
import io.arconia.multitenancy.core.observability.Cardinality;
import io.arconia.multitenancy.core.observability.MdcTenantEventListener;
import io.arconia.multitenancy.core.observability.TenantObservationFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the shipped defaults of the core properties agree with the defaults applied
 * by the components they configure.
 * <p>
 * The properties hold literals rather than referencing the components' defaults, because
 * spring-boot-configuration-processor only reads default values from literal
 * initializers. These tests are what keeps the two copies in step.
 */
class MultitenancyCorePropertiesDefaultsTests {

    @Test
    void fixedTenantIdentifierDefaultMatchesResolverDefault() {
        assertThat(new FixedTenantResolutionProperties().getTenantIdentifier())
            .isEqualTo(FixedTenantResolver.builder().build().resolveTenantIdentifier(this));
    }

    @Test
    void fixedTenantResolutionIsDisabledByDefault() {
        assertThat(new FixedTenantResolutionProperties().isEnabled()).isFalse();
    }

    @Test
    void observationKeyNameDefaultMatchesObservationFilterDefault() {
        assertThat(new TenantObservationProperties().getKeyName())
            .isEqualTo(TenantObservationFilter.builder().build().getTenantIdentifierKey());
    }

    @Test
    void observationCardinalityDefaultMatchesObservationFilterDefault() {
        assertThat(new TenantObservationProperties().getCardinality())
            .isEqualTo(TenantObservationFilter.builder().build().getCardinality());
    }

    @Test
    void observationsAreEnabledWithHighCardinalityByDefault() {
        var properties = new TenantObservationProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getCardinality()).isEqualTo(Cardinality.HIGH);
    }

    @Test
    void mdcKeyNameDefaultMatchesListenerDefault() {
        assertThat(new TenantLoggingProperties().getMdc().getKeyName())
            .isEqualTo(MdcTenantEventListener.builder().build().getTenantIdentifierKey());
    }

    @Test
    void mdcIsEnabledByDefault() {
        assertThat(new TenantLoggingProperties().getMdc().isEnabled()).isTrue();
    }

    @Test
    void noTenantsAreConfiguredByDefault() {
        assertThat(new TenantDetailsProperties().getTenants()).isEmpty();
    }

}
