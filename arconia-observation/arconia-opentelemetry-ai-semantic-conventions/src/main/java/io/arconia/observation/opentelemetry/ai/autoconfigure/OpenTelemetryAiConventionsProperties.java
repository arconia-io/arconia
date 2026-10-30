package io.arconia.observation.opentelemetry.ai.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.observation.opentelemetry.ai.instrumentation.GenAiConventionFlavor;

/**
 * Configuration properties for the OpenTelemetry AI Semantic Conventions.
 */
@ConfigurationProperties(prefix = OpenTelemetryAiConventionsProperties.CONFIG_PREFIX)
public class OpenTelemetryAiConventionsProperties {

    public static final String CONFIG_PREFIX = "arconia.observations.conventions.opentelemetry.ai";

    /**
     * Whether to enable the OpenTelemetry AI Semantic Conventions module.
     */
    private boolean enabled = true;

    /**
     * The convention flavor to apply. Accepted values: opentelemetry, openlit, openllmetry, langsmith, langfuse.
     */
    private GenAiConventionFlavor flavor = GenAiConventionFlavor.OPENTELEMETRY;

    /**
     * Whether to capture the input and output messages of model calls. The default depends on the flavor:
     * the opentelemetry flavor doesn't capture them, the other flavors do.
     */
    private boolean captureMessageContent = false;

    /**
     * Whether to capture the definitions of the tools available to the model in model calls. The default depends
     * on the flavor: the opentelemetry flavor doesn't capture them, the other flavors do.
     */
    private boolean captureToolDefinitions = false;

    /**
     * Whether to capture the arguments and result of tool executions. The default depends on the flavor:
     * the opentelemetry flavor doesn't capture them, the other flavors do.
     */
    private boolean captureToolCallContent = false;

    /**
     * Where to record the captured messages and tool definitions of model calls. Only the opentelemetry flavor
     * supports it, the other flavors record the content where their platform expects it.
     * Accepted values: span-attributes, span-events, log-events.
     */
    private InferenceContentDestination inferenceContentDestination = InferenceContentDestination.SPAN_ATTRIBUTES;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public GenAiConventionFlavor getFlavor() {
        return flavor;
    }

    public void setFlavor(GenAiConventionFlavor flavor) {
        this.flavor = flavor;
    }

    public boolean isCaptureMessageContent() {
        return captureMessageContent;
    }

    public void setCaptureMessageContent(boolean captureMessageContent) {
        this.captureMessageContent = captureMessageContent;
    }

    public boolean isCaptureToolDefinitions() {
        return captureToolDefinitions;
    }

    public void setCaptureToolDefinitions(boolean captureToolDefinitions) {
        this.captureToolDefinitions = captureToolDefinitions;
    }

    public boolean isCaptureToolCallContent() {
        return captureToolCallContent;
    }

    public void setCaptureToolCallContent(boolean captureToolCallContent) {
        this.captureToolCallContent = captureToolCallContent;
    }

    public InferenceContentDestination getInferenceContentDestination() {
        return inferenceContentDestination;
    }

    public void setInferenceContentDestination(InferenceContentDestination inferenceContentDestination) {
        this.inferenceContentDestination = inferenceContentDestination;
    }

    /**
     * Where to record the captured content of an inference observation.
     */
    public enum InferenceContentDestination {

        /**
         * Record the content as attributes of the inference span.
         */
        SPAN_ATTRIBUTES,

        /**
         * Record the content in a {@code gen_ai.client.inference.operation.details} event
         * attached to the inference span.
         */
        SPAN_EVENTS,

        /**
         * Record the content in a {@code gen_ai.client.inference.operation.details} event
         * exported through the OpenTelemetry Logs API and correlated with the inference span.
         */
        LOG_EVENTS

    }

}
