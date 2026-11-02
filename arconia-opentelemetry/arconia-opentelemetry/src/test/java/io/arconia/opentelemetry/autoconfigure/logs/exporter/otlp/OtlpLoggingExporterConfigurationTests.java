package io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterAutoConfiguration;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

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
    void grpcExporterCreatedWhenCommonProtocolIsGrpc() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.protocol=grpc")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpGrpcLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
            });
    }

    @Test
    void signalProtocolTakesPrecedenceOverCommonProtocol() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.exporter.otlp.protocol=grpc",
                "arconia.otel.logs.exporter.otlp.protocol=http_protobuf"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
            });
    }

    @Test
    void httpProtobufExporterCreatedWhenProtocolIsHyphenated() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.exporter.otlp.protocol=http-protobuf")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
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

    @Test
    void commonEndpointWithPathPreservedForHttpProtobuf() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.endpoint=https://eu.api.smith.langchain.com/otel")
            .run(context -> {
                OtlpLoggingConnectionDetails connectionDetails = context.getBean(OtlpLoggingConnectionDetails.class);
                assertThat(connectionDetails.getLogsUrl(io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol.HTTP_PROTOBUF))
                    .isEqualTo("https://eu.api.smith.langchain.com/otel/v1/logs");
            });
    }

    @Test
    void commonEndpointWithTrailingSlashHandledCorrectly() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.endpoint=https://example.com/path/")
            .run(context -> {
                OtlpLoggingConnectionDetails connectionDetails = context.getBean(OtlpLoggingConnectionDetails.class);
                assertThat(connectionDetails.getLogsUrl(io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol.HTTP_PROTOBUF))
                    .isEqualTo("https://example.com/path/v1/logs");
            });
    }

    @Test
    void commonEndpointWithoutPathAppendsCorrectly() {
        contextRunner
            .withPropertyValues("arconia.otel.exporter.otlp.endpoint=https://example.com")
            .run(context -> {
                OtlpLoggingConnectionDetails connectionDetails = context.getBean(OtlpLoggingConnectionDetails.class);
                assertThat(connectionDetails.getLogsUrl(io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol.HTTP_PROTOBUF))
                    .isEqualTo("https://example.com/v1/logs");
            });
    }

    @Test
    void commonEndpointNotModifiedForGrpc() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.exporter.otlp.endpoint=https://example.com/otel",
                "arconia.otel.logs.exporter.otlp.protocol=grpc"
            )
            .run(context -> {
                OtlpLoggingConnectionDetails connectionDetails = context.getBean(OtlpLoggingConnectionDetails.class);
                assertThat(connectionDetails.getLogsUrl(io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol.GRPC))
                    .isEqualTo("https://example.com/otel");
            });
    }


    @Test
    void sslBundleAppliedToHttpProtobufExporter() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
            .withPropertyValues(
                "spring.ssl.bundle.pem.otlp.truststore.certificate=classpath:ssl/ca.crt",
                "arconia.otel.logs.exporter.otlp.ssl.bundle=otlp"
            )
            .run(context -> {
                assertThat(context.getBean(OtlpLoggingConnectionDetails.class).getSslBundle()).isNotNull();
                assertThat(sslContext(context.getBean(OtlpHttpLogRecordExporter.class))).isNotNull();
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
            .run(context -> assertThat(sslContext(context.getBean(OtlpGrpcLogRecordExporter.class))).isNotNull());
    }

    @Test
    void noSslContextByDefault() {
        contextRunner
            .run(context -> {
                assertThat(context.getBean(OtlpLoggingConnectionDetails.class).getSslBundle()).isNull();
                assertThat(sslContext(context.getBean(OtlpHttpLogRecordExporter.class))).isNull();
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
