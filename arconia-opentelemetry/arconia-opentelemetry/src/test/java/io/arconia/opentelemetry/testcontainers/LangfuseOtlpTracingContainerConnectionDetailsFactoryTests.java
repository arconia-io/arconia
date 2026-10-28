package io.arconia.opentelemetry.testcontainers;

import java.util.concurrent.TimeUnit;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.OpenTelemetryTracingExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Integration tests for {@link LangfuseOtlpTracingContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
class LangfuseOtlpTracingContainerConnectionDetailsFactoryTests extends LangfuseTestcontainers {

    @Autowired
    OtlpTracingConnectionDetails connectionDetails;

    @Autowired
    OtlpHttpSpanExporter spanExporter;

    @Test
    void shouldProvideConnectionDetailsForHttpProtobuf() {
        String url = connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF);
        assertThat(url).isEqualTo(langfuseContainer.getOtlpTracesUrl());
    }

    @Test
    void shouldNotProvideConnectionDetailsForGrpc() {
        assertThatIllegalStateException().isThrownBy(() -> connectionDetails.getTracesUrl(Protocol.GRPC));
    }

    @Test
    void shouldProvideAuthenticationHeaders() {
        assertThat(connectionDetails.getHeaders())
                .containsEntry("Authorization", langfuseContainer.getBasicAuthHeader())
                .containsEntry("x-langfuse-ingestion-version", "4");
        assertThat(spanExporter.toString()).contains("Authorization=OBFUSCATED", "x-langfuse-ingestion-version=OBFUSCATED");
    }

    @Test
    void shouldExportSpans() {
        InMemorySpanExporter inMemorySpanExporter = InMemorySpanExporter.create();
        try (SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(inMemorySpanExporter))
                .build()) {
            tracerProvider.get("langfuse-test").spanBuilder("test-span").startSpan().end();
        }

        CompletableResultCode result = spanExporter.export(inMemorySpanExporter.getFinishedSpanItems())
                .join(30, TimeUnit.SECONDS);
        assertThat(result.isSuccess()).as(String.valueOf(result.getFailureThrowable())).isTrue();
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({OpenTelemetryExporterAutoConfiguration.class, OpenTelemetryTracingExporterAutoConfiguration.class})
    static class TestConfiguration {}

}
