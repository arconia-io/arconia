package io.arconia.docling.actuate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import io.arconia.docling.client.DoclingClient;
import io.arconia.docling.client.health.HealthCheckResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DoclingHealthIndicator}.
 */
@ExtendWith(MockitoExtension.class)
class DoclingHealthIndicatorTests {

    @Mock
    private DoclingClient doclingClient;

    @Test
    void whenDoclingClientIsNullThenThrow() {
        assertThatThrownBy(() -> new DoclingHealthIndicator(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("doclingClient cannot be null");
    }

    @Test
    void whenBuilderIsNullThenThrow() {
        DoclingHealthIndicator healthIndicator = new DoclingHealthIndicator(doclingClient);

        assertThatThrownBy(() -> healthIndicator.doHealthCheck(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("builder cannot be null");
    }

    @Test
    void whenHealthCheckReturnsOkStatusThenHealthIsUp() {
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse("ok");
        when(doclingClient.health()).thenReturn(healthCheckResponse);

        DoclingHealthIndicator healthIndicator = new DoclingHealthIndicator(doclingClient);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isEmpty();
    }

    @Test
    void whenHealthCheckReturnsNonOkStatusThenHealthIsDown() {
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse("unhealthy");
        when(doclingClient.health()).thenReturn(healthCheckResponse);

        DoclingHealthIndicator healthIndicator = new DoclingHealthIndicator(doclingClient);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "unhealthy");
    }

    @Test
    void whenHealthCheckReturnsNullStatusThenHealthIsDown() {
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(null);
        when(doclingClient.health()).thenReturn(healthCheckResponse);

        DoclingHealthIndicator healthIndicator = new DoclingHealthIndicator(doclingClient);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "unknown");
    }

    @Test
    void whenHealthCheckReturnsEmptyStatusThenHealthIsDown() {
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse("");
        when(doclingClient.health()).thenReturn(healthCheckResponse);

        DoclingHealthIndicator healthIndicator = new DoclingHealthIndicator(doclingClient);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "");
    }

    @Test
    void whenHealthCheckThrowsExceptionThenHealthIsDown() {
        RuntimeException exception = new RuntimeException("Health check failed");
        when(doclingClient.health()).thenThrow(exception);

        DoclingHealthIndicator healthIndicator = new DoclingHealthIndicator(doclingClient);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "java.lang.RuntimeException: Health check failed");
    }

}
