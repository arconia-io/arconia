package io.arconia.docling.actuate;

import java.net.URI;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.DoclingServeApiConfig;
import ai.docling.serve.api.health.HealthCheckResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DoclingServeHealthIndicator}.
 */
@ExtendWith(MockitoExtension.class)
class DoclingServeHealthIndicatorTests {

    private static final URI BASE_URL = URI.create("http://docling.example.net:5001");

    private static final DoclingServeApiConfig CONFIG = DoclingServeApi.builder()
            .baseUrl(BASE_URL)
            .config();

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
        when(doclingServeApi.config()).thenReturn(CONFIG);
        when(doclingServeApi.health()).thenReturn(healthCheckResponse);

        DoclingServeHealthIndicator healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsOnly(entry("location", BASE_URL.toString()));
    }

    @Test
    void whenHealthCheckReturnsNonOkStatusThenHealthIsDown() {
        HealthCheckResponse healthCheckResponse = HealthCheckResponse.builder().status("unhealthy").build();
        when(doclingServeApi.config()).thenReturn(CONFIG);
        when(doclingServeApi.health()).thenReturn(healthCheckResponse);

        DoclingServeHealthIndicator healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("location", BASE_URL.toString())
                .containsEntry("status", "unhealthy");
    }

    @Test
    void whenHealthCheckReturnsNullStatusThenHealthIsDown() {
        HealthCheckResponse healthCheckResponse = HealthCheckResponse.builder().status(null).build();
        when(doclingServeApi.config()).thenReturn(CONFIG);
        when(doclingServeApi.health()).thenReturn(healthCheckResponse);

        DoclingServeHealthIndicator healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("location", BASE_URL.toString())
                .containsEntry("status", "unknown");
    }

    @Test
    void whenHealthCheckReturnsEmptyStatusThenHealthIsDown() {
        HealthCheckResponse healthCheckResponse = HealthCheckResponse.builder().status("").build();
        when(doclingServeApi.config()).thenReturn(CONFIG);
        when(doclingServeApi.health()).thenReturn(healthCheckResponse);

        DoclingServeHealthIndicator healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("location", BASE_URL.toString())
                .containsEntry("status", "");
    }

    @Test
    void whenHealthCheckThrowsExceptionThenHealthIsDown() {
        RuntimeException exception = new RuntimeException("Health check failed");
        when(doclingServeApi.config()).thenReturn(CONFIG);
        when(doclingServeApi.health()).thenThrow(exception);

        DoclingServeHealthIndicator healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("location", BASE_URL.toString())
                .containsEntry("error", "java.lang.RuntimeException: Health check failed");
    }

}
