package io.arconia.dev.services.mysql;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mysql.MySQLContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationIT;

import static io.arconia.dev.services.mysql.MySqlDevServicesProperties.DEFAULT_DB_NAME;
import static io.arconia.dev.services.mysql.MySqlDevServicesProperties.DEFAULT_PASSWORD;
import static io.arconia.dev.services.mysql.MySqlDevServicesProperties.DEFAULT_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MySqlDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class MySqlDevServicesAutoConfigurationIT extends BaseJdbcDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(MySqlDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return MySqlDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends JdbcDatabaseContainer<?>> getContainerClass() {
        return MySQLContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "mysql";
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(getContainerClass());
            var container = context.getBean(getContainerClass());
            assertThat(container.getDockerImageName()).contains("mysql");
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
                    assertThat(container.execInContainer("mysql", "-u", "mytest", "-pmytest", "mytest", "-N", "-e",
                            "SELECT IF(EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'mytest' AND table_name = 'BOOK'), 'true', 'false')")
                            .getStdout())
                            .contains("true");
                    container.stop();
                });
    }

}
