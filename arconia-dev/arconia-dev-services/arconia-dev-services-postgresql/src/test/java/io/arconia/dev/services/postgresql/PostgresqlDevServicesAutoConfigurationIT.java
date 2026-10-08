package io.arconia.dev.services.postgresql;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PostgresqlDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class PostgresqlDevServicesAutoConfigurationIT extends BaseJdbcDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(PostgresqlDevServicesAutoConfiguration.class)
            .withClassLoader(new FilteredClassLoader(PgVectorStore.class));

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

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return JdbcConnectionDetails.class;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new PostgresqlDevServicesProperties();
        return asDiscoverableContainer(new ArconiaPostgreSqlContainer(properties), properties, ownerId);
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
                // The default context runner hides PgVectorStore: make it visible again.
                .withClassLoader(getClass().getClassLoader())
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains("pgvector/pgvector");
                });
    }

}
