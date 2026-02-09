package io.arconia.dev.services.oracle;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationIT;

import static io.arconia.dev.services.api.config.JdbcDevServicesProperties.DEFAULT_DB_NAME;
import static io.arconia.dev.services.api.config.JdbcDevServicesProperties.DEFAULT_PASSWORD;
import static io.arconia.dev.services.api.config.JdbcDevServicesProperties.DEFAULT_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OracleXeDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OracleXeDevServicesAutoConfigurationIT extends BaseJdbcDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(OracleXeDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return OracleXeDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends JdbcDatabaseContainer<?>> getContainerClass() {
        return OracleContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "oracle-xe";
    }

    @Test
    @Disabled("Too slow and heavy for the deployment pipeline. Also, it lacks ARM64 support.")
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(getContainerClass());
            var container = context.getBean(getContainerClass());
            assertThat(container.getDockerImageName()).contains(ArconiaOracleXeContainer.COMPATIBLE_IMAGE_NAME);
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.getNetworkAliases()).hasSize(1);
            assertThat(container.isShouldBeReused()).isFalse();
            container.start();
            assertThat(container.getUsername()).isEqualTo(DEFAULT_USERNAME);
            assertThat(container.getPassword()).isEqualTo(DEFAULT_PASSWORD);
            assertThat(container.getDatabaseName()).isEqualTo(DEFAULT_DB_NAME);
            container.stop();

            assertThatHasSingletonScope(context);
        });
    }

    @Test
    @Disabled("Too slow and heavy for the deployment pipeline. Also, it lacks ARM64 support.")
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties(), commonJdbcConfigurationProperties());

        getContextRunner()
                .withPropertyValues(properties)
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    assertThatJdbcConfigurationIsApplied(container);
                    container.stop();
                });
    }

}
