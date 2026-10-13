package io.arconia.dev.services.lgtm;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.observation.opentelemetry.autoconfigure.JvmConventionsAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link LgtmDashboards}.
 */
class LgtmDashboardsTests {

    private static final String CONTRIBUTED_DASHBOARD = "spring-boot.json";

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse(new LgtmDevServicesProperties().getImageName())
            .asCompatibleSubstituteFor(LgtmDevServicesAutoConfiguration.COMPATIBLE_IMAGE_NAME);

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    private final LgtmDevServicesProperties.Dashboards options = new LgtmDevServicesProperties.Dashboards();

    @Nested
    class BuiltInDashboards {

        @Test
        void openTelemetryJvmDashboardWhenOpenTelemetryConventionsPresent() {
            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.dashboards().keySet())
                    .contains("jvm-opentelemetry.json")
                    .doesNotContain("jvm-micrometer.json");
        }

        @Test
        void micrometerJvmDashboardWhenOpenTelemetryConventionsAbsent() {
            var resourceLoader = new DefaultResourceLoader(new FilteredClassLoader(JvmConventionsAutoConfiguration.class));

            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.dashboards().keySet())
                    .contains("jvm-micrometer.json")
                    .doesNotContain("jvm-opentelemetry.json");
        }

        @Test
        void micrometerJvmDashboardWhenOpenTelemetryJvmConventionsDisabled() {
            var environment = new MockEnvironment()
                    .withProperty("arconia.observations.conventions.opentelemetry.jvm.enabled", "false");

            var dashboards = LgtmDashboards.collect(environment, resourceLoader, options);

            assertThat(dashboards.dashboards().keySet())
                    .contains("jvm-micrometer.json")
                    .doesNotContain("jvm-opentelemetry.json");
        }

        @Test
        void loggingDashboardWhenLogbackPresent() {
            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.dashboards().keySet()).contains("logging.json");
        }

        @Test
        void noLoggingDashboardWhenLogbackAbsent() {
            var resourceLoader = new DefaultResourceLoader(new FilteredClassLoader("ch.qos.logback.classic"));

            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.dashboards().keySet()).doesNotContain("logging.json");
        }

        @Test
        void builtInDashboardsAreReadable() {
            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.dashboards().values())
                    .filteredOn(resource -> resource instanceof ClassPathResource)
                    .isNotEmpty()
                    .allSatisfy(resource -> assertThat(resource.exists()).isTrue());
        }

    }

    @Nested
    class ContributedDashboards {

        @Test
        void dashboardsUnderConventionalLocationAreCollected() {
            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.dashboards()).containsKey(CONTRIBUTED_DASHBOARD);
            assertThat(dashboards.dashboards().get(CONTRIBUTED_DASHBOARD).getFilename()).isEqualTo(CONTRIBUTED_DASHBOARD);
        }

        @Test
        void duplicateFileNamesKeepTheFirstDashboard(@TempDir Path tempDir) throws Exception {
            var first = new ClassPathResource(LgtmDashboards.CONTRIBUTED_LOCATION + "/" + CONTRIBUTED_DASHBOARD);
            var duplicate = new FileSystemResource(Files.writeString(tempDir.resolve(CONTRIBUTED_DASHBOARD), "{}"));
            var resourceLoader = new PathMatchingResourcePatternResolver() {
                @Override
                public Resource[] getResources(String locationPattern) {
                    return new Resource[] { first, duplicate };
                }
            };

            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.dashboards()).containsEntry(CONTRIBUTED_DASHBOARD, first);
        }

    }

    @Nested
    class Switches {

        @Test
        void provisioningEnabledByDefault() {
            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.enabled()).isTrue();
            assertThat(dashboards.dashboards()).isNotEmpty();
        }

        @Test
        void nothingCollectedWhenDisabled() {
            options.setEnabled(false);

            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.enabled()).isFalse();
            assertThat(dashboards.dashboards()).isEmpty();
        }

        @Test
        void onlyContributedDashboardsWhenBuiltInDisabled() {
            options.getBuiltIn().setEnabled(false);

            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.enabled()).isTrue();
            assertThat(dashboards.dashboards().keySet()).containsExactly(CONTRIBUTED_DASHBOARD);
        }

    }

    @Nested
    class HomeDashboard {

        @Test
        void noHomeDashboardByDefault() {
            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.home()).isNull();
            try (var container = new LgtmStackContainer(IMAGE_NAME)) {
                dashboards.applyTo(container);
                assertThat(container.getEnvMap()).doesNotContainKey(LgtmDashboards.HOME_DASHBOARD_ENV);
            }
        }

        @Test
        void homeDashboardPointsAtProvisionedFile() {
            options.setHome(CONTRIBUTED_DASHBOARD);

            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.home()).isEqualTo(CONTRIBUTED_DASHBOARD);
            try (var container = new LgtmStackContainer(IMAGE_NAME)) {
                dashboards.applyTo(container);
                assertThat(container.getEnvMap()).containsEntry(LgtmDashboards.HOME_DASHBOARD_ENV,
                        LgtmDashboards.CONTAINER_DASHBOARDS_PATH + "/" + CONTRIBUTED_DASHBOARD);
            }
        }

        @Test
        void homeDashboardMustBeProvisioned() {
            options.setHome("missing.json");

            assertThatThrownBy(() -> LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("missing.json")
                    .hasMessageContaining(CONTRIBUTED_DASHBOARD);
        }

        @Test
        void homeDashboardIgnoredWhenDisabled() {
            options.setEnabled(false);
            options.setHome("missing.json");

            var dashboards = LgtmDashboards.collect(new MockEnvironment(), resourceLoader, options);

            assertThat(dashboards.home()).isNull();
        }

    }

}
