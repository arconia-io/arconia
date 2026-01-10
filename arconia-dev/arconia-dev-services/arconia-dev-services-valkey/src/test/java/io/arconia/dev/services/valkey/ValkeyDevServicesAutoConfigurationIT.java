package io.arconia.dev.services.valkey;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.testcontainers.valkey.ValkeyContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ValkeyDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class ValkeyDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(ValkeyDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.valkey.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ValkeyContainer.class));
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(ValkeyContainer.class);
                    ValkeyContainer container = context.getBean(ValkeyContainer.class);
                    assertThat(container.getDockerImageName()).contains("ghcr.io/valkey-io/valkey");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
                .withPropertyValues(
                        "arconia.dev.services.valkey.environment.KEY=value",
                        "arconia.dev.services.valkey.shared=never",
                        "arconia.dev.services.valkey.startup-timeout=90s"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ValkeyContainer.class);
                    ValkeyContainer container = context.getBean(ValkeyContainer.class);
                    assertThat(container.getEnv()).contains("KEY=value");
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(ValkeyContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(ValkeyContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
