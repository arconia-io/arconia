package io.arconia.dev.service.connections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.dev.service.connections.testcontainers.LgtmTestcontainers;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.otlp.OtlpTracingExporterConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LgtmOtlpTracingContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
@TestPropertySource(properties = "arconia.otel.traces.enabled=false")
class LgtmOtlpTracingContainerConnectionDetailsFactoryTests extends LgtmTestcontainers {

    @Autowired
    OtlpTracingConnectionDetails connectionDetails;

    @Test
    void shouldProvideConnectionDetailsForHttpProtobuf() {
        String url = connectionDetails.getUrl(Protocol.HTTP_PROTOBUF);
        assertThat(url).isEqualTo(lgtmContainer.getOtlpHttpUrl() + "/v1/traces");
    }

    @Test
    void shouldProvideConnectionDetailsForGrpc() {
        String url = connectionDetails.getUrl(Protocol.GRPC);
        assertThat(url).isEqualTo(lgtmContainer.getOtlpGrpcUrl());
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(OtlpTracingExporterConfiguration.class)
    static class TestConfiguration {}

}
