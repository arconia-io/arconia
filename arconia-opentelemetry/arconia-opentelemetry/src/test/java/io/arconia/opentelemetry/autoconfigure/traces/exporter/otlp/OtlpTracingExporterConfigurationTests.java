package io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.OpenTelemetryTracingExporterAutoConfiguration;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
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

    /**
     * Reads the configured SSL context without calling the getter, which creates a default one.
     */
    private static Object sslContext(Object exporter) {
        Object builder = ReflectionTestUtils.getField(exporter, "builder");
        Object tlsConfigHelper = ReflectionTestUtils.getField(builder, "tlsConfigHelper");
        return ReflectionTestUtils.getField(tlsConfigHelper, "sslContext");
    }

}
