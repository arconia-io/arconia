package io.arconia.dev.services.phoenix;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
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
        return withDiscoveryLabels(new ArconiaPhoenixContainer(new PhoenixDevServicesProperties()), ownerId);
    }

    @Override
    protected List<DevServiceLinkDefinition> discoverableContainerLinkDefinitions() {
        return new ArconiaPhoenixContainer(new PhoenixDevServicesProperties()).devServiceLinkDefinitions();
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
    void containerAvailableInDevMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaPhoenixContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.getNetworkAliases()).hasSize(1);

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void devServiceLinksExposePhoenixUi() {
        getContextRunner().run(context -> {
            var container = context.getBean(getContainerClass());
            container.start();
            List<DevServiceLink> links = ((DevServiceLinkProvider) container).devServiceLinkDefinitions().stream()
                    .map(definition -> definition.toLink(container.getHost(), container.getMappedPort(definition.port())))
                    .toList();
            assertThat(links).singleElement().satisfies(link -> {
                assertThat(link.id()).isEqualTo("phoenix");
                assertThat(link.label()).isEqualTo("Phoenix UI");
                assertThat(link.url()).startsWith("http://")
                        .endsWith(":" + container.getMappedPort(PhoenixContainer.HTTP_PORT));
            });
            container.stop();
        });
    }

    @Test
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties());

        contextRunner
                .withPropertyValues(properties)
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    container.stop();
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
