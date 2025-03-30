package io.arconia.dev.services.postgresql;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PostgresqlDevServicesAutoConfiguration}.
 */
class PostgresqlDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(PostgresqlDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.postgresql.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(PostgreSQLContainer.class));
    }

    @Test
    void postgresContainerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PostgreSQLContainer.class);
            PostgreSQLContainer<?> container = context.getBean(PostgreSQLContainer.class);
            assertThat(container.getDockerImageName()).contains("postgres");
            assertThat(container.isShouldBeReused()).isFalse();
        });
    }

    @Test
    void postgresContainerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.postgresql.image-name=postgres:17.3-alpine",
                "arconia.dev.services.postgresql.reusable=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(PostgreSQLContainer.class);
                PostgreSQLContainer<?> container = context.getBean(PostgreSQLContainer.class);
                assertThat(container.getDockerImageName()).contains("postgres:17.3-alpine");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
