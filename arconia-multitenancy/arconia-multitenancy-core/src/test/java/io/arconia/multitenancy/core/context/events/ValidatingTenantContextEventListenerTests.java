package io.arconia.multitenancy.core.context.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.arconia.multitenancy.core.exceptions.TenantResolutionException;
import io.arconia.multitenancy.core.tenantdetails.Tenant;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatingTenantContextEventListenerTests {

    @Mock
    TenantDetailsService tenantDetailsService;

    @InjectMocks
    ValidatingTenantContextEventListener validatingTenantContextEventListener;

    @Test
    void whenTenantExistsThenOk() {
        var tenantIdentifier = "acme";
        when(tenantDetailsService.loadTenantByIdentifier(anyString()))
            .thenReturn(Tenant.create().identifier(tenantIdentifier).build());

        validatingTenantContextEventListener.onApplicationEvent(new TenantContextAttachedEvent(tenantIdentifier, this));
    }

    @Test
    void whenTenantDoesNotExistThenThrow() {
        var tenantIdentifier = "acme";
        when(tenantDetailsService.loadTenantByIdentifier(anyString())).thenReturn(null);

        assertThatThrownBy(() -> validatingTenantContextEventListener
            .onApplicationEvent(new TenantContextAttachedEvent(tenantIdentifier, this)))
            .isInstanceOf(TenantResolutionException.class)
            .hasMessageContaining("The resolved tenant is invalid or disabled");
    }

    @Test
    void whenTenantDisabledThenThrow() {
        var tenantIdentifier = "acme";
        when(tenantDetailsService.loadTenantByIdentifier(anyString()))
            .thenReturn(Tenant.create().identifier(tenantIdentifier).enabled(false).build());

        assertThatThrownBy(() -> validatingTenantContextEventListener
            .onApplicationEvent(new TenantContextAttachedEvent(tenantIdentifier, this)))
            .isInstanceOf(TenantResolutionException.class)
            .hasMessageContaining("The resolved tenant is invalid or disabled");
    }

}
