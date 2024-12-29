package io.arconia.multitenancy.core.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.arconia.multitenancy.core.context.events.TenantContextAttachedEvent;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultTenantEventPublisher}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultTenantEventPublisherTests {

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    DefaultTenantEventPublisher tenantEventPublisher;

    @Test
    void whenNullApplicationEventPublisherThenThrow() {
        assertThatThrownBy(() -> new DefaultTenantEventPublisher(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("applicationEventPublisher cannot be null");
    }

    @Test
    void whenTenantEventThenPublish() {
        var event = new TenantContextAttachedEvent("tenant", this);

        tenantEventPublisher.publishTenantEvent(event);

        Mockito.verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    void whenNullTenantEventThenThrow() {
        assertThatThrownBy(() -> tenantEventPublisher.publishTenantEvent(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantEvent cannot be null");
    }

}
