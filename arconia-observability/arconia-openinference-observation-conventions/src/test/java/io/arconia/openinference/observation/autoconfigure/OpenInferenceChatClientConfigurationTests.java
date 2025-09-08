package io.arconia.openinference.observation.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.openinference.observation.instrumentation.OpenInferenceAdvisorObservationConvention;
import io.arconia.openinference.observation.instrumentation.OpenInferenceChatClientObservationConvention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenInferenceChatClientConfiguration}.
 */
class OpenInferenceChatClientConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenInferenceChatClientConfiguration.class))
            .withBean(OpenInferenceProperties.class, () -> {
                OpenInferenceProperties properties = new OpenInferenceProperties();
                properties.setEnabled(true);
                return properties;
            });

    @Test
    void autoConfigurationNotActivatedWhenChatClientObservationConventionClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(ChatClientObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenInferenceChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenInferenceAdvisorObservationConvention.class);
                });
    }

    @Test
    void autoConfigurationNotActivatedWhenAdvisorObservationConventionClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(AdvisorObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenInferenceChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenInferenceAdvisorObservationConvention.class);
                });
    }

    @Test
    void observationConventionsAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenInferenceChatClientObservationConvention.class);
            assertThat(context).hasSingleBean(OpenInferenceAdvisorObservationConvention.class);
        });
    }

    @Test
    void customChatClientObservationConventionTakesPrecedence() {
        contextRunner
            .withUserConfiguration(CustomChatClientObservationConventionConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(ChatClientObservationConvention.class);
                assertThat(context.getBean(ChatClientObservationConvention.class))
                    .isSameAs(context.getBean(CustomChatClientObservationConventionConfiguration.class).customChatClientObservationConvention());
            });
    }

    @Test
    void customAdvisorObservationConventionTakesPrecedence() {
        contextRunner
            .withUserConfiguration(CustomAdvisorObservationConventionConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(AdvisorObservationConvention.class);
                assertThat(context.getBean(AdvisorObservationConvention.class))
                    .isSameAs(context.getBean(CustomAdvisorObservationConventionConfiguration.class).customAdvisorObservationConvention());
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomChatClientObservationConventionConfiguration {

        private final ChatClientObservationConvention customChatClientObservationConvention = mock(ChatClientObservationConvention.class);

        @Bean
        ChatClientObservationConvention customChatClientObservationConvention() {
            return customChatClientObservationConvention;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAdvisorObservationConventionConfiguration {

        private final AdvisorObservationConvention customAdvisorObservationConvention = mock(AdvisorObservationConvention.class);

        @Bean
        AdvisorObservationConvention customAdvisorObservationConvention() {
            return customAdvisorObservationConvention;
        }

    }

}
