package io.arconia.observation.opentelemetry.instrumentation.genai;

import java.util.HashMap;
import java.util.Map;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryTokenUsageMetricsGenerator}.
 */
class OpenTelemetryTokenUsageMetricsGeneratorTests {

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void inputTokensShouldRecordDistributionSummary() {
        Observation.Context context = new Observation.Context();
        context.addLowCardinalityKeyValue(
                io.micrometer.common.KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "chat"));
        context.addLowCardinalityKeyValue(
                io.micrometer.common.KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), "gpt-4"));

        OpenTelemetryTokenUsageMetricsGenerator.inputTokens(new TestUsage(), context, meterRegistry);

        DistributionSummary summary = meterRegistry.find(GenAiMoreIncubatingAttributes.GEN_AI_CLIENT_TOKEN_USAGE)
                .tag(GenAiIncubatingAttributes.GEN_AI_TOKEN_TYPE.getKey(),
                        GenAiIncubatingAttributes.GenAiTokenTypeIncubatingValues.INPUT)
                .summary();

        assertThat(summary).isNotNull();
        assertThat(summary.count()).isEqualTo(1);
        assertThat(summary.totalAmount()).isEqualTo(1000);
        assertThat(summary.getId().getBaseUnit()).isEqualTo("{token}");
    }

    @Test
    void outputTokensShouldRecordDistributionSummary() {
        Observation.Context context = new Observation.Context();
        context.addLowCardinalityKeyValue(
                io.micrometer.common.KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "chat"));

        OpenTelemetryTokenUsageMetricsGenerator.outputTokens(new TestUsage(), context, meterRegistry);

        DistributionSummary summary = meterRegistry.find(GenAiMoreIncubatingAttributes.GEN_AI_CLIENT_TOKEN_USAGE)
                .tag(GenAiIncubatingAttributes.GEN_AI_TOKEN_TYPE.getKey(),
                        GenAiIncubatingAttributes.GenAiTokenTypeIncubatingValues.OUTPUT)
                .summary();

        assertThat(summary).isNotNull();
        assertThat(summary.count()).isEqualTo(1);
        assertThat(summary.totalAmount()).isEqualTo(500);
    }

    @Test
    void shouldIncludeLowCardinalityTagsFromContext() {
        Observation.Context context = new Observation.Context();
        context.addLowCardinalityKeyValue(
                io.micrometer.common.KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "chat"));
        context.addLowCardinalityKeyValue(
                io.micrometer.common.KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(), "openai"));
        context.addLowCardinalityKeyValue(
                io.micrometer.common.KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), "gpt-4"));

        OpenTelemetryTokenUsageMetricsGenerator.inputTokens(new TestUsage(), context, meterRegistry);

        DistributionSummary summary = meterRegistry.find(GenAiMoreIncubatingAttributes.GEN_AI_CLIENT_TOKEN_USAGE)
                .tag(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "chat")
                .tag(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(), "openai")
                .tag(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), "gpt-4")
                .summary();

        assertThat(summary).isNotNull();
    }

    static class TestUsage implements Usage {
        @Override
        public Integer getPromptTokens() { return 1000; }
        @Override
        public Integer getCompletionTokens() { return 500; }
        @Override
        public Integer getTotalTokens() { return 1500; }
        @Override
        public Map<String, Integer> getNativeUsage() {
            Map<String, Integer> usage = new HashMap<>();
            usage.put("promptTokens", 1000);
            usage.put("completionTokens", 500);
            return usage;
        }
    }

}
