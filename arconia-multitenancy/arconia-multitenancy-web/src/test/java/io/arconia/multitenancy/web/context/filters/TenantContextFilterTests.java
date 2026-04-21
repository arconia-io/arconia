package io.arconia.multitenancy.web.context.filters;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.arconia.multitenancy.core.context.events.TenantContextAttachedEvent;
import io.arconia.multitenancy.core.context.events.TenantContextClosedEvent;
import io.arconia.multitenancy.core.exceptions.TenantVerificationException;
import io.arconia.multitenancy.core.tenantdetails.TenantVerifier;
import io.arconia.multitenancy.web.context.resolvers.HeaderTenantResolver;
import io.arconia.multitenancy.web.context.resolvers.HttpRequestTenantResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantContextFilter}.
 */
class TenantContextFilterTests {

    @Test
    void whenNullTenantResolverThenThrow() {
        var noTenantPathMatcher = Mockito.mock(TenantContextIgnorePathMatcher.class);
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        assertThatThrownBy(() -> new TenantContextFilter(null, noTenantPathMatcher, eventPublisher, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("httpRequestTenantResolver cannot be null");
    }

    @Test
    void whenNullPathMatcherThenThrow() {
        var httpRequestTenantResolver = Mockito.mock(HttpRequestTenantResolver.class);
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        assertThatThrownBy(() -> new TenantContextFilter(httpRequestTenantResolver, null, eventPublisher, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ignorePathMatcher cannot be null");
    }

    @Test
    void whenNullEventPublisherThenThrow() {
        var httpRequestTenantResolver = Mockito.mock(HttpRequestTenantResolver.class);
        var noTenantPathMatcher = Mockito.mock(TenantContextIgnorePathMatcher.class);
        assertThatThrownBy(() -> new TenantContextFilter(httpRequestTenantResolver, noTenantPathMatcher, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("eventPublisher cannot be null");
    }

    @Test
    void whenTenantResolvedThenPublishEvents() throws ServletException, IOException {
        var tenantIdentifier = "acme";
        var request = new MockHttpServletRequest();
        request.addHeader(HeaderTenantResolver.DEFAULT_HEADER_NAME, tenantIdentifier);
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var httpRequestTenantResolver = new HeaderTenantResolver();
        var noTenantPathMatcher = new TenantContextIgnorePathMatcher(Set.of());
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        var filter = new TenantContextFilter(httpRequestTenantResolver, noTenantPathMatcher, eventPublisher, null);

        filter.doFilter(request, response, filterChain);

        var eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        Mockito.verify(eventPublisher, Mockito.times(2)).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getAllValues().get(0))
            .isExactlyInstanceOf(TenantContextAttachedEvent.class)
            .extracting(event -> (TenantContextAttachedEvent) event)
            .matches(event -> event.getTenantIdentifier().equals(tenantIdentifier))
            .matches(event -> event.getSource().equals(request));

        assertThat(eventCaptor.getAllValues().get(1))
            .isExactlyInstanceOf(TenantContextClosedEvent.class)
            .extracting(event -> (TenantContextClosedEvent) event)
            .matches(event -> event.getTenantIdentifier().equals(tenantIdentifier))
            .matches(event -> event.getSource().equals(request));
    }

    @Test
    void whenTenantVerifierRejectsThenReturnBadRequest() throws ServletException, IOException {
        var tenantIdentifier = "invalid-tenant";
        var request = new MockHttpServletRequest();
        request.addHeader(HeaderTenantResolver.DEFAULT_HEADER_NAME, tenantIdentifier);
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var httpRequestTenantResolver = new HeaderTenantResolver();
        var noTenantPathMatcher = new TenantContextIgnorePathMatcher(Set.of());
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        TenantVerifier tenantVerifier = id -> {
            throw new TenantVerificationException("The resolved tenant is invalid or disabled");
        };
        var filter = new TenantContextFilter(httpRequestTenantResolver, noTenantPathMatcher, eventPublisher,
                tenantVerifier);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("The resolved tenant is invalid or disabled");

        Mockito.verify(eventPublisher, Mockito.times(0)).publishEvent(Mockito.any(ApplicationEvent.class));
    }

    @Test
    void whenRequiredTenantNotResolvedThenReturnBadRequest() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var httpRequestTenantResolver = new HeaderTenantResolver();
        var noTenantPathMatcher = new TenantContextIgnorePathMatcher(Set.of());
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        var filter = new TenantContextFilter(httpRequestTenantResolver, noTenantPathMatcher, eventPublisher, null);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("A tenant identifier must be specified for HTTP requests");

        Mockito.verify(eventPublisher, Mockito.times(0)).publishEvent(Mockito.any(ApplicationEvent.class));
    }

    @Test
    void whenIgnorePathThenNoTenantResolvedAndNoEventPublished() throws ServletException, IOException {
        var path = "/ignore-path";
        var request = new MockHttpServletRequest();
        request.setRequestURI(path);
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var httpRequestTenantResolver = new HeaderTenantResolver();
        var noTenantPathMatcher = new TenantContextIgnorePathMatcher(Set.of(path));
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        var filter = new TenantContextFilter(httpRequestTenantResolver, noTenantPathMatcher, eventPublisher, null);

        filter.doFilter(request, response, filterChain);

        Mockito.verify(eventPublisher, Mockito.times(0)).publishEvent(Mockito.any(ApplicationEvent.class));
    }

}
