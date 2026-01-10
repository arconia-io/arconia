package io.arconia.dev.services.pulsar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.pulsar.PulsarContainer;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PulsarDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class PulsarDevServicesAutoConfigurationIT {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(PulsarDevServicesAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.pulsar.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(PulsarContainer.class));
    }

    @Test
    void containerAvailableInDevelopmentMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(PulsarContainer.class);
                    var container = context.getBean(PulsarContainer.class);
                    assertThat(container.getDockerImageName()).contains("apachepulsar/pulsar");
                    assertThat(container.isShouldBeReused()).isTrue();
                });
    }

    @Test
    void containerAvailableInTestMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(PulsarContainer.class);
                    var container = context.getBean(PulsarContainer.class);
                    assertThat(container.getDockerImageName()).contains("apachepulsar/pulsar");
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.pulsar.environment.KEY=value",
                "arconia.dev.services.pulsar.shared=never",
                "arconia.dev.services.pulsar.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(PulsarContainer.class);
                var container = context.getBean(PulsarContainer.class);
                assertThat(container.getEnv()).contains("KEY=value");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(PulsarContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(PulsarContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
