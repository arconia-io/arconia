package io.arconia.multitenancy.core.tenantdetails;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CachingTenantDetailsService}.
 */
class CachingTenantDetailsServiceTests {

    private final TenantDetailsService delegate = Mockito.mock(TenantDetailsService.class);

    private final CachingTenantDetailsService service = CachingTenantDetailsService.builder()
        .delegate(delegate)
        .timeToLive(Duration.ofMinutes(1))
        .build();

    @Test
    void whenNullDelegateThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> CachingTenantDetailsService.builder().timeToLive(Duration.ofMinutes(1)).build())
            .withMessageContaining("delegate cannot be null");
    }

    @Test
    void whenNullTimeToLiveThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> CachingTenantDetailsService.builder().delegate(delegate).build())
            .withMessageContaining("timeToLive cannot be null");
    }

    @Test
    void whenTimeToLiveNotPositiveThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> CachingTenantDetailsService.builder().delegate(delegate).timeToLive(Duration.ZERO).build())
            .withMessageContaining("timeToLive must be positive");
    }

    @Test
    void whenMaxSizeNotPositiveThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> CachingTenantDetailsService.builder()
                .delegate(delegate)
                .timeToLive(Duration.ofMinutes(1))
                .maxSize(0)
                .build())
            .withMessageContaining("maxSize must be greater than zero");
    }

    @Test
    void whenDefaultsThenMaxSizeIsOneThousand() {
        assertThat(service.getMaxSize()).isEqualTo(1000);
        assertThat(service.getTimeToLive()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void whenTenantFoundThenLoadedOnce() {
        var tenant = Tenant.builder().identifier("acme").build();
        when(delegate.loadTenantByIdentifier("acme")).thenReturn(tenant);

        assertThat(service.loadTenantByIdentifier("acme")).isSameAs(tenant);
        assertThat(service.loadTenantByIdentifier("acme")).isSameAs(tenant);

        verify(delegate, times(1)).loadTenantByIdentifier("acme");
    }

    @Test
    void whenTenantDisabledThenCachedToo() {
        var tenant = Tenant.builder().identifier("acme").enabled(false).build();
        when(delegate.loadTenantByIdentifier("acme")).thenReturn(tenant);

        service.loadTenantByIdentifier("acme");
        service.loadTenantByIdentifier("acme");

        verify(delegate, times(1)).loadTenantByIdentifier("acme");
    }

    @Test
    void whenTenantNotFoundThenNotCached() {
        when(delegate.loadTenantByIdentifier("unknown")).thenReturn(null);

        assertThat(service.loadTenantByIdentifier("unknown")).isNull();
        assertThat(service.loadTenantByIdentifier("unknown")).isNull();

        verify(delegate, times(2)).loadTenantByIdentifier("unknown");
    }

    @Test
    void whenLookupFailsThenNotCached() {
        var tenant = Tenant.builder().identifier("acme").build();
        when(delegate.loadTenantByIdentifier("acme")).thenThrow(new IllegalStateException("database down"))
            .thenReturn(tenant);

        assertThatIllegalStateException().isThrownBy(() -> service.loadTenantByIdentifier("acme"))
            .withMessage("database down");
        assertThat(service.loadTenantByIdentifier("acme")).isSameAs(tenant);
    }

    @Test
    void whenLoadAllTenantsThenAlwaysDelegated() {
        List<Tenant> tenants = List.of(Tenant.builder().identifier("acme").build());
        Mockito.<List<? extends TenantDetails>>when(delegate.loadAllTenants()).thenReturn(tenants);

        service.loadAllTenants();
        service.loadAllTenants();

        verify(delegate, times(2)).loadAllTenants();
    }

    @Test
    void whenEmptyIdentifierThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.loadTenantByIdentifier(""))
            .withMessageContaining("identifier cannot be null or empty");
    }

}
