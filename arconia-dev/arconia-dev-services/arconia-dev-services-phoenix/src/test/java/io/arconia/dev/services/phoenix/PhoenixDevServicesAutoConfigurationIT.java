package io.arconia.dev.services.phoenix;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PhoenixDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class PhoenixDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(PhoenixDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return PhoenixDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return PhoenixContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "phoenix";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return OtlpTracingConnectionDetails.class;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new PhoenixDevServicesProperties();
        return asDiscoverableContainer(new ArconiaPhoenixContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        PhoenixContainer container = (PhoenixContainer) discoveredContainer;
        OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF))
                .isEqualTo("http://%s:%d".formatted(container.getHost(), container.getHttpPort()) + OtlpTracingConnectionDetails.TRACES_PATH);
        assertThat(connectionDetails.getTracesUrl(Protocol.GRPC))
                .isEqualTo("http://%s:%d".formatted(container.getHost(), container.getGrpcPort()));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.otel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(PhoenixContainer.class));
    }

    @Test
    void devServiceLinksExposePhoenixUi() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var container = context.getBean(getContainerClass());
            List<DevServiceLink> links = context.getBean(DevServiceRegistration.class).links();
            assertThat(links).singleElement().satisfies(link -> {
                assertThat(link.label()).isEqualTo("Phoenix UI");
                assertThat(link.url()).startsWith("http://")
                        .endsWith(":" + container.getMappedPort(PhoenixContainer.HTTP_PORT));
            });
        });
    }

    @Test
    void customDefaultPropertiesConfiguredWhenNotOverridden() {
        getContextRunner()
                .run(context -> {
                    var loggingExporterType = context.getEnvironment().getProperty(
                            OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type");
                    var metricsExporterType = context.getEnvironment().getProperty(
                            OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type");

                    assertThat(loggingExporterType).isEqualTo("none");
                    assertThat(metricsExporterType).isEqualTo("none");
                });
    }

    @Test
    void customDefaultPropertiesNotConfiguredWhenOverridden() {
        getContextRunner()
                .withPropertyValues(
                        "arconia.otel.logs.exporter.type=console",
                        "arconia.otel.metrics.exporter.type=console"
                )
                .run(context -> {
                    var loggingExporterType = context.getEnvironment().getProperty(
                            OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type");
                    var metricsExporterType = context.getEnvironment().getProperty(
                            OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type");

                    assertThat(loggingExporterType).isEqualTo("console");
                    assertThat(metricsExporterType).isEqualTo("console");
                });
    }

}
