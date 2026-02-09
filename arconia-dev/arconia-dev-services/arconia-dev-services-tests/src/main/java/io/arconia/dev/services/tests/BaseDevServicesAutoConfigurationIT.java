package io.arconia.dev.services.tests;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.SimpleThreadScope;
import org.testcontainers.containers.GenericContainer;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for integration tests of dev services auto-configuration.
 */
public abstract class BaseDevServicesAutoConfigurationIT {

    @TempDir
    protected static Path testMountDir;

    /**
     * The application context runner used to execute tests.
     */
    protected abstract ApplicationContextRunner getContextRunner();

    /**
     * The auto-configuration class for the Dev Service to test.
     */
    protected abstract Class<?> getAutoConfigurationClass();

    /**
     * The specific container bean class for the Dev Service to test.
     */
    protected abstract Class<? extends GenericContainer<?>> getContainerClass();

    /**
     * The name of the Dev Service to test.
     */
    protected abstract String getServiceName();

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenGloballyDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(getContainerClass()));
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.dev.services.%s.enabled=false".formatted(getServiceName()))
                .run(context -> assertThat(context).doesNotHaveBean(getContainerClass()));
    }

    @Test
    void containerAvailableInTestMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerWithRestartScope() {
        getContextRunner()
                .withClassLoader(this.getClass().getClassLoader())
                .withInitializer(context ->
                        context.getBeanFactory().registerScope("restart", new SimpleThreadScope()))
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(getContainerClass());
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

    /**
     * Assert that the given container class is instantiated as a singleton bean in the given application context.
     */
    protected void assertThatHasSingletonScope(AssertableApplicationContext context) {
        String[] beanNames = context.getBeanFactory().getBeanNamesForType(getContainerClass());
        assertThat(beanNames).hasSize(1);
        assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                .isEqualTo("singleton");
    }

    /**
     * Build common configuration properties for a service.
     */
    protected String[] commonConfigurationProperties() {
        String prefix = "arconia.dev.services." + getServiceName();
        return new String[] {
                prefix + ".environment.KEY=value",
                prefix + ".network-aliases=network1",
                prefix + ".resources[0].source-path=test-resource.txt",
                prefix + ".resources[0].container-path=/tmp/test-resource.txt",
                prefix + ".volumes[0].host-path=" + testMountDir.toAbsolutePath(),
                prefix + ".volumes[0].container-path=/arconia"
        };
    }

    /**
     * Assert common configuration properties were applied correctly.
     * Container must be started before calling.
     */
    protected static void assertThatConfigurationIsApplied(GenericContainer<?> container) throws Exception {
        assertThat(container.getEnv()).contains("KEY=value");
        assertThat(container.getNetworkAliases()).contains("network1");
        assertThat(container.getCurrentContainerInfo().getState().getStatus()).isEqualTo("running");

        String mappedResourceContent = container.copyFileFromContainer(
                "/tmp/test-resource.txt",
                inputStream -> new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
        );
        assertThat(mappedResourceContent).isNotEmpty();

        assertThat(container.getBinds().getFirst().getPath()).isEqualTo(testMountDir.toAbsolutePath().toString());
        assertThat(container.getBinds().getFirst().getVolume().getPath()).isEqualTo("/arconia");
    }

    /**
     * Builds a default ApplicationContextRunner for testing auto-configuration with the given auto-configuration class.
     */
    protected static ApplicationContextRunner defaultContextRunner(Class<?> autoConfigurationClass) {
        return new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(RestartScope.class))
                .withConfiguration(AutoConfigurations.of(autoConfigurationClass));
    }

}
