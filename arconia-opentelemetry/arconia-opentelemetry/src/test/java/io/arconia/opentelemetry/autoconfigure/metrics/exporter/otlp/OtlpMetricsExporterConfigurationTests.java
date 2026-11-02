package io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;

import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMeterProviderBuilderCustomizer;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsAutoConfiguration;
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
    void grpcExporterCreatedWhenCommonProtocolIsGrpc() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.protocol=grpc")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpGrpcMetricExporter.class);
                assertThat(context).doesNotHaveBean(OtlpHttpMetricExporter.class);
            });
    }

    @Test
    void signalProtocolTakesPrecedenceOverCommonProtocol() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.exporter.otlp.protocol=grpc",
                "arconia.otel.metrics.exporter.otlp.protocol=http_protobuf"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcMetricExporter.class);
            });
    }

    @Test
    void httpProtobufExporterCreatedWhenProtocolIsHyphenated() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.otlp.protocol=http-protobuf")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcMetricExporter.class);
            });
    }

    @Test
    void existingConnectionDetailsRespected() {
        contextRunner
            .withBean(OtlpMetricsConnectionDetails.class, () -> new OtlpMetricsConnectionDetails() {
                @Override
                public String getMetricsUrl(Protocol protocol) {
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
                assertThat(connectionDetails.getMetricsUrl(Protocol.HTTP_PROTOBUF)).isEqualTo("http://custom:4318");
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
    void explicitBucketHistogramAggregationByDefault() {
        contextRunner
            .run(context -> {
                OtlpHttpMetricExporter exporter = context.getBean(OtlpHttpMetricExporter.class);
                assertThat(exporter.getDefaultAggregation(InstrumentType.HISTOGRAM)).isEqualTo(Aggregation.defaultAggregation());
            });
    }

    @Test
    void base2ExponentialHistogramAggregationApplied() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.histogram-aggregation=base2-exponential-bucket-histogram")
            .run(context -> {
                OtlpHttpMetricExporter exporter = context.getBean(OtlpHttpMetricExporter.class);
                assertThat(exporter.getDefaultAggregation(InstrumentType.HISTOGRAM)).isEqualTo(Aggregation.base2ExponentialBucketHistogram());
                assertThat(exporter.getDefaultAggregation(InstrumentType.COUNTER)).isEqualTo(Aggregation.defaultAggregation());
            });

        contextRunner
            .withPropertyValues(
                "arconia.otel.metrics.exporter.histogram-aggregation=base2-exponential-bucket-histogram",
                "arconia.otel.metrics.exporter.otlp.protocol=grpc"
            )
            .run(context -> {
                OtlpGrpcMetricExporter exporter = context.getBean(OtlpGrpcMetricExporter.class);
                assertThat(exporter.getDefaultAggregation(InstrumentType.HISTOGRAM)).isEqualTo(Aggregation.base2ExponentialBucketHistogram());
            });
    }

    @ParameterizedTest
    @ValueSource(strings = { "http_protobuf", "grpc" })
    void exporterMetricsDoNotCreateCycleWithMeterProvider(String protocol) {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryAutoConfiguration.class, OpenTelemetryExporterAutoConfiguration.class,
                OpenTelemetryMetricsAutoConfiguration.class, OpenTelemetryMetricsExporterAutoConfiguration.class))
            .withBean(Resource.class, Resource::empty)
            .withPropertyValues(
                "arconia.otel.metrics.exporter.otlp.metrics=true",
                "arconia.otel.metrics.exporter.otlp.protocol=" + protocol
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(SdkMeterProvider.class);
                assertThat(context).hasSingleBean(MetricExporter.class);
            });
    }

    @Test
    void histogramBucketAdvicePreserved() {
        InMemoryMetricReader metricReader = InMemoryMetricReader.create();
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryAutoConfiguration.class, OpenTelemetryExporterAutoConfiguration.class,
                OpenTelemetryMetricsAutoConfiguration.class, OpenTelemetryMetricsExporterAutoConfiguration.class))
            .withBean(Resource.class, Resource::empty)
            .withBean(OpenTelemetryMeterProviderBuilderCustomizer.class, () -> builder -> builder.registerMetricReader(metricReader))
            .run(context -> {
                SdkMeterProvider meterProvider = context.getBean(SdkMeterProvider.class);
                meterProvider.get("test").histogramBuilder("test.duration")
                    .setExplicitBucketBoundariesAdvice(List.of(0.1, 0.2, 0.3))
                    .build()
                    .record(0.15);

                MetricData metric = metricReader.collectAllMetrics().iterator().next();
                assertThat(metric.getHistogramData().getPoints().iterator().next().getBoundaries())
                    .containsExactly(0.1, 0.2, 0.3);
            });
    }

    @Test
    void commonEndpointWithPathPreservedForHttpProtobuf() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.endpoint=https://eu.api.smith.langchain.com/otel")
            .run(context -> {
                OtlpMetricsConnectionDetails connectionDetails = context.getBean(OtlpMetricsConnectionDetails.class);
                assertThat(connectionDetails.getMetricsUrl(Protocol.HTTP_PROTOBUF))
                    .isEqualTo("https://eu.api.smith.langchain.com/otel/v1/metrics");
            });
    }

    @Test
    void commonEndpointWithTrailingSlashHandledCorrectly() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.endpoint=https://example.com/path/")
            .run(context -> {
                OtlpMetricsConnectionDetails connectionDetails = context.getBean(OtlpMetricsConnectionDetails.class);
                assertThat(connectionDetails.getMetricsUrl(Protocol.HTTP_PROTOBUF))
                    .isEqualTo("https://example.com/path/v1/metrics");
            });
    }

    @Test
    void commonEndpointWithoutPathAppendsCorrectly() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.endpoint=https://example.com")
            .run(context -> {
                OtlpMetricsConnectionDetails connectionDetails = context.getBean(OtlpMetricsConnectionDetails.class);
                assertThat(connectionDetails.getMetricsUrl(Protocol.HTTP_PROTOBUF))
                    .isEqualTo("https://example.com/v1/metrics");
            });
    }

    @Test
    void commonEndpointNotModifiedForGrpc() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.exporter.otlp.endpoint=https://example.com/otel",
                "arconia.otel.metrics.exporter.otlp.protocol=grpc"
            )
            .run(context -> {
                OtlpMetricsConnectionDetails connectionDetails = context.getBean(OtlpMetricsConnectionDetails.class);
                assertThat(connectionDetails.getMetricsUrl(Protocol.GRPC))
                    .isEqualTo("https://example.com/otel");
            });
    }


    @Test
    void sslBundleAppliedToHttpProtobufExporter() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
            .withPropertyValues(
                "spring.ssl.bundle.pem.otlp.truststore.certificate=classpath:ssl/ca.crt",
                "arconia.otel.metrics.exporter.otlp.ssl.bundle=otlp"
            )
            .run(context -> {
                assertThat(context.getBean(OtlpMetricsConnectionDetails.class).getSslBundle()).isNotNull();
                assertThat(sslContext(context.getBean(OtlpHttpMetricExporter.class))).isNotNull();
            });
    }

    @Test
    void sslBundleAppliedToGrpcExporter() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
            .withPropertyValues(
                "spring.ssl.bundle.pem.otlp.truststore.certificate=classpath:ssl/ca.crt",
                "arconia.otel.exporter.otlp.ssl.bundle=otlp",
                "arconia.otel.exporter.otlp.protocol=grpc"
            )
            .run(context -> assertThat(sslContext(context.getBean(OtlpGrpcMetricExporter.class))).isNotNull());
    }

    @Test
    void noSslContextByDefault() {
        contextRunner
            .run(context -> {
                assertThat(context.getBean(OtlpMetricsConnectionDetails.class).getSslBundle()).isNull();
                assertThat(sslContext(context.getBean(OtlpHttpMetricExporter.class))).isNull();
            });
    }

    @Test
    void failsWhenSslBundleConfiguredWithoutSslBundles() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.ssl.bundle=otlp")
            .run(context -> assertThat(context).getFailure()
                .rootCause()
                .hasMessage("The SSL bundle 'otlp' is configured for the OTLP exporter, but no SSL bundles are available"));
    }

    @Test
    void proxyAppliedToHttpProtobufExporter() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.metrics.exporter.otlp.proxy.host=proxy.example.com",
                "arconia.otel.metrics.exporter.otlp.proxy.port=3128"
            )
            .run(context -> assertThat(proxyOptions(context.getBean(OtlpHttpMetricExporter.class)).getProxySelector().select(URI.create("https://otlp.example.com")))
                .extracting(Proxy::address)
                .containsExactly(InetSocketAddress.createUnresolved("proxy.example.com", 3128)));
    }

    @Test
    void jvmProxySelectorAppliedToHttpProtobufExporterByDefault() {
        contextRunner
            .run(context -> assertThat(proxyOptions(context.getBean(OtlpHttpMetricExporter.class)).getProxySelector())
                .isSameAs(ProxySelector.getDefault()));
    }

    @Test
    void failsWhenProxyConfiguredWithGrpc() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.exporter.otlp.proxy.host=proxy.example.com",
                "arconia.otel.exporter.otlp.proxy.port=3128",
                "arconia.otel.exporter.otlp.protocol=grpc"
            )
            .run(context -> assertThat(context).getFailure()
                .rootCause()
                .hasMessageContaining("doesn't support a proxy with the gRPC protocol"));
    }

    @Test
    void httpProtobufExporterUsesVirtualThreadsWhenEnabled() {
        contextRunner
            .withPropertyValues("spring.threads.virtual.enabled=true")
            .run(context -> {
                ExecutorService executorService = executorService(context.getBean(OtlpHttpMetricExporter.class));
                assertThat(executorService).isNotNull();
                assertThat(executorService.submit(() -> Thread.currentThread().isVirtual()).get()).isTrue();
            });
    }

    @Test
    void httpProtobufExporterUsesDefaultExecutorWhenVirtualThreadsDisabled() {
        contextRunner
            .run(context -> assertThat(executorService(context.getBean(OtlpHttpMetricExporter.class))).isNull());
    }

    private static @Nullable ExecutorService executorService(Object exporter) {
        Object builder = ReflectionTestUtils.getField(exporter, "builder");
        return (ExecutorService) ReflectionTestUtils.getField(builder, "executorService");
    }

    private static ProxyOptions proxyOptions(Object exporter) {
        Object builder = ReflectionTestUtils.getField(exporter, "builder");
        return (ProxyOptions) ReflectionTestUtils.getField(builder, "proxyOptions");
    }

    /**
     * Reads the configured SSL context without calling the getter, which creates a default one.
     */
    private static Object sslContext(Object exporter) {
        Object builder = ReflectionTestUtils.getField(exporter, "builder");
        Object tlsConfigHelper = ReflectionTestUtils.getField(builder, "tlsConfigHelper");
        return ReflectionTestUtils.getField(tlsConfigHelper, "sslContext");
    }

}
