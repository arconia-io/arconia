package io.arconia.dev.services.openlit;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.openlit.OpenLitContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenLitDevServicesAutoConfiguration}.
 */
class OpenLitDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    OpenLitDevServicesAutoConfigurationTests() {
        super(OpenLitDevServicesAutoConfiguration.class, "openlit", OpenLitContainer.class, OtlpTracingConnectionDetails.class);
    }

    @Override
    protected boolean isSkippedInTestsByDefault() {
        return true;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new OpenLitDevServicesProperties();
        return asDiscoverableContainer(OpenLitDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected Map<String, Integer> additionalFixedPortProperties() {
        return Map.of(
                "otlp-grpc-port", OpenLitContainer.OTLP_GRPC_PORT,
                "otlp-http-port", OpenLitContainer.OTLP_HTTP_PORT);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        assertThat(context).hasSingleBean(OtlpMetricsConnectionDetails.class);
        assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);
        OpenLitContainer container = (OpenLitContainer) discoveredContainer;
        OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF))
                .isEqualTo(container.getOtlpHttpUrl() + OtlpTracingConnectionDetails.TRACES_PATH);
        assertThat(connectionDetails.getTracesUrl(Protocol.GRPC)).isEqualTo(container.getOtlpGrpcUrl());
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.otel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OpenLitContainer.class));
    }

    @Test
    @DisabledIfDockerUnavailable
    void devServiceLinksExposeOpenLitUi() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var container = context.getBean(getContainerClass());
            List<DevServiceLink> links = context.getBean(DevServiceRegistration.class).getLinks();
            assertThat(links).singleElement().satisfies(link -> {
                assertThat(link.label()).isEqualTo("OpenLIT UI");
                assertThat(link.url()).startsWith("http://" + container.getHost() + ":");
            });
        });
    }

}
