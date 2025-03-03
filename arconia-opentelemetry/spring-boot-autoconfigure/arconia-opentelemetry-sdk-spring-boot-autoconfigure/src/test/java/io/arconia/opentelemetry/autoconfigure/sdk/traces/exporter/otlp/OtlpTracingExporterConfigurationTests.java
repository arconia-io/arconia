package io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.otlp;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.OpenTelemetryTracingExporterAutoConfiguration;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OtlpTracingExporterConfiguration}.
 */
class OtlpTracingExporterConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryExporterAutoConfiguration.class, 
                OpenTelemetryTracingExporterAutoConfiguration.class));

    @Test
    void otlpExporterConfigurationEnabledByDefault() {
        contextRunner
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpTracingExporterConfiguration.class);
                assertThat(context).hasSingleBean(OtlpTracingConnectionDetails.class);
                assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcSpanExporter.class);
            });
    }

    @Test
    void otlpExporterConfigurationEnabledWhenTypeIsOtlp() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.type=otlp")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpTracingExporterConfiguration.class);
                assertThat(context).hasSingleBean(OtlpTracingConnectionDetails.class);
                assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcSpanExporter.class);
            });
    }

    @Test
    void otlpExporterConfigurationDisabledWhenTypeIsNotOtlp() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.type=console")
            .run(context -> {
                assertThat(context).doesNotHaveBean(OtlpTracingExporterConfiguration.class);
                assertThat(context).doesNotHaveBean(OtlpTracingConnectionDetails.class);
                assertThat(context).doesNotHaveBean(OtlpHttpSpanExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcSpanExporter.class);
            });
    }

    @Test
    void httpProtobufExporterCreatedByDefault() {
        contextRunner
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcSpanExporter.class);
            });
    }

    @Test
    void httpProtobufExporterCreatedWhenProtocolIsHttpProtobuf() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.otlp.protocol=http_protobuf")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcSpanExporter.class);
            });
    }

    @Test
    void grpcExporterCreatedWhenProtocolIsGrpc() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.otlp.protocol=grpc")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpGrpcSpanExporter.class);
                assertThat(context).doesNotHaveBean(OtlpHttpSpanExporter.class);
            });
    }

    @Test
    void existingConnectionDetailsRespected() {
        contextRunner
            .withBean(OtlpTracingConnectionDetails.class, () -> protocol -> "http://test:4318")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpTracingConnectionDetails.class);
                assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class);
            });
    }

    @Test
    void existingHttpExporterRespected() {
        contextRunner
            .withBean(OtlpHttpSpanExporter.class, () -> OtlpHttpSpanExporter.builder().build())
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcSpanExporter.class);
            });
    }

    @Test
    void existingGrpcExporterRespected() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.otlp.protocol=grpc")
            .withBean(OtlpGrpcSpanExporter.class, () -> OtlpGrpcSpanExporter.builder().build())
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpGrpcSpanExporter.class);
                assertThat(context).doesNotHaveBean(OtlpHttpSpanExporter.class);
            });
    }

}
