package io.arconia.openinference.observation.testcontainers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingExporterConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenInferenceOtlpTracingContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
class OpenInferenceOtlpTracingContainerConnectionDetailsFactoryTests extends PhoenixTestcontainers {

    @Autowired
    OtlpTracingConnectionDetails connectionDetails;

    @Test
    void shouldProvideConnectionDetailsForHttpProtobuf() {
        String url = connectionDetails.getUrl(Protocol.HTTP_PROTOBUF);
        String expectedUrl = "http://localhost:" + phoenixContainer.getMappedPort(HTTP_PORT) + OtlpTracingConnectionDetails.TRACES_PATH;
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    void shouldProvideConnectionDetailsForGrpc() {
        String url = connectionDetails.getUrl(Protocol.GRPC);
        String expectedUrl = "http://localhost:" + phoenixContainer.getMappedPort(OtlpTracingConnectionDetails.DEFAULT_GRPC_PORT);
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({OtlpTracingExporterConfiguration.class})
    static class TestConfiguration {}

}
