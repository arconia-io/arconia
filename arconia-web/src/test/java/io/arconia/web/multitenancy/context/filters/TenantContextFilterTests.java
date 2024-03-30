package io.arconia.web.multitenancy.context.filters;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ServerHttpObservationFilter;

import io.arconia.core.multitenancy.context.events.TenantContextAttachedEvent;
import io.arconia.core.multitenancy.context.events.TenantContextClosedEvent;
import io.arconia.core.multitenancy.events.TenantEvent;
import io.arconia.core.multitenancy.events.TenantEventPublisher;
import io.arconia.web.multitenancy.context.resolvers.HeaderTenantResolver;
import io.arconia.web.multitenancy.context.resolvers.HttpRequestTenantResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantContextFilter}.
 */
class TenantContextFilterTests {

    @Test
    void whenNullTenantResolverThenThrow() {
        var noTenantPathMatcher = Mockito.mock(TenantContextIgnorePathMatcher.class);
        var tenantEventPublisher = Mockito.mock(TenantEventPublisher.class);
        assertThatThrownBy(() -> new TenantContextFilter(null, noTenantPathMatcher, tenantEventPublisher))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("httpRequestTenantResolver cannot be null");
    }

    @Test
    void whenNullPathMatcherThenThrow() {
        var httpRequestTenantResolver = Mockito.mock(HttpRequestTenantResolver.class);
        var tenantEventPublisher = Mockito.mock(TenantEventPublisher.class);
        assertThatThrownBy(() -> new TenantContextFilter(httpRequestTenantResolver, null, tenantEventPublisher))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ignorePathMatcher cannot be null");
    }

    @Test
    void whenNullEventPublisherThenThrow() {
        var httpRequestTenantResolver = Mockito.mock(HttpRequestTenantResolver.class);
        var noTenantPathMatcher = Mockito.mock(TenantContextIgnorePathMatcher.class);
        assertThatThrownBy(() -> new TenantContextFilter(httpRequestTenantResolver, noTenantPathMatcher, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantEventPublisher cannot be null");
    }

    @Test
    void whenTenantResolvedThenPublishEvent() throws ServletException, IOException {
        var tenantIdentifier = "acme";
        var observationContext = Mockito.mock(ServerRequestObservationContext.class);
        var request = new MockHttpServletRequest();
        request.addHeader(HeaderTenantResolver.DEFAULT_HEADER_NAME, tenantIdentifier);
        request.setAttribute(ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observationContext);
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var httpRequestTenantResolver = new HeaderTenantResolver();
        var noTenantPathMatcher = new TenantContextIgnorePathMatcher(Set.of());
        var tenantEventPublisher = Mockito.mock(TenantEventPublisher.class);
        var filter = new TenantContextFilter(httpRequestTenantResolver, noTenantPathMatcher, tenantEventPublisher);

        filter.doFilter(request, response, filterChain);

        var tenantEventArgumentCaptor = ArgumentCaptor.forClass(TenantEvent.class);
        Mockito.verify(tenantEventPublisher, Mockito.times(2)).publishTenantEvent(tenantEventArgumentCaptor.capture());

        assertThat(tenantEventArgumentCaptor.getAllValues().get(0))
            .isExactlyInstanceOf(TenantContextAttachedEvent.class)
            .extracting(event -> (TenantContextAttachedEvent) event)
            .matches(event -> event.getTenantIdentifier().equals(tenantIdentifier))
            .matches(event -> event.getSource().equals(request))
            .matches(event -> event.getObservationContext() != null);

        assertThat(tenantEventArgumentCaptor.getAllValues().get(tenantEventArgumentCaptor.getAllValues().size() - 1))
            .isExactlyInstanceOf(TenantContextClosedEvent.class)
            .extracting(event -> (TenantContextClosedEvent) event)
            .matches(event -> event.getTenantIdentifier().equals(tenantIdentifier))
            .matches(event -> event.getSource().equals(request));
    }

    @Test
    void whenRequiredTenantNotResolvedThenThrow() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var httpRequestTenantResolver = new HeaderTenantResolver();
        var noTenantPathMatcher = new TenantContextIgnorePathMatcher(Set.of());
        var tenantEventPublisher = Mockito.mock(TenantEventPublisher.class);
        var filter = new TenantContextFilter(httpRequestTenantResolver, noTenantPathMatcher, tenantEventPublisher);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("A tenant identifier must be specified for HTTP requests");

        Mockito.verify(tenantEventPublisher, Mockito.times(0)).publishTenantEvent(Mockito.any(TenantEvent.class));
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
        var tenantEventPublisher = Mockito.mock(TenantEventPublisher.class);
        var filter = new TenantContextFilter(httpRequestTenantResolver, noTenantPathMatcher, tenantEventPublisher);

        filter.doFilter(request, response, filterChain);

        Mockito.verify(tenantEventPublisher, Mockito.times(0)).publishTenantEvent(Mockito.any(TenantEvent.class));
    }

}
