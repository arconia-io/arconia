package io.arconia.dev.services.postgresql;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationIT;

import static io.arconia.dev.services.postgresql.PostgresqlDevServicesProperties.DEFAULT_DB_NAME;
import static io.arconia.dev.services.postgresql.PostgresqlDevServicesProperties.DEFAULT_PASSWORD;
import static io.arconia.dev.services.postgresql.PostgresqlDevServicesProperties.DEFAULT_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PostgresqlDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class PostgresqlDevServicesAutoConfigurationIT extends BaseJdbcDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(PostgresqlDevServicesAutoConfiguration.class)
            .withClassLoader(new FilteredClassLoader(RestartScope.class, PgVectorStore.class));

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return PostgresqlDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends JdbcDatabaseContainer<?>> getContainerClass() {
        return PostgreSQLContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "postgresql";
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        getContextRunner().run(context -> {
            assertThat(context).hasSingleBean(getContainerClass());
            var container = context.getBean(getContainerClass());
            assertThat(container.getDockerImageName()).contains(ArconiaPostgreSqlContainer.COMPATIBLE_IMAGE_NAME);
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
                assertThat(container.execInContainer("psql", "-U", "mytest", "-d", "mytest", "-t", "-A", "-c",
                    "SELECT EXISTS (SELECT FROM pg_tables WHERE tablename = 'book')::text")
                    .getStdout())
                    .contains("true");
                container.stop();
            });
    }

    @Test
    void pgVectorImageConfiguredWhenNotSpringAi() {
        getContextRunner()
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains("postgres");
                });
    }

    @Test
    void pgVectorImageConfiguredWhenSpringAi() {
        getContextRunner()
                .withClassLoader(new FilteredClassLoader(RestartScope.class))
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains("pgvector/pgvector");
                });
    }

}
