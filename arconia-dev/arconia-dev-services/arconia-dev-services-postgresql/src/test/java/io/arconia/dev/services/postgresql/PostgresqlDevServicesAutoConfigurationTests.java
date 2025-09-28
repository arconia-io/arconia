package io.arconia.dev.services.postgresql;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PostgresqlDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
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
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PostgreSQLContainer.class);
            PostgreSQLContainer<?> container = context.getBean(PostgreSQLContainer.class);
            assertThat(container.getDockerImageName()).contains("postgres");
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
                "arconia.dev.services.postgresql.environment.POSTGRES_USER=postgres",
                "arconia.dev.services.postgresql.shared=never",
                "arconia.dev.services.postgresql.startup-timeout=90s",
                "arconia.dev.services.postgresql.username=mytest",
                "arconia.dev.services.postgresql.password=mytest",
                "arconia.dev.services.postgresql.db-name=mytest",
                "arconia.dev.services.postgresql.init-script-paths=sql/init.sql"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(PostgreSQLContainer.class);
                PostgreSQLContainer<?> container = context.getBean(PostgreSQLContainer.class);
                assertThat(container.getEnv()).contains("POSTGRES_USER=postgres");
                assertThat(container.isShouldBeReused()).isFalse();
                container.start();
                assertThat(container.getUsername()).isEqualTo("mytest");
                assertThat(container.getPassword()).isEqualTo("mytest");
                assertThat(container.getDatabaseName()).isEqualTo("mytest");
                assertThat(container.execInContainer("psql", "-U", "mytest", "-d", "mytest", "-t", "-A", "-c",
                        "SELECT EXISTS (SELECT FROM pg_tables WHERE tablename = 'book')::text")
                        .getStdout())
                        .contains("true");
            });
    }

}
