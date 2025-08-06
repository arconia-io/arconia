package io.arconia.docling.client.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HealthCheckResponse}.
 */
class HealthCheckResponseTests {

    @Test
    void whenValidParametersThenCreateHealthCheckResponse() {
        String status = "healthy";

        HealthCheckResponse response = new HealthCheckResponse(status);

        assertThat(response.status()).isEqualTo(status);
    }

    @Test
    void whenStatusIsNullThenCreateHealthCheckResponse() {
        HealthCheckResponse response = new HealthCheckResponse(null);

        assertThat(response.status()).isNull();
    }

    @Test
    void whenEmptyStatusThenCreateHealthCheckResponse() {
        String status = "";

        HealthCheckResponse response = new HealthCheckResponse(status);

        assertThat(response.status()).isEqualTo(status);
    }

}
