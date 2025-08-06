package io.arconia.dev.services.mysql;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MySqlDevServicesAutoConfiguration}.
 */
class MySqlDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(MySqlDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.mysql.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(MySQLContainer.class));
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MySQLContainer.class);
            MySQLContainer<?> container = context.getBean(MySQLContainer.class);
            assertThat(container.getDockerImageName()).contains("mysql");
            assertThat(container.isShouldBeReused()).isFalse();
        });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.mysql.image-name=docker.io/mysql",
                "arconia.dev.services.mysql.environment.MYSQL_USER=test",
                "arconia.dev.services.mysql.shared=never"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(MySQLContainer.class);
                MySQLContainer<?> container = context.getBean(MySQLContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/mysql");
                assertThat(container.getEnv()).contains("MYSQL_USER=test");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
