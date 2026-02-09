package io.arconia.dev.services.artemis;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ArtemisDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class ArtemisDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private static final ApplicationContextRunner contextRunner = defaultContextRunner(ArtemisDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return ArtemisDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return ArtemisContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "artemis";
    }

    @Test
    void containerAvailableInDevMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = (ArtemisContainer) context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaArtemisContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isTrue();
                    assertThat(container.getBinds()).isEmpty();
                    container.start();
                    assertThat(container.getUser()).isEqualTo(ArtemisDevServicesProperties.DEFAULT_USERNAME);
                    assertThat(container.getPassword()).isEqualTo(ArtemisDevServicesProperties.DEFAULT_PASSWORD);
                    container.stop();

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties(),
                "arconia.dev.services.%s.username=myusername".formatted(getServiceName()),
                "arconia.dev.services.%s.password=mypassword".formatted(getServiceName())
        );

        getContextRunner()
                .withPropertyValues(properties)
                .run(context -> {
                    var container = (ArtemisContainer) context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    assertThat(container.getUser()).isEqualTo("myusername");
                    assertThat(container.getPassword()).isEqualTo("mypassword");
                    container.stop();
                });
    }

}
