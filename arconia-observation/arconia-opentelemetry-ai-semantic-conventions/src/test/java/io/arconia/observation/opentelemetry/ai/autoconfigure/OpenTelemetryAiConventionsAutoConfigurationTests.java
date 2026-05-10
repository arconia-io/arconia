package io.arconia.observation.opentelemetry.ai.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

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

import io.arconia.observation.conventions.AiObservationConventionsProvider;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithAiAdvisorConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithChatClientObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithChatModelObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithEmbeddingModelObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithToolCallingObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryAdvisorObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryToolCallingObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatClientEventObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelEventObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelMeterObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryEmbeddingMeterObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryToolCallingObservationConvention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenTelemetryAiConventionsAutoConfiguration}.
 * Covers all three flavors: opentelemetry (default), openllmetry, and langsmith.
 */
class OpenTelemetryAiConventionsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryAiConventionsAutoConfiguration.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    // -------------------------------------------------------------------------
    // Activation / deactivation
    // -------------------------------------------------------------------------

    @Test
    void activatesWithDefaultOtelFlavor() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AiObservationConventionsProvider.class);
            assertThat(context.getBean(AiObservationConventionsProvider.class).name()).isEqualTo("opentelemetry");
            assertThat(context).hasSingleBean(OpenTelemetryChatModelObservationConvention.class);
            assertThat(context).hasSingleBean(OpenTelemetryEmbeddingModelObservationConvention.class);
            assertThat(context).hasSingleBean(OpenTelemetryToolCallingObservationConvention.class);
            assertThat(context).hasSingleBean(OpenTelemetryChatModelMeterObservationHandler.class);
            assertThat(context).hasSingleBean(OpenTelemetryEmbeddingMeterObservationHandler.class);
            assertThat(context).hasSingleBean(OpenTelemetryChatModelEventObservationHandler.class);
        });
    }

    @Test
    void doesNotActivateWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AiObservationConventionsProvider.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryToolCallingObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelMeterObservationHandler.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingMeterObservationHandler.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelEventObservationHandler.class);
                });
    }

    @Test
    void doesNotActivateWhenSpringAiNotOnClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(ChatModelObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AiObservationConventionsProvider.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryToolCallingObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelMeterObservationHandler.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingMeterObservationHandler.class);
                });
    }

    // -------------------------------------------------------------------------
    // OpenTelemetry flavor (default)
    // -------------------------------------------------------------------------

    @Test
    void otelFlavorActivatesWhenFlavorExplicitlySet() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=opentelemetry")
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenTelemetryChatModelObservationConvention.class);
                    assertThat(context).hasSingleBean(OpenTelemetryEmbeddingModelObservationConvention.class);
                    assertThat(context).hasSingleBean(OpenTelemetryToolCallingObservationConvention.class);
                });
    }

    @Test
    void otelFlavorRegistersChatClientBeansWhenChatClientOnClasspath() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenTelemetryChatClientObservationConvention.class);
            assertThat(context).hasSingleBean(OpenTelemetryChatClientEventObservationHandler.class);
        });
    }

    @Test
    void otelFlavorDoesNotRegisterChatClientBeansWhenChatClientNotOnClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(ChatClientObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatClientEventObservationHandler.class);
                });
    }

    @Test
    void otelFlavorDoesNotRegisterEventHandlersWhenOtelBridgeNotOnClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("io.micrometer.tracing.otel.bridge.OtelSpan"))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelEventObservationHandler.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatClientEventObservationHandler.class);
                });
    }

    // Custom bean precedence (OTel flavor)

    @Test
    void otelFlavorCustomChatModelConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomChatModelConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelObservationConvention.class);
                });
    }

    @Test
    void otelFlavorCustomEmbeddingModelConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomEmbeddingModelConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(EmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingModelObservationConvention.class);
                });
    }

    @Test
    void otelFlavorCustomToolCallingConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomToolCallingConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ToolCallingObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryToolCallingObservationConvention.class);
                });
    }

    @Test
    void otelFlavorCustomChatClientConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomChatClientConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatClientObservationConvention.class);
                });
    }

    // -------------------------------------------------------------------------
    // OpenLLMetry flavor
    // -------------------------------------------------------------------------

    @Test
    void openllmetryFlavorActivatesWhenFlavorSet() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenLLMetryChatModelObservationConvention.class);
                    assertThat(context).hasSingleBean(OpenLLMetryEmbeddingModelObservationConvention.class);
                    assertThat(context).hasSingleBean(OpenLLMetryToolCallingObservationConvention.class);
                });
    }

    @Test
    void openllmetryFlavorDoesNotActivateOtelOnlyBeans() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelMeterObservationHandler.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingMeterObservationHandler.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelEventObservationHandler.class);
                });
    }

    @Test
    void openllmetryFlavorRegistersChatClientBeansWhenChatClientOnClasspath() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenLLMetryChatClientObservationConvention.class);
                    assertThat(context).hasSingleBean(OpenLLMetryAdvisorObservationConvention.class);
                });
    }

    @Test
    void openllmetryFlavorDoesNotRegisterChatClientBeansWhenChatClientNotOnClasspath() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                .withClassLoader(new FilteredClassLoader(ChatClientObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenLLMetryChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenLLMetryAdvisorObservationConvention.class);
                });
    }

    @Test
    void openllmetryFlavorDoesNotRegisterAdvisorBeanWhenAdvisorNotOnClasspath() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                .withClassLoader(new FilteredClassLoader(AdvisorObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenLLMetryChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenLLMetryAdvisorObservationConvention.class);
                });
    }

    // Custom bean precedence (OpenLLMetry flavor)

    @Test
    void openllmetryFlavorCustomChatModelConventionTakesPrecedence() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                .withUserConfiguration(CustomChatModelConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenLLMetryChatModelObservationConvention.class);
                });
    }

    @Test
    void openllmetryFlavorCustomEmbeddingModelConventionTakesPrecedence() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                .withUserConfiguration(CustomEmbeddingModelConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(EmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenLLMetryEmbeddingModelObservationConvention.class);
                });
    }

    @Test
    void openllmetryFlavorCustomToolCallingConventionTakesPrecedence() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                .withUserConfiguration(CustomToolCallingConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ToolCallingObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenLLMetryToolCallingObservationConvention.class);
                });
    }

    @Test
    void openllmetryFlavorCustomChatClientConventionTakesPrecedence() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                .withUserConfiguration(CustomChatClientConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenLLMetryChatClientObservationConvention.class);
                });
    }

    @Test
    void openllmetryFlavorCustomAdvisorConventionTakesPrecedence() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                .withUserConfiguration(CustomAdvisorConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AdvisorObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenLLMetryAdvisorObservationConvention.class);
                });
    }

    // -------------------------------------------------------------------------
    // LangSmith flavor
    // -------------------------------------------------------------------------

    @Test
    void langsmithFlavorActivatesWhenFlavorSet() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .run(context -> {
                    assertThat(context).hasSingleBean(LangSmithChatModelObservationConvention.class);
                    assertThat(context).hasSingleBean(LangSmithEmbeddingModelObservationConvention.class);
                    assertThat(context).hasSingleBean(LangSmithToolCallingObservationConvention.class);
                });
    }

    @Test
    void langsmithFlavorDoesNotActivateOtelOnlyBeans() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelMeterObservationHandler.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingMeterObservationHandler.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelEventObservationHandler.class);
                });
    }

    @Test
    void langsmithFlavorRegistersObservationHandlersWhenOtelBridgeOnClasspath() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .run(context -> {
                    assertThat(context).hasSingleBean(LangSmithChatModelObservationHandler.class);
                    assertThat(context).hasSingleBean(LangSmithEmbeddingModelObservationHandler.class);
                });
    }

    @Test
    void langsmithFlavorDoesNotRegisterObservationHandlersWhenOtelBridgeNotOnClasspath() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .withClassLoader(new FilteredClassLoader("io.micrometer.tracing.otel.bridge.OtelSpan"))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LangSmithChatModelObservationHandler.class);
                    assertThat(context).doesNotHaveBean(LangSmithEmbeddingModelObservationHandler.class);
                });
    }

    @Test
    void langsmithFlavorRegistersChatClientBeansWhenChatClientOnClasspath() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .run(context -> {
                    assertThat(context).hasSingleBean(LangSmithChatClientObservationConvention.class);
                    assertThat(context).hasSingleBean(LangSmithChatClientObservationHandler.class);
                    assertThat(context).hasSingleBean(LangSmithAiAdvisorConvention.class);
                });
    }

    @Test
    void langsmithFlavorDoesNotRegisterChatClientBeansWhenChatClientNotOnClasspath() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .withClassLoader(new FilteredClassLoader(ChatClientObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LangSmithChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatClientObservationHandler.class);
                    assertThat(context).doesNotHaveBean(LangSmithAiAdvisorConvention.class);
                });
    }

    @Test
    void langsmithFlavorDoesNotRegisterChatClientHandlerWhenOtelBridgeNotOnClasspath() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .withClassLoader(new FilteredClassLoader("io.micrometer.tracing.otel.bridge.OtelSpan"))
                .run(context ->
                        assertThat(context).doesNotHaveBean(LangSmithChatClientObservationHandler.class));
    }

    // Custom bean precedence (LangSmith flavor)

    @Test
    void langsmithFlavorCustomChatModelConventionTakesPrecedence() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .withUserConfiguration(CustomChatModelConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatModelObservationConvention.class);
                });
    }

    @Test
    void langsmithFlavorCustomEmbeddingModelConventionTakesPrecedence() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .withUserConfiguration(CustomEmbeddingModelConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(EmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithEmbeddingModelObservationConvention.class);
                });
    }

    @Test
    void langsmithFlavorCustomToolCallingConventionTakesPrecedence() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .withUserConfiguration(CustomToolCallingConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ToolCallingObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithToolCallingObservationConvention.class);
                });
    }

    @Test
    void langsmithFlavorCustomChatClientConventionTakesPrecedence() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .withUserConfiguration(CustomChatClientConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithChatClientObservationConvention.class);
                });
    }

    @Test
    void langsmithFlavorCustomAdvisorConventionTakesPrecedence() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                .withUserConfiguration(CustomAdvisorConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AdvisorObservationConvention.class);
                    assertThat(context).doesNotHaveBean(LangSmithAiAdvisorConvention.class);
                });
    }

    // -------------------------------------------------------------------------
    // Shared custom bean configurations
    // -------------------------------------------------------------------------

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
