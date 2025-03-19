package io.arconia.dev.service.connections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.dev.service.connections.testcontainers.LgtmTestcontainers;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.otlp.OtlpMetricsExporterConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LgtmOtlpMetricsContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
@TestPropertySource(properties = "arconia.otel.metrics.exporter.type=none")
class LgtmOtlpMetricsContainerConnectionDetailsFactoryTests extends LgtmTestcontainers {

    @Autowired
    OtlpMetricsConnectionDetails connectionDetails;

    @Test
    void shouldProvideConnectionDetailsForHttpProtobuf() {
        String url = connectionDetails.getUrl(Protocol.HTTP_PROTOBUF);
        assertThat(url).isEqualTo(lgtmContainer.getOtlpHttpUrl() + "/v1/metrics");
    }

    @Test
    void shouldProvideConnectionDetailsForGrpc() {
        String url = connectionDetails.getUrl(Protocol.GRPC);
        assertThat(url).isEqualTo(lgtmContainer.getOtlpGrpcUrl());
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(OtlpMetricsExporterConfiguration.class)
    static class TestConfiguration {}

}
