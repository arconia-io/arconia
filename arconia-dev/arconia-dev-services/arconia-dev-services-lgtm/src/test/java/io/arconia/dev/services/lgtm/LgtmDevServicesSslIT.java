package io.arconia.dev.services.lgtm;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocket;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;

import io.arconia.core.bootstrap.BootstrapModeDetector;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.dev.services.tests.SslTestCertificates;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.OpenTelemetryTracingExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Grafana LGTM Dev Service with SSL enabled: the application exports
 * telemetry to it over SSL through the service connection, with the bundle built from the properties.
 */
@DisabledIfDockerUnavailable
class LgtmDevServicesSslIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LgtmDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class, SslAutoConfiguration.class,
                    OpenTelemetryExporterAutoConfiguration.class, OpenTelemetryTracingExporterAutoConfiguration.class))
            .withPropertyValues("arconia.dev.services.lgtm.skip-in-tests=false")
            .withInitializer(new TestcontainersLifecycleApplicationContextInitializer());

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapModeDetector.clearCache();
    }

    @Test
    void applicationExportsTracesOverSsl() throws Exception {
        contextRunner
                .withPropertyValues(SslTestCertificates.pemProperties("lgtm"))
                .run(context -> {
                    OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
                    assertThat(connectionDetails.getSslBundle()).isNotNull();
                    assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF)).startsWith("https://");
                    assertThat(exportSpan(context.getBean(OtlpHttpSpanExporter.class))).isTrue();
                    assertThat(context.getBean(DevServiceRegistration.class).getSslBundleName()).isEqualTo("dev-services-lgtm");

                    // The gRPC port serves SSL too.
                    URI grpcUrl = URI.create(connectionDetails.getTracesUrl(Protocol.GRPC));
                    assertThat(grpcUrl.getScheme()).isEqualTo("https");
                    SslBundle sslBundle = context.getBean(SslBundles.class).getBundle("dev-services-lgtm");
                    try (SSLSocket socket = (SSLSocket) sslBundle.createSslContext().getSocketFactory().createSocket(grpcUrl.getHost(), grpcUrl.getPort())) {
                        socket.startHandshake();
                        assertThat(socket.getSession().getProtocol()).startsWith("TLS");
                    }
                });
    }

    /**
     * Exports a span and reports whether the export succeeded, which requires an SSL handshake
     * with the dev service.
     */
    private static boolean exportSpan(SpanExporter exporter) {
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();
        tracerProvider.get("ssl-test").spanBuilder("ssl-test").startSpan().end();
        return tracerProvider.forceFlush().join(30, TimeUnit.SECONDS).isSuccess();
    }

}
