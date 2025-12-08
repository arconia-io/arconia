package io.arconia.docling.actuate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.health.HealthCheckResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DoclingServeHealthIndicator}.
 */
@ExtendWith(MockitoExtension.class)
class DoclingServeHealthIndicatorTests {

    @Mock
    private DoclingServeApi doclingServeApi;

    @Test
    void whenDoclingServeApiIsNullThenThrow() {
        assertThatThrownBy(() -> new DoclingServeHealthIndicator(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("doclingServeApi cannot be null");
    }

    @Test
    void whenBuilderIsNullThenThrow() {
        DoclingServeHealthIndicator healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);

        assertThatThrownBy(() -> healthIndicator.doHealthCheck(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("builder cannot be null");
    }

    @Test
    void whenHealthCheckReturnsOkStatusThenHealthIsUp() {
        HealthCheckResponse healthCheckResponse = HealthCheckResponse.builder().status("ok").build();
        when(doclingServeApi.health()).thenReturn(healthCheckResponse);

        DoclingServeHealthIndicator healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isEmpty();
    }

    @Test
    void whenHealthCheckReturnsNonOkStatusThenHealthIsDown() {
        HealthCheckResponse healthCheckResponse = HealthCheckResponse.builder().status("unhealthy").build();
        when(doclingServeApi.health()).thenReturn(healthCheckResponse);

        DoclingServeHealthIndicator healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "unhealthy");
    }

    @Test
    void whenHealthCheckReturnsNullStatusThenHealthIsDown() {
        HealthCheckResponse healthCheckResponse = HealthCheckResponse.builder().status(null).build();
        when(doclingServeApi.health()).thenReturn(healthCheckResponse);

        DoclingServeHealthIndicator healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "unknown");
    }

    @Test
    void whenHealthCheckReturnsEmptyStatusThenHealthIsDown() {
        HealthCheckResponse healthCheckResponse = HealthCheckResponse.builder().status("").build();
        when(doclingServeApi.health()).thenReturn(healthCheckResponse);

        DoclingServeHealthIndicator healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "");
    }

    @Test
    void whenHealthCheckThrowsExceptionThenHealthIsDown() {
        RuntimeException exception = new RuntimeException("Health check failed");
        when(doclingServeApi.health()).thenThrow(exception);

        DoclingServeHealthIndicator healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "java.lang.RuntimeException: Health check failed");
    }

}
