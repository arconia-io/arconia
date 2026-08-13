package io.arconia.multitenancy.core.tenantdetails;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Tenant}.
 */
class TenantTests {

    @Test
    void whenIdentifierIsNullThenThrow() {
        assertThatThrownBy(() -> Tenant.builder().build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("identifier cannot be null or empty");
    }

    @Test
    void whenIdentifierIsEmptyThenThrow() {
        assertThatThrownBy(() -> Tenant.builder().identifier("").build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("identifier cannot be null or empty");
    }

    @Test
    void whenAttributesIsNullThenThrow() {
        assertThatThrownBy(() -> Tenant.builder().identifier("acme").attributes(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("attributes cannot be null");
    }

    @Test
    void whenBuiltThenCarriesIdentifierEnabledAndAttributes() {
        var tenant = Tenant.builder().identifier("acme").enabled(false).addAttribute("region", "eu").build();

        assertThat(tenant.identifier()).isEqualTo("acme");
        assertThat(tenant.enabled()).isFalse();
        assertThat(tenant.attributes()).containsExactly(Map.entry("region", "eu"));
    }

    @Test
    void whenEnabledNotSetThenDefaultsToTrue() {
        assertThat(Tenant.builder().identifier("acme").build().enabled()).isTrue();
    }

    @Test
    void whenAttributesGivenThenCallerMapIsNotAliased() {
        var attributes = new HashMap<String, Object>();
        attributes.put("region", "eu");

        var tenant = Tenant.builder().identifier("acme").attributes(attributes).addAttribute("tier", "gold").build();

        assertThat(attributes).containsOnlyKeys("region");
        assertThat(tenant.attributes()).containsOnlyKeys("region", "tier");
    }

    @Test
    void whenAttributesAreImmutableThenAddAttributeStillWorks() {
        assertThatNoException().isThrownBy(() -> Tenant.builder()
            .identifier("acme")
            .attributes(Map.of("region", "eu"))
            .addAttribute("tier", "gold")
            .build());
    }

    @Test
    void whenBuiltThenAttributesAreImmutable() {
        var tenant = Tenant.builder().identifier("acme").addAttribute("region", "eu").build();

        assertThatThrownBy(() -> tenant.attributes().put("tier", "gold"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

}
