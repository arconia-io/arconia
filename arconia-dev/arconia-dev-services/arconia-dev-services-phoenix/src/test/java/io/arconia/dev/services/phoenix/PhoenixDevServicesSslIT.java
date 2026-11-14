package io.arconia.dev.services.phoenix;

import java.util.concurrent.TimeUnit;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PropertiesSslBundle;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;

import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.DevServiceContainers;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.dev.services.tests.SslTestCertificates;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.OpenTelemetryTracingExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Phoenix Dev Service with SSL enabled: the application exports traces
 * to it over SSL through the service connection, with the bundle built from the properties.
 */
@DisabledIfDockerUnavailable
class PhoenixDevServicesSslIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PhoenixDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class, SslAutoConfiguration.class,
                    OpenTelemetryExporterAutoConfiguration.class, OpenTelemetryTracingExporterAutoConfiguration.class))
            .withPropertyValues("arconia.dev.services.phoenix.skip-in-tests=false")
            .withInitializer(new TestcontainersLifecycleApplicationContextInitializer());

    @Test
    void applicationExportsTracesOverSsl() {
        contextRunner
                .withPropertyValues(SslTestCertificates.pemProperties("phoenix"))
                .run(context -> {
                    OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
                    assertThat(connectionDetails.getSslBundle()).isNotNull();
                    assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF)).startsWith("https://");
                    assertThat(exportSpan(context.getBean(OtlpHttpSpanExporter.class))).isTrue();
                    assertThat(context.getBean(DevServiceRegistration.class).getSslBundleName()).isEqualTo("dev-services-phoenix");
                });
    }

    @Test
    void applicationExportsTracesToDiscoveredContainerOverSsl() {
        // A container started by another application with the same SSL configuration.
        var properties = new PhoenixDevServicesProperties();
        properties.getSsl().setEnabled(true);
        properties.getSsl().getPem().getKeystore().setCertificate(SslTestCertificates.CERTIFICATE);
        properties.getSsl().getPem().getKeystore().setPrivateKey(SslTestCertificates.PRIVATE_KEY);
        PhoenixContainer peer = DevServiceContainers.discoverable(PhoenixDevServicesAutoConfiguration.createContainer(properties), properties, "phoenix", "another-application");
        PhoenixDevServicesAutoConfiguration.configureSsl(peer, sslBundle());
        try (peer) {
            peer.start();
            contextRunner
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .withPropertyValues(SslTestCertificates.pemProperties("phoenix"))
                    .withPropertyValues("arconia.dev.services.phoenix.reuse-strategy=framework")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(PhoenixContainer.class);
                        assertThat(context.getBean(DevServiceRegistration.class).getOrigin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
                        OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
                        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF)).startsWith("https://");
                        assertThat(exportSpan(context.getBean(OtlpHttpSpanExporter.class))).isTrue();
                    });
        }
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

    private static SslBundle sslBundle() {
        PemSslBundleProperties properties = new PemSslBundleProperties();
        properties.getKeystore().setCertificate(SslTestCertificates.CERTIFICATE);
        properties.getKeystore().setPrivateKey(SslTestCertificates.PRIVATE_KEY);
        return PropertiesSslBundle.get(properties);
    }

}
