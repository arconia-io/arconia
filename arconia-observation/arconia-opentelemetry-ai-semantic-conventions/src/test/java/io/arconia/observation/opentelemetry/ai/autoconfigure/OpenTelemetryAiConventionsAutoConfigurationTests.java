package io.arconia.observation.opentelemetry.ai.autoconfigure;

import java.util.List;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelMeterObservationHandler;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelMeterObservationHandler;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.conventions.AiObservationConventionsProvider;
import io.arconia.observation.opentelemetry.ai.instrumentation.langfuse.LangfuseAdvisorObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langfuse.LangfuseChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langfuse.LangfuseChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithAdvisorObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithChatModelObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithEmbeddingModelObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithImageModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithToolCallingObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openlit.OpenLitChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openlit.OpenLitChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openlit.OpenLitEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openlit.OpenLitImageModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryAdvisorObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryToolCallingObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryAdvisorObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelEventObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelLogEventObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelMeterObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryEmbeddingMeterObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryImageModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryToolCallingObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConversationIdObservationHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenTelemetryAiConventionsAutoConfiguration}.
 * Covers all the flavors: opentelemetry (default), openllmetry, langsmith, openlit, and langfuse.
 */
class OpenTelemetryAiConventionsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryAiConventionsAutoConfiguration.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Nested
    class Activation {

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

    }

    @Nested
    class OpenTelemetryFlavor {


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
        void otelFlavorRegistersImageModelBeanWhenImageModelOnClasspath() {
            contextRunner.run(context ->
                    assertThat(context).hasSingleBean(OpenTelemetryImageModelObservationConvention.class));
        }

        @Test
        void otelFlavorRegistersChatClientBeansWhenChatClientOnClasspath() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(OpenTelemetryChatClientObservationConvention.class);
                        assertThat(context).hasSingleBean(GenAiConversationIdObservationHandler.class);
            });
        }

        @Test
        void otelFlavorDoesNotRegisterChatClientBeansWhenChatClientNotOnClasspath() {
            contextRunner
                    .withClassLoader(new FilteredClassLoader(ChatClientObservationConvention.class))
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(OpenTelemetryChatClientObservationConvention.class);
                        assertThat(context).doesNotHaveBean(GenAiConversationIdObservationHandler.class);
                    });
        }

        @Test
        void otelFlavorDoesNotRegisterEventHandlersWhenOtelBridgeNotOnClasspath() {
            contextRunner
                    .withClassLoader(new FilteredClassLoader("io.micrometer.tracing.otel.bridge.OtelSpan"))
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(OpenTelemetryChatModelEventObservationHandler.class);
                    });
        }

        @Test
        void otelFlavorRegistersLogEventHandlerWithSpringBootOpenTelemetry() {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(OpenTelemetryAiConventionsAutoConfiguration.class,
                            OpenTelemetrySdkAutoConfiguration.class))
                    .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                    .run(context -> {
                        assertThat(context).hasSingleBean(OpenTelemetry.class);
                        assertThat(context).hasSingleBean(OpenTelemetryChatModelLogEventObservationHandler.class);
                    });
        }

        @Test
        void otelFlavorDoesNotRegisterLogEventHandlerWhenOpenTelemetryNotAvailable() {
            contextRunner.run(context ->
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelLogEventObservationHandler.class));
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

    }

    @Nested
    class OpenLLMetryFlavor {


        @Test
        void openllmetryFlavorActivatesWhenFlavorSet() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                    .run(context -> {
                        assertThat(context).hasSingleBean(OpenLLMetryChatModelObservationConvention.class);
                        assertThat(context).hasSingleBean(OpenTelemetryEmbeddingModelObservationConvention.class);
                        assertThat(context).hasSingleBean(OpenLLMetryToolCallingObservationConvention.class);
                    });
        }

        @Test
        void openllmetryFlavorDoesNotActivateOtelOnlyBeans() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                    .run(context -> assertThat(context).doesNotHaveBean(OpenTelemetryChatModelEventObservationHandler.class));
        }

        @Test
        void openllmetryFlavorRegistersChatClientBeansWhenChatClientOnClasspath() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openllmetry")
                    .run(context -> {
                        assertThat(context).hasSingleBean(OpenLLMetryChatClientObservationConvention.class);
                        assertThat(context).hasSingleBean(OpenLLMetryAdvisorObservationConvention.class);
                        assertThat(context).hasSingleBean(GenAiConversationIdObservationHandler.class);
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
                        assertThat(context).doesNotHaveBean(GenAiConversationIdObservationHandler.class);
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
                        assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingModelObservationConvention.class);
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

    }

    @Nested
    class LangSmithFlavor {


        @Test
        void langsmithFlavorActivatesWhenFlavorSet() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                    .run(context -> {
                        assertThat(context).hasSingleBean(LangSmithChatModelObservationConvention.class);
                        assertThat(context).hasSingleBean(LangSmithEmbeddingModelObservationConvention.class);
                        assertThat(context).hasSingleBean(LangSmithToolCallingObservationConvention.class);
                        assertThat(context).hasSingleBean(LangSmithImageModelObservationConvention.class);
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
                        assertThat(context).hasSingleBean(LangSmithAdvisorObservationConvention.class);
                        assertThat(context).hasSingleBean(GenAiConversationIdObservationHandler.class);
                    });
        }

        @Test
        void langsmithFlavorDoesNotRegisterChatClientBeansWhenChatClientNotOnClasspath() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langsmith")
                    .withClassLoader(new FilteredClassLoader(ChatClientObservationConvention.class))
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(LangSmithChatClientObservationConvention.class);
                        assertThat(context).doesNotHaveBean(LangSmithAdvisorObservationConvention.class);
                        assertThat(context).doesNotHaveBean(GenAiConversationIdObservationHandler.class);
                    });
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
                        assertThat(context).doesNotHaveBean(LangSmithAdvisorObservationConvention.class);
                    });
        }

    }

    @Nested
    class OpenLitFlavor {


        @Test
        void openlitFlavorActivatesWhenFlavorSet() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openlit")
                    .run(context -> {
                        assertThat(context).hasSingleBean(OpenLitChatModelObservationConvention.class);
                        assertThat(context).hasSingleBean(OpenLitEmbeddingModelObservationConvention.class);
                        assertThat(context).hasSingleBean(OpenTelemetryToolCallingObservationConvention.class);
                        assertThat(context).hasSingleBean(OpenLitImageModelObservationConvention.class);
                    });
        }

        @Test
        void openlitFlavorRegistersChatClientBeansWhenChatClientOnClasspath() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openlit")
                    .run(context -> {
                        assertThat(context).hasSingleBean(OpenLitChatClientObservationConvention.class);
                        assertThat(context).hasSingleBean(OpenTelemetryAdvisorObservationConvention.class);
                        assertThat(context).hasSingleBean(GenAiConversationIdObservationHandler.class);
                    });
        }

        @Test
        void openlitFlavorDoesNotRegisterChatClientBeansWhenChatClientNotOnClasspath() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openlit")
                    .withClassLoader(new FilteredClassLoader(ChatClientObservationConvention.class))
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(OpenLitChatClientObservationConvention.class);
                        assertThat(context).doesNotHaveBean(OpenTelemetryAdvisorObservationConvention.class);
                        assertThat(context).doesNotHaveBean(GenAiConversationIdObservationHandler.class);
                    });
        }

        // Custom bean precedence (OpenLIT flavor)

        @Test
        void openlitFlavorCustomChatModelConventionTakesPrecedence() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openlit")
                    .withUserConfiguration(CustomChatModelConventionConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatModelObservationConvention.class);
                        assertThat(context).doesNotHaveBean(OpenLitChatModelObservationConvention.class);
                    });
        }

        @Test
        void openlitFlavorCustomEmbeddingModelConventionTakesPrecedence() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openlit")
                    .withUserConfiguration(CustomEmbeddingModelConventionConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(EmbeddingModelObservationConvention.class);
                        assertThat(context).doesNotHaveBean(OpenLitEmbeddingModelObservationConvention.class);
                    });
        }

        @Test
        void openlitFlavorCustomToolCallingConventionTakesPrecedence() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openlit")
                    .withUserConfiguration(CustomToolCallingConventionConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ToolCallingObservationConvention.class);
                        assertThat(context).doesNotHaveBean(OpenTelemetryToolCallingObservationConvention.class);
                    });
        }

        @Test
        void openlitFlavorCustomImageModelConventionTakesPrecedence() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openlit")
                    .withUserConfiguration(CustomImageModelConventionConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ImageModelObservationConvention.class);
                        assertThat(context).doesNotHaveBean(OpenLitImageModelObservationConvention.class);
                    });
        }

        @Test
        void openlitFlavorCustomChatClientConventionTakesPrecedence() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openlit")
                    .withUserConfiguration(CustomChatClientConventionConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientObservationConvention.class);
                        assertThat(context).doesNotHaveBean(OpenLitChatClientObservationConvention.class);
                    });
        }

        @Test
        void openlitFlavorCustomAdvisorConventionTakesPrecedence() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=openlit")
                    .withUserConfiguration(CustomAdvisorConventionConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(AdvisorObservationConvention.class);
                        assertThat(context).doesNotHaveBean(OpenTelemetryAdvisorObservationConvention.class);
                    });
        }

    }

    @Nested
    class LangfuseFlavor {


        @Test
        void langfuseFlavorActivatesWhenFlavorSet() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langfuse")
                    .run(context -> {
                        assertThat(context).hasSingleBean(LangfuseChatModelObservationConvention.class);
                        assertThat(context).hasSingleBean(OpenTelemetryEmbeddingModelObservationConvention.class);
                        assertThat(context).hasSingleBean(OpenTelemetryToolCallingObservationConvention.class);
                        assertThat(context).hasSingleBean(OpenTelemetryImageModelObservationConvention.class);
                    });
        }

        @Test
        void langfuseFlavorRegistersChatClientBeansWhenChatClientOnClasspath() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langfuse")
                    .run(context -> {
                        assertThat(context).hasSingleBean(LangfuseChatClientObservationConvention.class);
                        assertThat(context).hasSingleBean(LangfuseAdvisorObservationConvention.class);
                        assertThat(context).hasSingleBean(GenAiConversationIdObservationHandler.class);
                    });
        }

        @Test
        void langfuseFlavorDoesNotRegisterChatClientBeansWhenChatClientNotOnClasspath() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langfuse")
                    .withClassLoader(new FilteredClassLoader(ChatClientObservationConvention.class))
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(LangfuseChatClientObservationConvention.class);
                        assertThat(context).doesNotHaveBean(LangfuseAdvisorObservationConvention.class);
                        assertThat(context).doesNotHaveBean(GenAiConversationIdObservationHandler.class);
                    });
        }

        @Test
        void langfuseFlavorCustomChatModelConventionTakesPrecedence() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langfuse")
                    .withUserConfiguration(CustomChatModelConventionConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatModelObservationConvention.class);
                        assertThat(context).doesNotHaveBean(LangfuseChatModelObservationConvention.class);
                    });
        }

        @Test
        void langfuseFlavorCustomAdvisorConventionTakesPrecedence() {
            contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=langfuse")
                    .withUserConfiguration(CustomAdvisorConventionConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(AdvisorObservationConvention.class);
                        assertThat(context).doesNotHaveBean(LangfuseAdvisorObservationConvention.class);
                    });
        }

    }

    @Nested
    class ConversationIdPropagation {


        @Test
        void conversationIdObservationHandlerUsesTheKeysOfTheFlavor() {
            Map<String, List<String>> keysByFlavor = Map.of(
                    "opentelemetry", List.of("gen_ai.conversation.id"),
                    "openlit", List.of("gen_ai.conversation.id"),
                    "openllmetry", List.of("gen_ai.conversation.id", "traceloop.association.properties.session_id"),
                    "langsmith", List.of("langsmith.metadata.thread_id"),
                    "langfuse", List.of("gen_ai.conversation.id", "session.id"));
            keysByFlavor.forEach((flavor, keys) -> contextRunner
                    .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=" + flavor)
                    .run(context -> assertThat(context.getBean(GenAiConversationIdObservationHandler.class).getAttributeKeys())
                            .as(flavor).containsExactlyElementsOf(keys)));
        }

    }

    @Nested
    class SharedDefaults {


        @Test
        void eachFlavorRegistersOneConventionPerObservationType() {
            for (String flavor : List.of("opentelemetry", "openllmetry", "langsmith", "openlit", "langfuse")) {
                contextRunner
                        .withPropertyValues("arconia.observations.conventions.opentelemetry.ai.flavor=" + flavor)
                        .run(context -> {
                            assertThat(context).as(flavor).hasSingleBean(ChatModelObservationConvention.class);
                            assertThat(context).as(flavor).hasSingleBean(EmbeddingModelObservationConvention.class);
                            assertThat(context).as(flavor).hasSingleBean(ImageModelObservationConvention.class);
                            assertThat(context).as(flavor).hasSingleBean(ToolCallingObservationConvention.class);
                            assertThat(context).as(flavor).hasSingleBean(ChatClientObservationConvention.class);
                            assertThat(context).as(flavor).hasSingleBean(AdvisorObservationConvention.class);
                            assertThat(context).as(flavor).hasSingleBean(ChatModelMeterObservationHandler.class);
                            assertThat(context).as(flavor).hasSingleBean(EmbeddingModelMeterObservationHandler.class);
                        });
            }
        }

        @Test
        void doesNotRegisterMeterHandlersWithoutMeterRegistry() {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(OpenTelemetryAiConventionsAutoConfiguration.class))
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(OpenTelemetryChatModelObservationConvention.class);
                        assertThat(context).doesNotHaveBean(ChatModelMeterObservationHandler.class);
                        assertThat(context).doesNotHaveBean(EmbeddingModelMeterObservationHandler.class);
                    });
        }

    }

    // Shared custom bean configurations


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

    @Configuration(proxyBeanMethods = false)
    static class CustomImageModelConventionConfig {
        @Bean
        ImageModelObservationConvention imageModelObservationConvention() {
            return mock(ImageModelObservationConvention.class);
        }
    }

}
