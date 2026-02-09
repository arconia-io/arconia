package io.arconia.dev.services.mariadb;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationIT;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mariadb.MariaDBContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static io.arconia.dev.services.mariadb.MariaDbDevServicesProperties.DEFAULT_DB_NAME;
import static io.arconia.dev.services.mariadb.MariaDbDevServicesProperties.DEFAULT_PASSWORD;
import static io.arconia.dev.services.mariadb.MariaDbDevServicesProperties.DEFAULT_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MariaDbDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class MariaDbDevServicesAutoConfigurationIT extends BaseJdbcDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(MariaDbDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return MariaDbDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends JdbcDatabaseContainer<?>> getContainerClass() {
        return MariaDBContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "mariadb";
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        getContextRunner().run(context -> {
            assertThat(context).hasSingleBean(getContainerClass());
            var container = context.getBean(getContainerClass());
            assertThat(container.getDockerImageName()).contains(ArconiaMariaDbContainer.COMPATIBLE_IMAGE_NAME);
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
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties(), commonJdbcConfigurationProperties());

        getContextRunner()
                .withPropertyValues(properties)
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    assertThatJdbcConfigurationIsApplied(container);
                    assertThat(container.execInContainer("mariadb", "-u", "mytest", "-pmytest", "mytest", "-N", "-e",
                            "SELECT IF(EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'mytest' AND table_name = 'BOOK'), 'true', 'false')")
                            .getStdout())
                            .contains("true");
                    container.stop();
                });
    }

}
