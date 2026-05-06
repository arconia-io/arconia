package io.arconia.dev.services.garage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GarageSpringCloudAwsDevServicesAutoConfiguration}.
 */
class GarageSpringCloudAwsDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(
                    GarageDevServicesAutoConfiguration.class,
                    GarageSpringCloudAwsDevServicesAutoConfiguration.class));

    @Test
    void pathStyleAccessDefaultedWhenSpringCloudAwsOnClasspath() {
        contextRunner.run(context -> assertThat(context.getEnvironment()
                .getProperty("spring.cloud.aws.s3.path-style-access-enabled"))
                .isEqualTo("true"));
    }

    @Test
    void pathStyleAccessOverridableByUser() {
        contextRunner
                .withPropertyValues("spring.cloud.aws.s3.path-style-access-enabled=false")
                .run(context -> assertThat(context.getEnvironment()
                        .getProperty("spring.cloud.aws.s3.path-style-access-enabled"))
                        .isEqualTo("false"));
    }

    @Test
    void autoConfigurationAbsentWhenSpringCloudAwsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(RestartScope.class, io.awspring.cloud.autoconfigure.s3.properties.S3Properties.class))
                .withConfiguration(AutoConfigurations.of(
                        GarageDevServicesAutoConfiguration.class,
                        GarageSpringCloudAwsDevServicesAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(
                        GarageSpringCloudAwsDevServicesAutoConfiguration.class));
    }

}
