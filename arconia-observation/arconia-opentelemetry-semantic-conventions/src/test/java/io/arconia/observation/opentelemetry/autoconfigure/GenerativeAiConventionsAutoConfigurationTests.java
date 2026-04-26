package io.arconia.observation.opentelemetry.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryChatClientEventObservationHandler;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryChatModelEventObservationHandler;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryChatModelMeterObservationHandler;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryEmbeddingModelMeterObservationHandler;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryToolCallingModelObservationConvention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link GenerativeAiConventionsAutoConfiguration}.
 */
class GenerativeAiConventionsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GenerativeAiConventionsAutoConfiguration.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    // Activation / deactivation

    @Test
    void activatesWhenConventionTypePropertyNotSet() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenTelemetryChatModelObservationConvention.class);
            assertThat(context).hasSingleBean(OpenTelemetryEmbeddingModelObservationConvention.class);
            assertThat(context).hasSingleBean(OpenTelemetryToolCallingModelObservationConvention.class);
            assertThat(context).hasSingleBean(OpenTelemetryChatModelMeterObservationHandler.class);
            assertThat(context).hasSingleBean(OpenTelemetryEmbeddingModelMeterObservationHandler.class);
            assertThat(context).hasSingleBean(OpenTelemetryChatModelEventObservationHandler.class);
        });
    }

    @Test
    void doesNotActivateWhenConventionTypeSetToDifferentValue() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.type=micrometer")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryToolCallingModelObservationConvention.class);
                });
    }

    @Test
    void doesNotActivateWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.generative-ai.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryToolCallingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelMeterObservationHandler.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingModelMeterObservationHandler.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelEventObservationHandler.class);
                });
    }

    @Test
    void doesNotActivateWhenSpringAiNotOnClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(ChatModelObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryToolCallingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelMeterObservationHandler.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingModelMeterObservationHandler.class);
                });
    }

    // Chat client

    @Test
    void registersChatClientBeanWhenChatClientOnClasspath() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenTelemetryChatClientObservationConvention.class);
            assertThat(context).hasSingleBean(OpenTelemetryChatClientEventObservationHandler.class);

        });
    }

    @Test
    void doesNotRegisterChatClientBeanWhenChatClientNotOnClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(ChatClientObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatClientEventObservationHandler.class);
                });
    }

    // Event handler

    @Test
    void registersEventHandlerWhenOtelBridgeOnClasspath() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenTelemetryChatModelEventObservationHandler.class);
            assertThat(context).hasSingleBean(OpenTelemetryChatClientEventObservationHandler.class);
        });
    }

    @Test
    void doesNotRegisterEventHandlerWhenOtelBridgeNotOnClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("io.micrometer.tracing.otel.bridge.OtelSpan"))
                .run(context -> {
                        assertThat(context).doesNotHaveBean(OpenTelemetryChatModelEventObservationHandler.class);
                        assertThat(context).doesNotHaveBean(OpenTelemetryChatClientEventObservationHandler.class);
                });
    }

    // Custom bean precedence

    @Test
    void customChatModelObservationConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomChatModelConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatModelObservationConvention.class);
                });
    }

    @Test
    void customEmbeddingModelObservationConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomEmbeddingModelConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(EmbeddingModelObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryEmbeddingModelObservationConvention.class);
                });
    }

    @Test
    void customToolCallingObservationConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomToolCallingConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ToolCallingObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryToolCallingModelObservationConvention.class);
                });
    }

    @Test
    void customChatClientObservationConventionTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomChatClientConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatClientObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryChatClientObservationConvention.class);
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

}
