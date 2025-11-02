package io.arconia.dev.services.apachepulsar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApachePulsarDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class ApachePulsarDevServicesAutoConfigurationTests {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(ApachePulsarDevServicesAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.apachepulsar.enabled=false")
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
                "arconia.dev.services.apachepulsar.environment.KEY=value",
                "arconia.dev.services.apachepulsar.shared=never",
                "arconia.dev.services.apachepulsar.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(PulsarContainer.class);
                var container = context.getBean(PulsarContainer.class);
                assertThat(container.getEnv()).contains("KEY=value");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
