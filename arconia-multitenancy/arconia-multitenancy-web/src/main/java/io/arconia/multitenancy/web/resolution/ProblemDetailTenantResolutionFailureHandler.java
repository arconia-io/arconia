package io.arconia.multitenancy.web.resolution;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.util.Assert;

import tools.jackson.databind.json.JsonMapper;

import io.arconia.core.support.Incubating;

/**
 * Default {@link TenantResolutionFailureHandler} that writes an RFC 9457
 * {@link ProblemDetail} response. A missing, invalid, or rejected tenant is the caller's
 * fault and gets a {@code 400 Bad Request}; a registry outage is not, and gets a
 * {@code 503 Service Unavailable}.
 * <p>
 * The body is serialized with a dedicated {@link JsonMapper} carrying the same
 * {@link ProblemDetailJacksonMixin} Spring MVC applies, so that it has the shape of a
 * problem detail rendered by a controller while a customised application mapper cannot
 * break the error path.
 */
@Incubating
public final class ProblemDetailTenantResolutionFailureHandler implements TenantResolutionFailureHandler {

    private final JsonMapper jsonMapper = JsonMapper.builder()
        .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
        .build();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, TenantResolutionFailure failure) throws IOException {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");
        Assert.notNull(failure, "failure cannot be null");
        var status = statusFor(failure.outcome());
        var problemDetail = ProblemDetail.forStatusAndDetail(status, failure.message());
        problemDetail.setInstance(instanceFor(request));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(status.value());
        response.getWriter().write(jsonMapper.writeValueAsString(problemDetail));
    }

    private static HttpStatus statusFor(TenantResolutionFailure.Outcome outcome) {
        return switch (outcome) {
            case MISSING, INVALID, REJECTED -> HttpStatus.BAD_REQUEST;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }

    /**
     * The request URI is client input, so a value that is not a valid URI leaves the
     * instance unset rather than failing the error response.
     */
    @Nullable
    private static URI instanceFor(HttpServletRequest request) {
        try {
            return URI.create(request.getRequestURI());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

}
