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
     * Whether to enable GenAI semantic conventions.
     */
    private boolean enabled = true;

    /**
     * The convention flavor to use for GenAI observations.
     */
    private GenAiConventionFlavor flavor = GenAiConventionFlavor.OPENTELEMETRY;

    /**
     * Whether to capture the input and output messages of an inference observation.
     * The actual default depends on the configured flavor: the opentelemetry flavor doesn't capture it,
     * the other flavors do.
     */
    private boolean captureMessageContent = false;

    /**
     * Whether to capture the tool definitions of an inference observation.
     * The actual default depends on the configured flavor: the opentelemetry flavor doesn't capture it,
     * the other flavors do.
     */
    private boolean captureToolDefinitions = false;

    /**
     * Whether to capture the tool content (arguments and result) of a tool execution observation.
     * The actual default depends on the configured flavor: the opentelemetry flavor doesn't capture it,
     * the other flavors do.
     */
    private boolean captureToolCallContent = false;

    /**
     * Where to record the captured content of an inference observation (messages and tool definitions).
     * Only the {@code opentelemetry} flavor supports it. The other flavors record the content
     * where their platform expects it.
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
