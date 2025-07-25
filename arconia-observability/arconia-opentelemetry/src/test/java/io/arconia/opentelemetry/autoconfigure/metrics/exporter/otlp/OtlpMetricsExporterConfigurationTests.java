package io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp;

import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMeterProviderBuilderCustomizer;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OtlpMetricsExporterConfiguration}.
 */
class OtlpMetricsExporterConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryExporterAutoConfiguration.class, OpenTelemetryMetricsExporterAutoConfiguration.class))
            .withBean(CardinalityLimitSelector.class, CardinalityLimitSelector::defaultCardinalityLimitSelector)
            .withBean(OpenTelemetryMetricsProperties.class, OpenTelemetryMetricsProperties::new);

    @Test
    void otlpExporterConfigurationEnabledByDefault() {
        contextRunner
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpMetricsExporterConfiguration.class);
                assertThat(context).hasSingleBean(OtlpMetricsConnectionDetails.class);
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcMetricExporter.class);
                assertThat(context).hasBean("histogramAggregation");

                // Verify default histogram aggregation
                OpenTelemetryMetricsExporterProperties properties = context.getBean(OpenTelemetryMetricsExporterProperties.class);
                assertThat(properties.getHistogramAggregation().name())
                        .isEqualTo("EXPLICIT_BUCKET_HISTOGRAM");
            });
    }

    @Test
    void otlpExporterConfigurationEnabledWhenTypeIsOtlp() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.type=otlp")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpMetricsExporterConfiguration.class);
                assertThat(context).hasSingleBean(OtlpMetricsConnectionDetails.class);
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcMetricExporter.class);
            });
    }

    @Test
    void otlpExporterConfigurationDisabledWhenTypeIsNotOtlp() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.type=console")
            .run(context -> {
                assertThat(context).doesNotHaveBean(OtlpMetricsExporterConfiguration.class);
                assertThat(context).doesNotHaveBean(OtlpMetricsConnectionDetails.class);
                assertThat(context).doesNotHaveBean(OtlpHttpMetricExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcMetricExporter.class);
            });
    }

    @Test
    void httpProtobufExporterCreatedByDefault() {
        contextRunner
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcMetricExporter.class);
            });
    }

    @Test
    void httpProtobufExporterCreatedWhenProtocolIsHttpProtobuf() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.otlp.protocol=http_protobuf")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcMetricExporter.class);
            });
    }

    @Test
    void grpcExporterCreatedWhenProtocolIsGrpc() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.otlp.protocol=grpc")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpGrpcMetricExporter.class);
                assertThat(context).doesNotHaveBean(OtlpHttpMetricExporter.class);
            });
    }

    @Test
    void existingConnectionDetailsRespected() {
        contextRunner
            .withBean(OtlpMetricsConnectionDetails.class, () -> new OtlpMetricsConnectionDetails() {
                @Override
                public String getUrl(Protocol protocol) {
                    return "http://test:4318";
                }
            })
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpMetricsConnectionDetails.class);
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
            });
    }

    @Test
    void aggregationTemporalityConfigurationRespected() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.aggregation-temporality=delta")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
            });
    }

    @Test
    void compressionConfigurationRespected() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.otlp.compression=gzip")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
            });
    }

    @Test
    void timeoutConfigurationRespected() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.metrics.exporter.otlp.timeout=5s",
                "arconia.otel.metrics.exporter.otlp.connect-timeout=2s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
            });
    }

    @Test
    void headersConfigurationRespected() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.metrics.exporter.otlp.headers.test=value",
                "arconia.otel.exporter.otlp.headers.common=shared"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
            });
    }

    @Test
    void customEndpointConfigurationRespected() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.otlp.endpoint=http://custom:4318")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
                OtlpMetricsConnectionDetails connectionDetails = context.getBean(OtlpMetricsConnectionDetails.class);
                assertThat(connectionDetails.getUrl(Protocol.HTTP_PROTOBUF)).isEqualTo("http://custom:4318");
            });
    }

    @Test
    void existingHttpExporterRespected() {
        contextRunner
            .withBean(OtlpHttpMetricExporter.class, () -> OtlpHttpMetricExporter.builder().build())
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcMetricExporter.class);
            });
    }

    @Test
    void existingGrpcExporterRespected() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.otlp.protocol=grpc")
            .withBean(OtlpGrpcMetricExporter.class, () -> OtlpGrpcMetricExporter.builder().build())
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpGrpcMetricExporter.class);
                assertThat(context).doesNotHaveBean(OtlpHttpMetricExporter.class);
            });
    }

    @Test
    void histogramAggregationConfigurationApplied() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.histogram-aggregation=base2-exponential-bucket-histogram")
            .run(context -> {
                assertThat(context).getBeanNames(OpenTelemetryMeterProviderBuilderCustomizer.class).hasSize(2);
                assertThat(context).hasBean("histogramAggregation");
                assertThat(context).hasBean("metricBuilderPlatformThreads");

                // Verify the histogram aggregation property is set correctly
                OpenTelemetryMetricsExporterProperties properties = context.getBean(OpenTelemetryMetricsExporterProperties.class);
                assertThat(properties.getHistogramAggregation().name())
                        .isEqualTo("BASE2_EXPONENTIAL_BUCKET_HISTOGRAM");
            });

        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.histogram-aggregation=explicit-bucket-histogram")
            .run(context -> {
                assertThat(context).getBeanNames(OpenTelemetryMeterProviderBuilderCustomizer.class).hasSize(2);
                assertThat(context).hasBean("histogramAggregation");
                assertThat(context).hasBean("metricBuilderPlatformThreads");

                // Verify the histogram aggregation property is set correctly
                OpenTelemetryMetricsExporterProperties properties = context.getBean(OpenTelemetryMetricsExporterProperties.class);
                assertThat(properties.getHistogramAggregation().name())
                        .isEqualTo("EXPLICIT_BUCKET_HISTOGRAM");
            });
    }

}
