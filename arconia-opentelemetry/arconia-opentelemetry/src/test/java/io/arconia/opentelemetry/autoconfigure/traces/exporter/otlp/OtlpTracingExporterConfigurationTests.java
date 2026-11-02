package io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutorService;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.OpenTelemetryTracingExporterAutoConfiguration;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

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
    void grpcExporterCreatedWhenCommonProtocolIsGrpc() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.protocol=grpc")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpGrpcSpanExporter.class);
                assertThat(context).doesNotHaveBean(OtlpHttpSpanExporter.class);
            });
    }

    @Test
    void signalProtocolTakesPrecedenceOverCommonProtocol() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.exporter.otlp.protocol=grpc",
                "arconia.otel.traces.exporter.otlp.protocol=http_protobuf"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcSpanExporter.class);
            });
    }

    @Test
    void httpProtobufExporterCreatedWhenProtocolIsHyphenated() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.otlp.protocol=http-protobuf")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcSpanExporter.class);
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

    @Test
    void commonEndpointWithPathPreservedForHttpProtobuf() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.endpoint=https://eu.api.smith.langchain.com/otel")
            .run(context -> {
                OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
                assertThat(connectionDetails.getTracesUrl(io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol.HTTP_PROTOBUF))
                    .isEqualTo("https://eu.api.smith.langchain.com/otel/v1/traces");
            });
    }

    @Test
    void commonEndpointWithTrailingSlashHandledCorrectly() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.endpoint=https://example.com/path/")
            .run(context -> {
                OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
                assertThat(connectionDetails.getTracesUrl(io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol.HTTP_PROTOBUF))
                    .isEqualTo("https://example.com/path/v1/traces");
            });
    }

    @Test
    void commonEndpointWithoutPathAppendsCorrectly() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.endpoint=https://example.com")
            .run(context -> {
                OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
                assertThat(connectionDetails.getTracesUrl(io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol.HTTP_PROTOBUF))
                    .isEqualTo("https://example.com/v1/traces");
            });
    }

    @Test
    void commonEndpointNotModifiedForGrpc() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.exporter.otlp.endpoint=https://example.com/otel",
                "arconia.otel.traces.exporter.otlp.protocol=grpc"
            )
            .run(context -> {
                OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
                assertThat(connectionDetails.getTracesUrl(io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol.GRPC))
                    .isEqualTo("https://example.com/otel");
            });
    }


    @Test
    void sslBundleAppliedToHttpProtobufExporter() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
            .withPropertyValues(
                "spring.ssl.bundle.pem.otlp.truststore.certificate=classpath:ssl/ca.crt",
                "arconia.otel.traces.exporter.otlp.ssl.bundle=otlp"
            )
            .run(context -> {
                assertThat(context.getBean(OtlpTracingConnectionDetails.class).getSslBundle()).isNotNull();
                assertThat(sslContext(context.getBean(OtlpHttpSpanExporter.class))).isNotNull();
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
            .run(context -> assertThat(sslContext(context.getBean(OtlpGrpcSpanExporter.class))).isNotNull());
    }

    @Test
    void noSslContextByDefault() {
        contextRunner
            .run(context -> {
                assertThat(context.getBean(OtlpTracingConnectionDetails.class).getSslBundle()).isNull();
                assertThat(sslContext(context.getBean(OtlpHttpSpanExporter.class))).isNull();
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
                "arconia.otel.traces.exporter.otlp.proxy.host=proxy.example.com",
                "arconia.otel.traces.exporter.otlp.proxy.port=3128"
            )
            .run(context -> assertThat(proxyOptions(context.getBean(OtlpHttpSpanExporter.class)).getProxySelector().select(URI.create("https://otlp.example.com")))
                .extracting(Proxy::address)
                .containsExactly(InetSocketAddress.createUnresolved("proxy.example.com", 3128)));
    }

    @Test
    void noProxyAppliedToHttpProtobufExporterByDefault() {
        contextRunner
            .run(context -> assertThat(proxyOptions(context.getBean(OtlpHttpSpanExporter.class))).isNull());
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
                ExecutorService executorService = executorService(context.getBean(OtlpHttpSpanExporter.class));
                assertThat(executorService).isNotNull();
                assertThat(executorService.submit(() -> Thread.currentThread().isVirtual()).get()).isTrue();
            });
    }

    @Test
    void httpProtobufExporterUsesDefaultExecutorWhenVirtualThreadsDisabled() {
        contextRunner
            .run(context -> assertThat(executorService(context.getBean(OtlpHttpSpanExporter.class))).isNull());
    }

    @Test
    void signalRetrySettingsCombineWithCommonOnes() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.exporter.otlp.retry.backoff-config.max-backoff=30s",
                "arconia.otel.traces.exporter.otlp.retry.max-attempts=3"
            )
            .run(context -> {
                RetryPolicy retryPolicy = retryPolicy(context.getBean(OtlpHttpSpanExporter.class));
                assertThat(retryPolicy.getMaxAttempts()).isEqualTo(3);
                assertThat(retryPolicy.getMaxBackoff()).isEqualTo(Duration.ofSeconds(30));
            });
    }

    private static RetryPolicy retryPolicy(Object exporter) {
        Object builder = ReflectionTestUtils.getField(exporter, "builder");
        return (RetryPolicy) ReflectionTestUtils.getField(builder, "retryPolicy");
    }

    private static @Nullable ExecutorService executorService(Object exporter) {
        Object builder = ReflectionTestUtils.getField(exporter, "builder");
        return (ExecutorService) ReflectionTestUtils.getField(builder, "executorService");
    }

    private static @Nullable ProxyOptions proxyOptions(Object exporter) {
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
