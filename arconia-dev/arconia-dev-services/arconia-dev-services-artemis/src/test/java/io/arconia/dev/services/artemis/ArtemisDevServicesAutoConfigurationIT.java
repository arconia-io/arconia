package io.arconia.dev.services.artemis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ArtemisDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class ArtemisDevServicesAutoConfigurationIT {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(ArtemisDevServicesAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.artemis.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ArtemisContainer.class));
    }

    @Test
    void containerAvailableInDevelopmentMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(ArtemisContainer.class);
                    ArtemisContainer container = context.getBean(ArtemisContainer.class);
                    assertThat(container.getDockerImageName()).contains("apache/activemq-artemis");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.isShouldBeReused()).isTrue();
                    container.start();
                    assertThat(container.getUser()).isEqualTo(ArtemisDevServicesProperties.DEFAULT_USERNAME);
                    assertThat(container.getPassword()).isEqualTo(ArtemisDevServicesProperties.DEFAULT_PASSWORD);
                });
    }

    @Test
    void containerAvailableInTestMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(ArtemisContainer.class);
                    ArtemisContainer container = context.getBean(ArtemisContainer.class);
                    assertThat(container.getDockerImageName()).contains("apache/activemq-artemis");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.isShouldBeReused()).isFalse();
                    container.start();
                    assertThat(container.getUser()).isEqualTo(ArtemisDevServicesProperties.DEFAULT_USERNAME);
                    assertThat(container.getPassword()).isEqualTo(ArtemisDevServicesProperties.DEFAULT_PASSWORD);
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
                .withPropertyValues(
                        "arconia.dev.services.artemis.environment.ANONYMOUS_LOGIN=true",
                        "arconia.dev.services.artemis.shared=never",
                        "arconia.dev.services.artemis.startup-timeout=90s",
                        "arconia.dev.services.artemis.username=myusername",
                        "arconia.dev.services.artemis.password=mypassword"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ArtemisContainer.class);
                    ArtemisContainer container = context.getBean(ArtemisContainer.class);
                    assertThat(container.getEnv()).contains("ANONYMOUS_LOGIN=true");
                    assertThat(container.isShouldBeReused()).isFalse();
                    container.start();
                    assertThat(container.getUser()).isEqualTo("myusername");
                    assertThat(container.getPassword()).isEqualTo("mypassword");
                });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(ArtemisContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(ArtemisContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
