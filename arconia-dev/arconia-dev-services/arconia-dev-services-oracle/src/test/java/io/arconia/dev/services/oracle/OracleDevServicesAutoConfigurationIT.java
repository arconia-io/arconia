package io.arconia.dev.services.oracle;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.oracle.OracleContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationIT;

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

                    String beanName = context.getBeanFactory().getBeanNamesForType(getContainerClass())[0];
                    BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition(beanName);
                    assertThat(beanDefinition).isInstanceOf(TestcontainerBeanDefinition.class);
                    assertThat(((TestcontainerBeanDefinition) beanDefinition).getAnnotations()
                            .isPresent(ServiceConnection.class)).isTrue();
                });
    }

}
