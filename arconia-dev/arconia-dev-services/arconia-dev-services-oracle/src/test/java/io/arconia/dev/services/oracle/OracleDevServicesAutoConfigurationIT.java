package io.arconia.dev.services.oracle;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.oracle.OracleContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationIT;

import static io.arconia.dev.services.oracle.OracleDevServicesProperties.DEFAULT_DB_NAME;
import static io.arconia.dev.services.oracle.OracleDevServicesProperties.DEFAULT_PASSWORD;
import static io.arconia.dev.services.oracle.OracleDevServicesProperties.DEFAULT_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OracleDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OracleDevServicesAutoConfigurationIT extends BaseJdbcDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(OracleDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return OracleDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends JdbcDatabaseContainer<?>> getContainerClass() {
        return OracleContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "oracle";
    }

    /**
     * Verifies the container bean and its {@code @ServiceConnection} wiring without starting the
     * heavyweight Oracle container: the container bean is registered (but not started) and its bean
     * definition carries the {@code @ServiceConnection} annotation, so Spring Boot would produce a
     * {@code JdbcConnectionDetails}.
     */
    @Test
    void containerAndServiceConnectionWiredWithoutStartingContainer() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaOracleContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.isRunning()).isFalse();
                    assertThatHasSingletonScope(context);

                    String beanName = context.getBeanFactory().getBeanNamesForType(getContainerClass())[0];
                    BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition(beanName);
                    assertThat(beanDefinition).isInstanceOf(TestcontainerBeanDefinition.class);
                    assertThat(((TestcontainerBeanDefinition) beanDefinition).getAnnotations()
                            .isPresent(ServiceConnection.class)).isTrue();
                });
    }

    @Test
    @Disabled("Too slow and heavy for the deployment pipeline.")
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(getContainerClass());
            var container = context.getBean(getContainerClass());
            assertThat(container.getDockerImageName()).contains(ArconiaOracleContainer.COMPATIBLE_IMAGE_NAME);
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
    @Disabled("Too slow and heavy for the deployment pipeline.")
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties(), commonJdbcConfigurationProperties());

        contextRunner
                .withPropertyValues(properties)
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    assertThatJdbcConfigurationIsApplied(container);
                    container.stop();
                });
    }

}
