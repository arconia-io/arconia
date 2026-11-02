package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.common.export.RetryPolicy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link EffectiveOtlpExporterConfig}.
 */
class EffectiveOtlpExporterConfigTests {

    private static final String SIGNAL_PATH = "/v1/traces";

    private final OpenTelemetryExporterProperties commonProperties = new OpenTelemetryExporterProperties();

    private final OtlpExporterConfig signalProperties = new OtlpExporterConfig();

    private final EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, signalProperties);

    @Test
    void whenCommonPropertiesNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EffectiveOtlpExporterConfig(null, signalProperties))
                .withMessage("commonProperties cannot be null");
    }

    @Test
    void whenSignalPropertiesNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EffectiveOtlpExporterConfig(commonProperties, null))
                .withMessage("signalProperties cannot be null");
    }

    @Nested
    class ProtocolResolution {

        @Test
        void defaultsToHttpProtobuf() {
            assertThat(config.getProtocol()).isEqualTo(Protocol.HTTP_PROTOBUF);
        }

        @Test
        void usesCommonProtocol() {
            commonProperties.getOtlp().setProtocol(Protocol.GRPC);

            assertThat(config.getProtocol()).isEqualTo(Protocol.GRPC);
        }

        @Test
        void signalProtocolTakesPrecedence() {
            commonProperties.getOtlp().setProtocol(Protocol.GRPC);
            signalProperties.setProtocol(Protocol.HTTP_PROTOBUF);

            assertThat(config.getProtocol()).isEqualTo(Protocol.HTTP_PROTOBUF);
        }

    }

    @Nested
    class UrlResolution {

        @Test
        void defaultsToLocalhost() {
            assertThat(config.getUrl(Protocol.HTTP_PROTOBUF, SIGNAL_PATH)).isEqualTo("http://localhost:4318/v1/traces");
            assertThat(config.getUrl(Protocol.GRPC, SIGNAL_PATH)).isEqualTo("http://localhost:4317");
        }

        @Test
        void appendsSignalPathToCommonEndpointForHttpProtobuf() {
            commonProperties.getOtlp().setEndpoint(URI.create("https://example.com/otel"));

            assertThat(config.getUrl(Protocol.HTTP_PROTOBUF, SIGNAL_PATH)).isEqualTo("https://example.com/otel/v1/traces");
        }

        @Test
        void appendsSignalPathToCommonEndpointWithTrailingSlash() {
            commonProperties.getOtlp().setEndpoint(URI.create("https://example.com/otel/"));

            assertThat(config.getUrl(Protocol.HTTP_PROTOBUF, SIGNAL_PATH)).isEqualTo("https://example.com/otel/v1/traces");
        }

        @Test
        void usesCommonEndpointAsItIsForGrpc() {
            commonProperties.getOtlp().setEndpoint(URI.create("https://example.com/otel"));

            assertThat(config.getUrl(Protocol.GRPC, SIGNAL_PATH)).isEqualTo("https://example.com/otel");
        }

        @Test
        void usesSignalEndpointAsItIs() {
            commonProperties.getOtlp().setEndpoint(URI.create("https://example.com/otel"));
            signalProperties.setEndpoint(URI.create("https://traces.example.com/custom"));

            assertThat(config.getUrl(Protocol.HTTP_PROTOBUF, SIGNAL_PATH)).isEqualTo("https://traces.example.com/custom");
            assertThat(config.getUrl(Protocol.GRPC, SIGNAL_PATH)).isEqualTo("https://traces.example.com/custom");
        }

    }

    @Nested
    class TimeoutResolution {

        @Test
        void usesCommonTimeouts() {
            assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        void signalTimeoutsTakePrecedence() {
            signalProperties.setTimeout(Duration.ofSeconds(3));
            signalProperties.setConnectTimeout(Duration.ofSeconds(2));

            assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(3));
            assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        }

    }

    @Nested
    class CompressionResolution {

        @Test
        void usesCommonCompression() {
            assertThat(config.getCompression()).isEqualTo("gzip");
        }

        @Test
        void signalCompressionTakesPrecedence() {
            signalProperties.setCompression(Compression.NONE);

            assertThat(config.getCompression()).isEqualTo("none");
        }

    }

    @Nested
    class RetryResolution {

        @Test
        void usesCommonRetry() {
            commonProperties.getOtlp().getRetry().setMaxAttempts(3);

            assertThat(config.getRetryPolicy().getMaxAttempts()).isEqualTo(3);
        }

        @Test
        void signalRetryTakesPrecedence() {
            RetryConfig retry = new RetryConfig();
            retry.setMaxAttempts(2);
            signalProperties.setRetry(retry);

            RetryPolicy retryPolicy = config.getRetryPolicy();

            assertThat(retryPolicy.getMaxAttempts()).isEqualTo(2);
        }

    }

    @Nested
    class HeaderResolution {

        @Test
        void laterSourcesOverrideEarlierOnes() {
            OtlpConnectionDetails connectionDetails = new OtlpConnectionDetails() {
                @Override
                public Map<String, String> getHeaders() {
                    return Map.of("Authorization", "from-connection-details", "X-Details", "details");
                }
            };
            commonProperties.getOtlp().setHeaders(Map.of("Authorization", "from-common", "X-Common", "common"));
            signalProperties.setHeaders(Map.of("X-Common", "from-signal"));

            assertThat(config.getHeaders(connectionDetails)).containsExactlyInAnyOrderEntriesOf(Map.of(
                    "Authorization", "from-common",
                    "X-Details", "details",
                    "X-Common", "from-signal"));
        }

    }

    @Nested
    class MetricsResolution {

        @Test
        void usesCommonMetrics() {
            commonProperties.getOtlp().setMetrics(true);

            assertThat(config.isMetrics()).isTrue();
        }

        @Test
        void signalMetricsTakesPrecedence() {
            commonProperties.getOtlp().setMetrics(true);
            signalProperties.setMetrics(false);

            assertThat(config.isMetrics()).isFalse();
        }

    }

    @Nested
    class LoggableUrl {

        @Test
        void keepsSchemeHostPortAndPath() {
            assertThat(EffectiveOtlpExporterConfig.toLoggableUrl("https://otlp.example.com:4318/v1/traces"))
                    .isEqualTo("https://otlp.example.com:4318/v1/traces");
        }

        @Test
        void removesUserInfoQueryAndFragment() {
            assertThat(EffectiveOtlpExporterConfig.toLoggableUrl("https://user:secret@otlp.example.com/v1/traces?api-key=secret#part"))
                    .isEqualTo("https://otlp.example.com/v1/traces");
        }

        @Test
        void hidesUnparseableUrl() {
            assertThat(EffectiveOtlpExporterConfig.toLoggableUrl("not a url")).isEqualTo("<unparseable URL>");
            assertThat(EffectiveOtlpExporterConfig.toLoggableUrl("otlp.example.com:4317")).isEqualTo("<unparseable URL>");
        }

    }

    @Test
    void usesCommonMemoryMode() {
        commonProperties.setMemoryMode(MemoryMode.IMMUTABLE_DATA);

        assertThat(config.getMemoryMode()).isEqualTo(MemoryMode.IMMUTABLE_DATA);
    }

}
