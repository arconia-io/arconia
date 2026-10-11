package io.arconia.dev.services.postgresql;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PostgresqlDevServicesAutoConfiguration}.
 */
class PostgresqlDevServicesAutoConfigurationTests extends BaseJdbcDevServicesAutoConfigurationTests {

    PostgresqlDevServicesAutoConfigurationTests() {
        super(PostgresqlDevServicesAutoConfiguration.class, "postgresql", PostgreSQLContainer.class);
    }

    @Override
    protected ApplicationContextRunner getContextRunner() {
        // Hide PgVectorStore, so that the default image is the plain PostgreSQL one.
        return super.getContextRunner().withClassLoader(new FilteredClassLoader(PgVectorStore.class));
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
