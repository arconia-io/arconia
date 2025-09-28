package io.arconia.dev.services.mysql;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MySqlDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
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
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.isShouldBeReused()).isFalse();
            container.start();
            assertThat(container.getUsername()).isEqualTo("test");
            assertThat(container.getPassword()).isEqualTo("test");
            assertThat(container.getDatabaseName()).isEqualTo("test");
        });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.mysql.image-name=docker.io/library/mysql:8.4",
                "arconia.dev.services.mysql.environment.KEY=value",
                "arconia.dev.services.mysql.shared=never",
                "arconia.dev.services.mysql.startup-timeout=90s",
                "arconia.dev.services.mysql.username=mytest",
                "arconia.dev.services.mysql.password=mytest",
                "arconia.dev.services.mysql.db-name=mytest",
                "arconia.dev.services.mysql.init-script-paths=sql/init.sql"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(MySQLContainer.class);
                MySQLContainer<?> container = context.getBean(MySQLContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/library/mysql:8.4");
                assertThat(container.getEnv()).contains("KEY=value");
                assertThat(container.isShouldBeReused()).isFalse();
                container.start();
                assertThat(container.getUsername()).isEqualTo("mytest");
                assertThat(container.getPassword()).isEqualTo("mytest");
                assertThat(container.getDatabaseName()).isEqualTo("mytest");
                assertThat(container.execInContainer("mysql", "-u", "mytest", "-pmytest", "mytest", "-N", "-e",
                        "SELECT IF(EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'mytest' AND table_name = 'BOOK'), 'true', 'false')")
                        .getStdout())
                        .contains("true");
            });
    }

}
