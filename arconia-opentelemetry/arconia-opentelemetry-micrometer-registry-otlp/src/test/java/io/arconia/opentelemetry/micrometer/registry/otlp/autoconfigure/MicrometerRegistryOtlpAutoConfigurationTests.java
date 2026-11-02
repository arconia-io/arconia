package io.arconia.opentelemetry.micrometer.registry.otlp.autoconfigure;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.CompressionMode;
import io.micrometer.registry.otlp.HistogramFlavor;
import io.micrometer.registry.otlp.OtlpHttpMetricsSender;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.registry.otlp.OtlpMetricsSender;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MicrometerRegistryOtlpAutoConfiguration}.
 */
class MicrometerRegistryOtlpAutoConfigurationTests {

    private final ApplicationContextRunner baseContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MetricsAutoConfiguration.class,
                    MicrometerRegistryOtlpAutoConfiguration.class))
            .withBean(OpenTelemetryExporterProperties.class, OpenTelemetryExporterProperties::new)
            .withBean(OpenTelemetryMetricsExporterProperties.class, OpenTelemetryMetricsExporterProperties::new);

    private final ApplicationContextRunner contextRunner = baseContextRunner
            .withBean(OtlpMetricsConnectionDetails.class, () -> protocol ->
                    protocol == Protocol.GRPC ? "http://localhost:4317" : "http://localhost:4318/v1/metrics");

    @Test
    void autoConfigurationNotActivatedWhenRegistryDisabled() {
        contextRunner
                .withPropertyValues("arconia.otel.exporter.otlp.micrometer.enabled=false")
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MicrometerOtlpConfig.class);
                    assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class);
                });
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
                .withPropertyValues("arconia.otel.enabled=false")
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MicrometerOtlpConfig.class);
                    assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class);
                });
    }

    @Test
    void autoConfigurationNotActivatedWhenMetricsDisabled() {
        contextRunner
                .withPropertyValues("arconia.otel.metrics.enabled=false")
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MicrometerOtlpConfig.class);
                    assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class);
                });
    }

    @Test
    void autoConfigurationNotActivatedWhenMetricsExporterNotOtlp() {
        contextRunner
                .withPropertyValues("arconia.otel.metrics.exporter.type=console")
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MicrometerOtlpConfig.class);
                    assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class);
                });
    }

    @Test
    void autoConfigurationNotActivatedWhenMetricsExportDisabled() {
        contextRunner
                .withPropertyValues("arconia.otel.metrics.exporter.type=none")
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MicrometerOtlpConfig.class);
                    assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class);
                });
    }

    @Test
    void beansAvailableWithDefaultConfiguration() {
        contextRunner
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> {
                    assertThat(context).hasSingleBean(MicrometerOtlpConfig.class);
                    assertThat(context).hasSingleBean(OtlpMeterRegistry.class);

                    MicrometerOtlpConfig config = context.getBean(MicrometerOtlpConfig.class);
                    assertThat(config).isNotNull();
                    assertThat(config.url()).isEqualTo("http://localhost:4318/v1/metrics");

                    OtlpMeterRegistry registry = context.getBean(OtlpMeterRegistry.class);
                    assertThat(registry).isNotNull();
                });
    }

    @Test
    void otlpConfigConfiguredWithCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "arconia.otel.exporter.otlp.micrometer.base-time-unit=milliseconds",
                        "arconia.otel.exporter.otlp.micrometer.max-scale=10",
                        "arconia.otel.exporter.otlp.micrometer.max-bucket-count=100",
                        "arconia.otel.metrics.exporter.interval=PT10S",
                        "arconia.otel.metrics.exporter.aggregation-temporality=delta",
                        "arconia.otel.metrics.exporter.histogram-aggregation=base2-exponential-bucket-histogram"
                )
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> {
                    assertThat(context).hasSingleBean(MicrometerOtlpConfig.class);

                    MicrometerOtlpConfig config = context.getBean(MicrometerOtlpConfig.class);
                    assertThat(config.baseTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
                    assertThat(config.maxScale()).isEqualTo(10);
                    assertThat(config.maxBucketCount()).isEqualTo(100);
                    assertThat(config.step()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(config.aggregationTemporality()).isEqualTo(AggregationTemporality.DELTA);
                    assertThat(config.histogramFlavor()).isEqualTo(HistogramFlavor.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM);
                });
    }

    @Test
    void otlpConfigWithCustomHeaders() {
        contextRunner
                .withPropertyValues(
                        "arconia.otel.exporter.otlp.headers.custom-header=common-value",
                        "arconia.otel.metrics.exporter.otlp.headers.metrics-header=metrics-value"
                )
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> {
                    assertThat(context).hasSingleBean(MicrometerOtlpConfig.class);

                    MicrometerOtlpConfig config = context.getBean(MicrometerOtlpConfig.class);
                    assertThat(config.headers()).containsEntry("custom-header", "common-value");
                    assertThat(config.headers()).containsEntry("metrics-header", "metrics-value");
                });
    }

    @Test
    void otlpConfigWithResourceAttributes() {
        contextRunner
                .withBean(Resource.class, () -> Resource.create(
                        Attributes.of(
                                AttributeKey.stringKey("service.name"), "test-service",
                                AttributeKey.stringKey("service.version"), "1.0.0",
                                AttributeKey.stringKey("telemetry.sdk.language"), "custom",
                                AttributeKey.stringKey("telemetry.sdk.name"), "custom",
                                AttributeKey.stringKey("telemetry.sdk.version"), "2.1.0"
                        )
                ))
                .run(context -> {
                    assertThat(context).hasSingleBean(MicrometerOtlpConfig.class);

                    MicrometerOtlpConfig config = context.getBean(MicrometerOtlpConfig.class);
                    assertThat(config.resourceAttributes()).containsEntry("service.name", "test-service");
                    assertThat(config.resourceAttributes()).containsEntry("service.version", "1.0.0");
                    assertThat(config.resourceAttributes()).doesNotContainKey("telemetry.sdk.language");
                    assertThat(config.resourceAttributes()).doesNotContainKey("telemetry.sdk.name");
                    assertThat(config.resourceAttributes()).doesNotContainKey("telemetry.sdk.version");
                });
    }

    @Test
    void otlpConfigIncludesConnectionDetailsHeaders() {
        baseContextRunner
                .withPropertyValues("arconia.otel.exporter.otlp.headers.Authorization=from-properties")
                .withBean(Resource.class, Resource::getDefault)
                .withBean(OtlpMetricsConnectionDetails.class, () -> new OtlpMetricsConnectionDetails() {
                    @Override
                    public String getMetricsUrl(Protocol protocol) {
                        return "http://localhost:4318/v1/metrics";
                    }

                    @Override
                    public Map<String, String> getHeaders() {
                        return Map.of("Authorization", "from-connection-details", "X-Tenant", "acme");
                    }
                })
                .run(context -> {
                    MicrometerOtlpConfig config = context.getBean(MicrometerOtlpConfig.class);
                    assertThat(config.headers())
                            .containsEntry("Authorization", "from-properties")
                            .containsEntry("X-Tenant", "acme");
                });
    }

    @Test
    void otlpConfigWithCompression() {
        contextRunner
                .withPropertyValues("arconia.otel.metrics.exporter.otlp.compression=none")
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> assertThat(context.getBean(MicrometerOtlpConfig.class).compressionMode())
                        .isEqualTo(CompressionMode.NONE));
    }

    @Test
    void otlpConfigFailsWithGrpcProtocol() {
        contextRunner
                .withPropertyValues("arconia.otel.exporter.otlp.protocol=grpc")
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .hasMessageContaining("The Micrometer OTLP registry only supports the HTTP/Protobuf protocol"));
    }

    @Test
    void otlpConfigWithGrpcProtocolForOtherSignalsOnly() {
        contextRunner
                .withPropertyValues(
                        "arconia.otel.exporter.otlp.protocol=grpc",
                        "arconia.otel.metrics.exporter.otlp.protocol=http-protobuf"
                )
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> assertThat(context.getBean(MicrometerOtlpConfig.class).url())
                        .isEqualTo("http://localhost:4318/v1/metrics"));
    }

    @Test
    void metricsSenderUsesOtlpTimeouts() {
        contextRunner
                .withPropertyValues(
                        "arconia.otel.exporter.otlp.connect-timeout=2s",
                        "arconia.otel.metrics.exporter.otlp.timeout=7s"
                )
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> {
                    JdkHttpClientSender httpSender = httpSender(context.getBean(OtlpHttpMetricsSender.class));
                    HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(httpSender, "httpClient");
                    assertThat(httpClient.connectTimeout()).contains(Duration.ofSeconds(2));
                    assertThat(ReflectionTestUtils.getField(httpSender, "timeout")).isEqualTo(Duration.ofSeconds(7));
                });
    }

    @Test
    void metricsSenderUsesProxy() {
        contextRunner
                .withPropertyValues(
                        "arconia.otel.metrics.exporter.otlp.proxy.host=proxy.example.com",
                        "arconia.otel.metrics.exporter.otlp.proxy.port=3128"
                )
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> {
                    JdkHttpClientSender httpSender = httpSender(context.getBean(OtlpHttpMetricsSender.class));
                    HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(httpSender, "httpClient");
                    assertThat(httpClient.proxy()).hasValueSatisfying(proxySelector ->
                            assertThat(proxySelector.select(URI.create("https://otlp.example.com")))
                                    .extracting(Proxy::address)
                                    .containsExactly(InetSocketAddress.createUnresolved("proxy.example.com", 3128)));
                });
    }

    @Test
    void metricsSenderUsesConnectionDetailsSslBundle() {
        SslBundle sslBundle = SslBundle.of(new PemSslStoreBundle(null, PemSslStoreDetails.forCertificate("classpath:ssl/ca.crt")));
        baseContextRunner
                .withBean(Resource.class, Resource::getDefault)
                .withBean(OtlpMetricsConnectionDetails.class, () -> new OtlpMetricsConnectionDetails() {
                    @Override
                    public String getMetricsUrl(Protocol protocol) {
                        return "https://localhost:4318/v1/metrics";
                    }

                    @Override
                    public SslBundle getSslBundle() {
                        return sslBundle;
                    }
                })
                .run(context -> {
                    JdkHttpClientSender httpSender = httpSender(context.getBean(OtlpHttpMetricsSender.class));
                    HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(httpSender, "httpClient");
                    assertThat(httpClient.sslContext()).isNotSameAs(SSLContext.getDefault());
                });
    }

    @Test
    void customMetricsSenderUsed() {
        OtlpMetricsSender customMetricsSender = request -> {};
        contextRunner
                .withBean(Resource.class, Resource::getDefault)
                .withBean(OtlpMetricsSender.class, () -> customMetricsSender)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OtlpHttpMetricsSender.class);
                    assertThat(context).hasSingleBean(OtlpMeterRegistry.class);
                });
    }

    @Test
    void otlpMeterRegistryWithVirtualThreads() {
        contextRunner
                .withPropertyValues("spring.threads.virtual.enabled=true")
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> assertThat(context).hasSingleBean(OtlpMeterRegistry.class));
    }

    @Test
    void otlpMeterRegistryWithPlatformThreads() {
        contextRunner
                .withPropertyValues("spring.threads.virtual.enabled=false")
                .withBean(Resource.class, Resource::getDefault)
                .run(context -> assertThat(context).hasSingleBean(OtlpMeterRegistry.class));
    }

    private static JdkHttpClientSender httpSender(OtlpHttpMetricsSender metricsSender) {
        Object httpSender = ReflectionTestUtils.getField(metricsSender, "httpSender");
        assertThat(httpSender).isInstanceOf(JdkHttpClientSender.class);
        return (JdkHttpClientSender) httpSender;
    }

}
