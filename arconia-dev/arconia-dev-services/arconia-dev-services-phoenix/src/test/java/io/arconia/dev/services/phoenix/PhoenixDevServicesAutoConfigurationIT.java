package io.arconia.dev.services.phoenix;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.DevServiceLabels;
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
    protected boolean supportsSharing() {
        return true;
    }

    @Override
    protected GenericContainer<?> createSharedContainer(String ownerId) {
        PhoenixDevServicesProperties properties = new PhoenixDevServicesProperties();
        return withSharedLabels(new PhoenixContainer(DockerImageName.parse(properties.getImageName())), ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> sharedContainer) {
        OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF)).endsWith(
                ":%d%s".formatted(sharedContainer.getMappedPort(PhoenixContainer.HTTP_PORT),
                        OtlpTracingConnectionDetails.TRACES_PATH));
        assertThat(connectionDetails.getTracesUrl(Protocol.GRPC)).endsWith(
                ":" + sharedContainer.getMappedPort(PhoenixContainer.GRPC_PORT));
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
                    assertThat(container.isShouldBeReused()).isFalse();
                    assertThat(container.getLabels())
                            .containsEntry(DevServiceLabels.NAME, "phoenix")
                            .containsEntry(DevServiceLabels.SHARED, "true")
                            .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void devServiceLinksExposePhoenixUi() {
        getContextRunner().run(context -> {
            var container = context.getBean(getContainerClass());
            container.start();
            List<DevServiceLink> links = ((DevServiceLinkProvider) container).devServiceLinks();
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
