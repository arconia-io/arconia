package io.arconia.observation.langsmith.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.conventions.ObservationConventionsProvider;
import io.arconia.observation.langsmith.instrumentation.LangSmithAdvisorObservationConvention;
import io.arconia.observation.langsmith.instrumentation.LangSmithChatClientObservationConvention;
import io.arconia.observation.langsmith.instrumentation.LangSmithChatClientObservationHandler;
import io.arconia.observation.langsmith.instrumentation.LangSmithChatModelObservationConvention;
import io.arconia.observation.langsmith.instrumentation.LangSmithChatModelObservationHandler;
import io.arconia.observation.langsmith.instrumentation.LangSmithEmbeddingModelObservationConvention;
import io.arconia.observation.langsmith.instrumentation.LangSmithEmbeddingModelObservationHandler;
import io.arconia.observation.langsmith.instrumentation.LangSmithToolCallingObservationConvention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link LangSmithConventionsAutoConfiguration}.
 */
class LangSmithConventionsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LangSmithConventionsAutoConfiguration.class));

    // Activation / deactivation

    @Test
    void activatesWhenConventionTypePropertyNotSet() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ObservationConventionsProvider.class);
            assertThat(context).hasSingleBean(LangSmithChatModelObservationConvention.class);
            assertThat(context).hasSingleBean(LangSmithEmbeddingModelObservationConvention.class);
            assertThat(context).hasSingleBean(LangSmithToolCallingObservationConvention.class);
            assertThat(context).hasSingleBean(LangSmithChatModelObservationHandler.class);
            assertThat(context).hasSingleBean(LangSmithEmbeddingModelObservationHandler.class);
        });
    }

    @Test
    void doesNotActivateWhenConventionTypeSetToDifferentValue() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.type=opentelemetry")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ObservationConventionsProvider.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithEmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithToolCallingObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatModelObservationHandler.class);
                    assertThat(context).doesNotHaveBean(LangSmithEmbeddingModelObservationHandler.class);
                });
    }

    @Test
    void doesNotActivateWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.langsmith.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ObservationConventionsProvider.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithEmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithToolCallingObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatModelObservationHandler.class);
                    assertThat(context).doesNotHaveBean(LangSmithEmbeddingModelObservationHandler.class);
                });
    }

    @Test
    void doesNotActivateWhenSpringAiNotOnClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(ChatModelObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ObservationConventionsProvider.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithEmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithToolCallingObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatModelObservationHandler.class);
                    assertThat(context).doesNotHaveBean(LangSmithEmbeddingModelObservationHandler.class);
                });
    }

    // Chat client

    @Test
    void registersChatClientBeansWhenChatClientOnClasspath() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LangSmithChatClientObservationConvention.class);
            assertThat(context).hasSingleBean(LangSmithChatClientObservationHandler.class);
            assertThat(context).hasSingleBean(LangSmithAdvisorObservationConvention.class);
        });
    }

    @Test
    void doesNotRegisterChatClientBeansWhenChatClientNotOnClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(ChatClientObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LangSmithChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatClientObservationHandler.class);
                    assertThat(context).doesNotHaveBean(LangSmithAdvisorObservationConvention.class);
                });
    }

    // Event handler

    @Test
    void registersObservationHandlersWhenOtelBridgeOnClasspath() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LangSmithChatModelObservationHandler.class);
            assertThat(context).hasSingleBean(LangSmithEmbeddingModelObservationHandler.class);
            assertThat(context).hasSingleBean(LangSmithChatClientObservationHandler.class);
        });
    }

    @Test
    void doesNotRegisterObservationHandlersWhenOtelBridgeNotOnClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("io.micrometer.tracing.otel.bridge.OtelSpan"))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LangSmithChatModelObservationHandler.class);
                    assertThat(context).doesNotHaveBean(LangSmithEmbeddingModelObservationHandler.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatClientObservationHandler.class);
                });
    }

    // Custom bean precedence

    @Test
    void customChatModelObservationConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomChatModelConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatModelObservationConvention.class);
                });
    }

    @Test
    void customEmbeddingModelObservationConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomEmbeddingModelConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(EmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithEmbeddingModelObservationConvention.class);
                });
    }

    @Test
    void customToolCallingObservationConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomToolCallingConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ToolCallingObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithToolCallingObservationConvention.class);
                });
    }

    @Test
    void customChatClientObservationConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomChatClientConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatClientObservationConvention.class);
                });
    }

    @Test
    void customAdvisorObservationConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomAdvisorConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AdvisorObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithAdvisorObservationConvention.class);
                });
    }

    // Custom bean configurations

    @Configuration(proxyBeanMethods = false)
    static class CustomChatModelConventionConfig {
        @Bean
        ChatModelObservationConvention chatModelObservationConvention() {
            return mock(ChatModelObservationConvention.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomEmbeddingModelConventionConfig {
        @Bean
        EmbeddingModelObservationConvention embeddingModelObservationConvention() {
            return mock(EmbeddingModelObservationConvention.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomToolCallingConventionConfig {
        @Bean
        ToolCallingObservationConvention toolCallingObservationConvention() {
            return mock(ToolCallingObservationConvention.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomChatClientConventionConfig {
        @Bean
        ChatClientObservationConvention chatClientObservationConvention() {
            return mock(ChatClientObservationConvention.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAdvisorConventionConfig {
        @Bean
        AdvisorObservationConvention advisorObservationConvention() {
            return mock(AdvisorObservationConvention.class);
        }
    }

}
