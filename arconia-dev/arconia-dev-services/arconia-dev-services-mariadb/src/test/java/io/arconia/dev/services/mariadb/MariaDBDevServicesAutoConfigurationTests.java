package io.arconia.dev.services.mariadb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.MariaDBContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MariaDBDevServicesAutoConfiguration}.
 */
class MariaDBDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(MariaDBDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.mariadb.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(MariaDBContainer.class));
    }

    @Test
    void mariadbContainerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MariaDBContainer.class);
            MariaDBContainer<?> container = context.getBean(MariaDBContainer.class);
            assertThat(container.getDockerImageName()).contains("mariadb");
            assertThat(container.isShouldBeReused()).isFalse();
        });
    }

    @Test
    void mariadbContainerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.mariadb.image-name=docker.io/mariadb",
                "arconia.dev.services.mariadb.environment.MARIADB_USER=test",
                "arconia.dev.services.mariadb.shared=never"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(MariaDBContainer.class);
                MariaDBContainer<?> container = context.getBean(MariaDBContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/mariadb");
                assertThat(container.getEnv()).contains("MARIADB_USER=test");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
