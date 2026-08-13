package io.arconia.multitenancy.web.context.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.util.WebUtils;

import io.arconia.multitenancy.core.context.events.TenantContextAttachedEvent;
import io.arconia.multitenancy.core.context.events.TenantContextClosedEvent;
import io.arconia.multitenancy.core.exceptions.TenantVerificationException;
import io.arconia.multitenancy.core.observability.Cardinality;
import io.arconia.multitenancy.core.observability.TenantObservationFilter;
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
        assertThatThrownBy(() -> TenantContextFilter.builder()
            .tenantContextIgnorePathMatcher(noTenantPathMatcher)
            .eventPublisher(eventPublisher)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("httpRequestTenantResolver cannot be null");
    }

    @Test
    void whenNullPathMatcherThenThrow() {
        var httpRequestTenantResolver = Mockito.mock(HttpRequestTenantResolver.class);
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        assertThatThrownBy(() -> TenantContextFilter.builder()
            .httpRequestTenantResolver(httpRequestTenantResolver)
            .eventPublisher(eventPublisher)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantContextIgnorePathMatcher cannot be null");
    }

    @Test
    void whenNullEventPublisherThenThrow() {
        var httpRequestTenantResolver = Mockito.mock(HttpRequestTenantResolver.class);
        var noTenantPathMatcher = Mockito.mock(TenantContextIgnorePathMatcher.class);
        assertThatThrownBy(() -> TenantContextFilter.builder()
            .httpRequestTenantResolver(httpRequestTenantResolver)
            .tenantContextIgnorePathMatcher(noTenantPathMatcher)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("eventPublisher cannot be null");
    }

    @Test
    void whenTenantResolvedThenPublishEvents() throws ServletException, IOException {
        var tenantIdentifier = "acme";
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", tenantIdentifier);
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        var filter = TenantContextFilter.builder()
            .httpRequestTenantResolver(HeaderTenantResolver.builder().build())
            .tenantContextIgnorePathMatcher(new TenantContextIgnorePathMatcher(Set.of()))
            .eventPublisher(eventPublisher)
            .build();

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
        request.addHeader("X-TenantId", tenantIdentifier);
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        TenantVerifier tenantVerifier = id -> {
            throw new TenantVerificationException("The resolved tenant is invalid or disabled");
        };
        var filter = TenantContextFilter.builder()
            .httpRequestTenantResolver(HeaderTenantResolver.builder().build())
            .tenantContextIgnorePathMatcher(new TenantContextIgnorePathMatcher(Set.of()))
            .eventPublisher(eventPublisher)
            .tenantVerifier(tenantVerifier)
            .build();

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
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        var filter = TenantContextFilter.builder()
            .httpRequestTenantResolver(HeaderTenantResolver.builder().build())
            .tenantContextIgnorePathMatcher(new TenantContextIgnorePathMatcher(Set.of()))
            .eventPublisher(eventPublisher)
            .build();

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
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        var filter = TenantContextFilter.builder()
            .httpRequestTenantResolver(HeaderTenantResolver.builder().build())
            .tenantContextIgnorePathMatcher(new TenantContextIgnorePathMatcher(Set.of(path)))
            .eventPublisher(eventPublisher)
            .build();

        filter.doFilter(request, response, filterChain);

        Mockito.verify(eventPublisher, Mockito.times(0)).publishEvent(Mockito.any(ApplicationEvent.class));
    }

    @Test
    void whenTenantResolvedAndObservationFilterPresentThenEnrichHttpObservation() throws ServletException, IOException {
        var tenantIdentifier = "acme";
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", tenantIdentifier);
        var observationContext = new ServerRequestObservationContext(request, new MockHttpServletResponse());
        request.setAttribute(ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observationContext);
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        var filter = TenantContextFilter.builder()
            .httpRequestTenantResolver(HeaderTenantResolver.builder().build())
            .tenantContextIgnorePathMatcher(new TenantContextIgnorePathMatcher(Set.of()))
            .eventPublisher(eventPublisher)
            .tenantObservationFilter(
                    TenantObservationFilter.builder().tenantIdentifierKey("tenant.id").cardinality(Cardinality.HIGH).build())
            .build();

        filter.doFilter(request, response, filterChain);

        assertThat(observationContext.getHighCardinalityKeyValues())
            .anyMatch(kv -> kv.getKey().equals("tenant.id") && kv.getValue().equals(tenantIdentifier));
    }

    @Test
    void whenTenantIdentifierIsInvalidThenReturnBadRequest() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", "acme\nmalicious");
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        var filter = defaultFilter(eventPublisher).build();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString())
            .contains("The tenant identifier must contain only alphanumeric characters");
        Mockito.verify(eventPublisher, Mockito.times(0)).publishEvent(Mockito.any(ApplicationEvent.class));
    }

    @Test
    void whenNoTenantVerifierThenIdentifierIsStillValidated() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", "../../etc/passwd");
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var filter = defaultFilter(Mockito.mock(ApplicationEventPublisher.class)).build();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void whenCustomTenantIdentifierValidatorThenApplied() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", "acme");
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var filter = defaultFilter(Mockito.mock(ApplicationEventPublisher.class))
            .tenantIdentifierValidator(tenantIdentifier -> {
                throw new TenantVerificationException("Tenants must be onboarded first");
            })
            .build();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Tenants must be onboarded first");
    }

    @Test
    void whenTenantLookupFailsThenReturnServiceUnavailable() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", "acme");
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        TenantVerifier tenantVerifier = id -> {
            throw new IllegalStateException("Connection pool exhausted");
        };
        var filter = defaultFilter(eventPublisher).tenantVerifier(tenantVerifier).build();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getContentAsString()).contains("the tenant registry is currently unavailable");
        Mockito.verify(eventPublisher, Mockito.times(0)).publishEvent(Mockito.any(ApplicationEvent.class));
    }

    @Test
    void whenErrorResponseThenProblemJsonWithUtf8() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var filter = defaultFilter(Mockito.mock(ApplicationEventPublisher.class)).build();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase(StandardCharsets.UTF_8.name());
    }

    @Test
    void whenAttachedListenerThrowsThenClosedEventIsStillPublished() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", "acme");
        var response = new MockHttpServletResponse();
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        Mockito.doThrow(new IllegalStateException("listener failed"))
            .when(eventPublisher)
            .publishEvent(Mockito.any(TenantContextAttachedEvent.class));
        var filter = defaultFilter(eventPublisher).build();

        assertThatThrownBy(() -> filter.doFilter(request, response, new MockFilterChain()))
            .isInstanceOf(IllegalStateException.class);

        Mockito.verify(eventPublisher).publishEvent(Mockito.any(TenantContextClosedEvent.class));
    }

    @Test
    void whenRuntimeExceptionFromChainThenNotWrappedInServletException() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", "acme");
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                throw new IllegalArgumentException("boom");
            }
        };
        var filter = defaultFilter(Mockito.mock(ApplicationEventPublisher.class)).build();

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("boom");
    }

    @Test
    void whenErrorDispatchAndTenantMissingThenChainProceedsWithoutSecondResponse()
            throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        request.setAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE, "/failing");
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();
        var filter = defaultFilter(Mockito.mock(ApplicationEventPublisher.class)).build();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    void whenErrorDispatchAndTenantResolvedThenContextIsBound() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", "acme");
        request.setAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE, "/failing");
        var response = new MockHttpServletResponse();
        var eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        var filter = defaultFilter(eventPublisher).build();

        filter.doFilter(request, response, new MockFilterChain());

        Mockito.verify(eventPublisher).publishEvent(Mockito.any(TenantContextAttachedEvent.class));
    }

    private TenantContextFilter.Builder defaultFilter(ApplicationEventPublisher eventPublisher) {
        return TenantContextFilter.builder()
            .httpRequestTenantResolver(HeaderTenantResolver.builder().build())
            .tenantContextIgnorePathMatcher(new TenantContextIgnorePathMatcher(Set.of()))
            .eventPublisher(eventPublisher);
    }

}
