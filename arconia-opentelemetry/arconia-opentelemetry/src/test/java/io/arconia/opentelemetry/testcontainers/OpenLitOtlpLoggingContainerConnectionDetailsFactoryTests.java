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
 * Integration tests for {@link OpenLitOtlpLoggingContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
@TestPropertySource(properties = "arconia.otel.logs.exporter.type=none")
class OpenLitOtlpLoggingContainerConnectionDetailsFactoryTests extends OpenLitTestcontainers {

    @Autowired
    OtlpLoggingConnectionDetails connectionDetails;

    @Test
    void shouldProvideConnectionDetailsForHttpProtobuf() {
        String url = connectionDetails.getUrl(Protocol.HTTP_PROTOBUF);
        String expectedUrl = "http://localhost:" + openLitContainer.getOtlpHttpPort() + OtlpLoggingConnectionDetails.LOGS_PATH;
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    void shouldProvideConnectionDetailsForGrpc() {
        String url = connectionDetails.getUrl(Protocol.GRPC);
        String expectedUrl = "http://localhost:" + openLitContainer.getOtlpGrpcPort();
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(OtlpLoggingExporterConfiguration.class)
    static class TestConfiguration {}

}
