package io.arconia.dev.ui.server.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;

import io.arconia.dev.ui.server.error.DevUiErrorViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiErrorPagesAutoConfiguration}.
 */
class DevUiErrorPagesAutoConfigurationTests {

    private static final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
                    ErrorMvcAutoConfiguration.class, DevUiAutoConfiguration.class, DevUiErrorPagesAutoConfiguration.class));

    @Test
    void errorPagesAreAvailableWhenDevModeIsEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> assertThat(context).hasSingleBean(DevUiErrorViewResolver.class));
    }

    @Test
    void errorPagesAreNotAvailableWhenTestModeIsEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> assertThat(context).doesNotHaveBean(DevUiErrorViewResolver.class));
    }

    @Test
    void errorPagesAreNotAvailableWhenProdModeIsEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=prod")
                .run(context -> assertThat(context).doesNotHaveBean(DevUiErrorViewResolver.class));
    }

    @Test
    void errorPagesAreNotAvailableWhenTheDevUiIsDisabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.ui.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DevUiErrorViewResolver.class));
    }

    @Test
    void errorPagesAreNotAvailableWhenDisabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.ui.error-pages.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DevUiErrorViewResolver.class);
                    assertThat(context).hasBean("devUiServlet");
                });
    }

    @Test
    void errorPagesAreNotAvailableWithoutTheErrorHandlingOfSpringMvc() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withClassLoader(new FilteredClassLoader(ErrorViewResolver.class))
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean("devUiErrorViewResolver");
                    assertThat(context).hasBean("devUiServlet");
                });
    }

}
