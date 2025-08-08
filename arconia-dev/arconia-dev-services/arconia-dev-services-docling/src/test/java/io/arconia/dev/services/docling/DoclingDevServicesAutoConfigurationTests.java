package io.arconia.dev.services.docling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DoclingDevServicesAutoConfiguration}.
 */
class DoclingDevServicesAutoConfigurationTests {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(DoclingDevServicesAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.docling.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean("doclingContainer"));
    }

    @Test
    void containerAvailableInDevelopmentMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(GenericContainer.class);
                    GenericContainer<?> container = context.getBean("doclingContainer", GenericContainer.class);
                    assertThat(container.getDockerImageName()).contains("ghcr.io/docling-project/docling-serve");
                    assertThat(container.isShouldBeReused()).isTrue();
                });
    }

    @Test
    void containerAvailableInTestMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(GenericContainer.class);
                    GenericContainer<?> container = context.getBean("doclingContainer", GenericContainer.class);
                    assertThat(container.getDockerImageName()).contains("ghcr.io/docling-project/docling-serve");
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.docling.image-name=ghcr.io/docling-project/docling-serve:latest",
                "arconia.dev.services.docling.environment.DOCLING_SERVE_ENABLE_REMOTE_SERVICES=true",
                "arconia.dev.services.docling.shared=never"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(GenericContainer.class);
                GenericContainer<?> container = context.getBean("doclingContainer", GenericContainer.class);
                assertThat(container.getExposedPorts()).contains(DoclingDevServicesAutoConfiguration.DEFAULT_PORT);
                assertThat(container.getDockerImageName()).contains("ghcr.io/docling-project/docling-serve");
                assertThat(container.getEnv()).contains("DOCLING_SERVE_ENABLE_REMOTE_SERVICES=true");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void containerHasDoclingUiEnabledByDefault() {
        contextRunner
            .run(context -> {
                assertThat(context).hasSingleBean(GenericContainer.class);
                GenericContainer<?> container = context.getBean("doclingContainer", GenericContainer.class);
                assertThat(container.getEnv()).contains("DOCLING_SERVE_ENABLE_UI=1");
            });
    }

    @Test
    void containerCanDisableDoclingUi() {
        contextRunner
            .withPropertyValues("arconia.dev.services.docling.enable-ui=false")
            .run(context -> {
                assertThat(context).hasSingleBean(GenericContainer.class);
                GenericContainer<?> container = context.getBean("doclingContainer", GenericContainer.class);
                assertThat(container.getEnv()).contains("DOCLING_SERVE_ENABLE_UI=0");
            });
    }

    @Test
    void containerCanExplicitlyEnableDoclingUi() {
        contextRunner
            .withPropertyValues("arconia.dev.services.docling.enable-ui=true")
            .run(context -> {
                assertThat(context).hasSingleBean(GenericContainer.class);
                GenericContainer<?> container = context.getBean("doclingContainer", GenericContainer.class);
                assertThat(container.getEnv()).contains("DOCLING_SERVE_ENABLE_UI=1");
            });
    }

}
