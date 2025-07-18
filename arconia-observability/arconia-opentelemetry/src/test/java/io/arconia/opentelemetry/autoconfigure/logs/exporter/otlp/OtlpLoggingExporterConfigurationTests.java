package io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterAutoConfiguration;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OtlpLoggingExporterConfiguration}.
 */
class OtlpLoggingExporterConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryExporterAutoConfiguration.class,
                OpenTelemetryLoggingExporterAutoConfiguration.class));

    @Test
    void otlpExporterConfigurationEnabledByDefault() {
        contextRunner
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpLoggingExporterConfiguration.class);
                assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);
                assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
            });
    }

    @Test
    void otlpExporterConfigurationEnabledWhenTypeIsOtlp() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.exporter.type=otlp")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpLoggingExporterConfiguration.class);
                assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);
                assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
            });
    }

    @Test
    void otlpExporterConfigurationDisabledWhenTypeIsNotOtlp() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.exporter.type=console")
            .run(context -> {
                assertThat(context).doesNotHaveBean(OtlpLoggingExporterConfiguration.class);
                assertThat(context).doesNotHaveBean(OtlpLoggingConnectionDetails.class);
                assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
            });
    }

    @Test
    void httpProtobufExporterCreatedByDefault() {
        contextRunner
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
            });
    }

    @Test
    void httpProtobufExporterCreatedWhenProtocolIsHttpProtobuf() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.exporter.otlp.protocol=http_protobuf")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
            });
    }

    @Test
    void grpcExporterCreatedWhenProtocolIsGrpc() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.exporter.otlp.protocol=grpc")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpGrpcLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
            });
    }

    @Test
    void existingConnectionDetailsRespected() {
        contextRunner
            .withBean(OtlpLoggingConnectionDetails.class, () -> protocol -> "http://test:4318")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);
                assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
            });
    }

    @Test
    void existingHttpExporterRespected() {
        contextRunner
            .withBean(OtlpHttpLogRecordExporter.class, () -> OtlpHttpLogRecordExporter.builder().build())
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
            });
    }

    @Test
    void existingGrpcExporterRespected() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.exporter.otlp.protocol=grpc")
            .withBean(OtlpGrpcLogRecordExporter.class, () -> OtlpGrpcLogRecordExporter.builder().build())
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpGrpcLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
            });
    }

}
