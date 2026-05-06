package io.arconia.dev.services.garage;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;
import io.arconia.testcontainers.garage.GarageContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link GarageDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class GarageDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(GarageDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return GarageDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return GarageContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "garage";
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        getContextRunner()
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaGarageContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isFalse();

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties());

        getContextRunner()
                .withPropertyValues(properties)
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    container.stop();
                });
    }

    @Test
    void awsConnectionDetailsBeanProvidedFromRunningContainer() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(RestartScope.class))
                .withConfiguration(AutoConfigurations.of(
                        GarageDevServicesAutoConfiguration.class,
                        ServiceConnectionAutoConfiguration.class))
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    try {
                        assertThat(context).hasSingleBean(AwsConnectionDetails.class);
                        var details = context.getBean(AwsConnectionDetails.class);
                        assertThat(details.getEndpoint()).isNotNull();
                        assertThat(details.getEndpoint().toString()).startsWith("http://");
                        assertThat(details.getRegion()).isEqualTo(GarageContainer.DEFAULT_REGION);
                        assertThat(details.getAccessKey()).isEqualTo(GarageContainer.DEFAULT_ACCESS_KEY);
                        assertThat(details.getSecretKey()).isEqualTo(GarageContainer.DEFAULT_SECRET_KEY);
                    } finally {
                        container.stop();
                    }
                });
    }

}
