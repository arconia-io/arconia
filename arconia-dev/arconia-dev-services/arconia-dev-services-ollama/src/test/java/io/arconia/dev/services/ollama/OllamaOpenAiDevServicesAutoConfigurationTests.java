package io.arconia.dev.services.ollama;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.dev.services.core.registration.DevServiceDynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OllamaOpenAiDevServicesAutoConfiguration}.
 */
class OllamaOpenAiDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(
                    OllamaDevServicesAutoConfiguration.class,
                    OllamaOpenAiDevServicesAutoConfiguration.class))
            .withPropertyValues("arconia.dev.services.ollama.ignore-native-service=true");

    @Test
    void propertySourceRegistered() {
        contextRunner
                .run(context -> assertThat(context.getEnvironment().getPropertySources()
                        .contains(DevServiceDynamicPropertySource.PROPERTY_SOURCE_NAME)).isTrue());
    }

    @Test
    void propertySourceNotRegisteredWhenDevServicesGloballyDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context.getEnvironment().getPropertySources()
                        .contains(DevServiceDynamicPropertySource.PROPERTY_SOURCE_NAME)).isFalse());
    }

    @Test
    void propertySourceNotRegisteredWhenOllamaDevServiceDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.ollama.enabled=false")
                .run(context -> assertThat(context.getEnvironment().getPropertySources()
                        .contains(DevServiceDynamicPropertySource.PROPERTY_SOURCE_NAME)).isFalse());
    }

    @Test
    void propertySourceNotRegisteredWhenOpenAiNotOnClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(RestartScope.class, OpenAiConnectionProperties.class))
                .run(context -> assertThat(context.getEnvironment().getPropertySources()
                        .contains(DevServiceDynamicPropertySource.PROPERTY_SOURCE_NAME)).isFalse());
    }

    @Test
    void fallsBackToDefaultOllamaUrlWhenNoContainer() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(RestartScope.class))
                .withConfiguration(AutoConfigurations.of(OllamaOpenAiDevServicesAutoConfiguration.class))
                .run(context -> {
                    var environment = context.getEnvironment();
                    assertThat(environment.getProperty("spring.ai.openai.base-url"))
                            .isEqualTo("http://localhost:11434");
                    assertThat(environment.getProperty("spring.ai.openai.api-key"))
                            .isEqualTo("ollama");
                });
    }

    @Test
    void fallsBackToCustomOllamaUrlWhenNoContainer() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(RestartScope.class))
                .withConfiguration(AutoConfigurations.of(OllamaOpenAiDevServicesAutoConfiguration.class))
                .withPropertyValues("spring.ai.ollama.base-url=http://custom-host:8080")
                .run(context -> assertThat(context.getEnvironment().getProperty("spring.ai.openai.base-url"))
                        .isEqualTo("http://custom-host:8080"));
    }

}
