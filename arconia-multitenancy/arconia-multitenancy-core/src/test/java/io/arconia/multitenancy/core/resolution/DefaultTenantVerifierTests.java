package io.arconia.multitenancy.core.resolution;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.arconia.multitenancy.core.tenantdetails.Tenant;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;


import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultTenantVerifier}.
 */
class DefaultTenantVerifierTests {

    @Test
    void whenNullTenantDetailsServiceThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DefaultTenantVerifier(null))
            .withMessageContaining("tenantDetailsService cannot be null");
    }

    @Test
    void whenNullTenantIdentifierThenThrow() {
        var service = Mockito.mock(TenantDetailsService.class);
        var verifier = new DefaultTenantVerifier(service);

        assertThatIllegalArgumentException().isThrownBy(() -> verifier.verify(null))
            .withMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenEmptyTenantIdentifierThenThrow() {
        var service = Mockito.mock(TenantDetailsService.class);
        var verifier = new DefaultTenantVerifier(service);

        assertThatIllegalArgumentException().isThrownBy(() -> verifier.verify(""))
            .withMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenTenantIdentifierContainsAlphanumericDashUnderscoreThenPass() {
        var tenant = Tenant.builder().identifier("acme-corp_2").enabled(true).build();
        var service = Mockito.mock(TenantDetailsService.class);
        when(service.loadTenantByIdentifier("acme-corp_2")).thenReturn(tenant);
        var verifier = new DefaultTenantVerifier(service);

        assertThatNoException().isThrownBy(() -> verifier.verify("acme-corp_2"));
    }

    @Test
    void whenTenantExistsAndEnabledThenPass() {
        var tenant = Tenant.builder().identifier("acme").enabled(true).build();
        var service = Mockito.mock(TenantDetailsService.class);
        when(service.loadTenantByIdentifier("acme")).thenReturn(tenant);
        var verifier = new DefaultTenantVerifier(service);

        assertThatNoException().isThrownBy(() -> verifier.verify("acme"));
    }

    @Test
    void whenTenantExistsButDisabledThenThrow() {
        var tenant = Tenant.builder().identifier("acme").enabled(false).build();
        var service = Mockito.mock(TenantDetailsService.class);
        when(service.loadTenantByIdentifier("acme")).thenReturn(tenant);
        var verifier = new DefaultTenantVerifier(service);

        assertThatThrownBy(() -> verifier.verify("acme")).isInstanceOf(TenantVerificationException.class)
            .hasMessageContaining("The resolved tenant is invalid or disabled");
    }

    @Test
    void whenTenantNotFoundThenThrow() {
        var service = Mockito.mock(TenantDetailsService.class);
        when(service.loadTenantByIdentifier("unknown")).thenReturn(null);
        var verifier = new DefaultTenantVerifier(service);

        assertThatThrownBy(() -> verifier.verify("unknown")).isInstanceOf(TenantVerificationException.class)
            .hasMessageContaining("The resolved tenant is invalid or disabled");
    }

}
