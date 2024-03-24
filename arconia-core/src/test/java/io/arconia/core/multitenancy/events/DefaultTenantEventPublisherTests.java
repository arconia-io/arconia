package io.arconia.core.multitenancy.events;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import io.arconia.core.multitenancy.context.events.TenantContextAttachedEvent;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultTenantEventPublisher}.
 */
class DefaultTenantEventPublisherTests {

    @Test
    void whenNullApplicationEventPublisherThenThrow() {
        assertThatThrownBy(() -> new DefaultTenantEventPublisher(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("applicationEventPublisher cannot be null");
    }

    @Test
    void whenTenantEventThenPublish() {
        var applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        var publisher = new DefaultTenantEventPublisher(applicationEventPublisher);
        var event = new TenantContextAttachedEvent("tenant", this);

        publisher.publishTenantEvent(event);

        Mockito.verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    void whenNullTenantEventThenThrow() {
        var applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        var publisher = new DefaultTenantEventPublisher(applicationEventPublisher);

        assertThatThrownBy(() -> publisher.publishTenantEvent(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantEvent cannot be null");
    }

}
