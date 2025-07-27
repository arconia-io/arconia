package io.arconia.opentelemetry.testcontainers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingExporterConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LgtmOpenTelemetryLoggingContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
@TestPropertySource(properties = "arconia.otel.logs.exporter.type=none")
class LgtmOpenTelemetryLoggingContainerConnectionDetailsFactoryTests extends LgtmTestcontainers {

    @Autowired
    OtlpLoggingConnectionDetails connectionDetails;

    @Test
    void shouldProvideConnectionDetailsForHttpProtobuf() {
        String url = connectionDetails.getUrl(Protocol.HTTP_PROTOBUF);
        assertThat(url).isEqualTo(lgtmContainer.getOtlpHttpUrl() + "/v1/logs");
    }

    @Test
    void shouldProvideConnectionDetailsForGrpc() {
        String url = connectionDetails.getUrl(Protocol.GRPC);
        assertThat(url).isEqualTo(lgtmContainer.getOtlpGrpcUrl());
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(OtlpLoggingExporterConfiguration.class)
    static class TestConfiguration {}

}
