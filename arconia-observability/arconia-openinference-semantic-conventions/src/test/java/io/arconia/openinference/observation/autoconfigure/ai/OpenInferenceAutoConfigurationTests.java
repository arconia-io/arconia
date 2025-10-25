package io.arconia.openinference.observation.autoconfigure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.model.observation.ModelObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.openinference.observation.instrumentation.ai.OpenInferenceChatModelObservationConvention;
import io.arconia.openinference.observation.instrumentation.ai.OpenInferenceEmbeddingModelObservationConvention;
import io.arconia.openinference.observation.instrumentation.ai.OpenInferenceGenerativeAiOnlyObservationPredicate;
import io.arconia.openinference.observation.instrumentation.ai.OpenInferenceToolCallingObservationConvention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenInferenceAutoConfiguration}.
 */
class OpenInferenceAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenInferenceAutoConfiguration.class))
            .withPropertyValues("arconia.otel.enabled=true");

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(OpenInferenceChatModelObservationConvention.class);
                assertThat(context).doesNotHaveBean(OpenInferenceEmbeddingModelObservationConvention.class);
                assertThat(context).doesNotHaveBean(OpenInferenceToolCallingObservationConvention.class);
                assertThat(context).doesNotHaveBean(OpenInferenceGenerativeAiOnlyObservationPredicate.class);
            });
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenInferenceDisabled() {
        contextRunner
            .withPropertyValues("arconia.observations.generative-ai.openinference.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(OpenInferenceChatModelObservationConvention.class);
                assertThat(context).doesNotHaveBean(OpenInferenceEmbeddingModelObservationConvention.class);
                assertThat(context).doesNotHaveBean(OpenInferenceToolCallingObservationConvention.class);
                assertThat(context).doesNotHaveBean(OpenInferenceGenerativeAiOnlyObservationPredicate.class);
            });
    }

    @Test
    void autoConfigurationNotActivatedWhenModelObservationContextClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(ModelObservationContext.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenInferenceChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenInferenceEmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenInferenceToolCallingObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenInferenceGenerativeAiOnlyObservationPredicate.class);
                });
    }

    @Test
    void observationConventionsAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenInferenceChatModelObservationConvention.class);
            assertThat(context).hasSingleBean(OpenInferenceEmbeddingModelObservationConvention.class);
            assertThat(context).hasSingleBean(OpenInferenceToolCallingObservationConvention.class);
            assertThat(context).hasSingleBean(OpenInferenceGenerativeAiOnlyObservationPredicate.class);
        });
    }

    @Test
    void springAiOnlyObservationPredicateNotRegisteredWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.observations.generative-ai.openinference.exclusive=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(OpenInferenceGenerativeAiOnlyObservationPredicate.class);
            });
    }

    @Test
    void customChatModelObservationConventionTakesPrecedence() {
        contextRunner
            .withUserConfiguration(CustomChatModelObservationConventionConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(ChatModelObservationConvention.class);
                assertThat(context.getBean(ChatModelObservationConvention.class))
                    .isSameAs(context.getBean(CustomChatModelObservationConventionConfiguration.class).customChatModelObservationConvention());
            });
    }

    @Test
    void customEmbeddingModelObservationConventionTakesPrecedence() {
        contextRunner
            .withUserConfiguration(CustomEmbeddingModelObservationConventionConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(EmbeddingModelObservationConvention.class);
                assertThat(context.getBean(EmbeddingModelObservationConvention.class))
                    .isSameAs(context.getBean(CustomEmbeddingModelObservationConventionConfiguration.class).customEmbeddingModelObservationConvention());
            });
    }

    @Test
    void customToolCallingObservationConventionTakesPrecedence() {
        contextRunner
            .withUserConfiguration(CustomToolCallingObservationConventionConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(ToolCallingObservationConvention.class);
                assertThat(context.getBean(ToolCallingObservationConvention.class))
                    .isSameAs(context.getBean(CustomToolCallingObservationConventionConfiguration.class).customToolCallingObservationConvention());
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomChatModelObservationConventionConfiguration {

        private final ChatModelObservationConvention customChatModelObservationConvention = mock(ChatModelObservationConvention.class);

        @Bean
        ChatModelObservationConvention customChatModelObservationConvention() {
            return customChatModelObservationConvention;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomEmbeddingModelObservationConventionConfiguration {

        private final EmbeddingModelObservationConvention customEmbeddingModelObservationConvention = mock(EmbeddingModelObservationConvention.class);

        @Bean
        EmbeddingModelObservationConvention customEmbeddingModelObservationConvention() {
            return customEmbeddingModelObservationConvention;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomToolCallingObservationConventionConfiguration {

        private final ToolCallingObservationConvention customToolCallingObservationConvention = mock(ToolCallingObservationConvention.class);

        @Bean
        ToolCallingObservationConvention customToolCallingObservationConvention() {
            return customToolCallingObservationConvention;
        }

    }

}
