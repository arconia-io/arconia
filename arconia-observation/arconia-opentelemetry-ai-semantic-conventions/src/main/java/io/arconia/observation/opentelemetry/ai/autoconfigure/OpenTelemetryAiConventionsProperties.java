package io.arconia.observation.opentelemetry.ai.autoconfigure;

import org.jspecify.annotations.Nullable;
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
    @Nullable
    private Boolean captureMessageContent;

    /**
     * Whether to capture the definitions of the tools available to the model in model calls. The default depends
     * on the flavor: the opentelemetry flavor doesn't capture them, the other flavors do.
     */
    @Nullable
    private Boolean captureToolDefinitions;

    /**
     * Whether to capture the arguments and result of tool executions. The default depends on the flavor:
     * the opentelemetry flavor doesn't capture them, the other flavors do.
     */
    @Nullable
    private Boolean captureToolCallContent;

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

    public @Nullable Boolean getCaptureMessageContent() {
        return captureMessageContent;
    }

    public void setCaptureMessageContent(@Nullable Boolean captureMessageContent) {
        this.captureMessageContent = captureMessageContent;
    }

    public @Nullable Boolean getCaptureToolDefinitions() {
        return captureToolDefinitions;
    }

    public void setCaptureToolDefinitions(@Nullable Boolean captureToolDefinitions) {
        this.captureToolDefinitions = captureToolDefinitions;
    }

    public @Nullable Boolean getCaptureToolCallContent() {
        return captureToolCallContent;
    }

    public void setCaptureToolCallContent(@Nullable Boolean captureToolCallContent) {
        this.captureToolCallContent = captureToolCallContent;
    }

    public InferenceContentDestination getInferenceContentDestination() {
        return inferenceContentDestination;
    }

    public void setInferenceContentDestination(InferenceContentDestination inferenceContentDestination) {
        this.inferenceContentDestination = inferenceContentDestination;
    }

    // The methods below resolve the effective settings. They aren't getters, so that they aren't bound as properties.

    /**
     * Whether the input and output messages of model calls are captured, by default when the flavor captures content.
     */
    public boolean capturesMessageContent() {
        return orFlavorDefault(captureMessageContent);
    }

    /**
     * Whether the tool definitions of model calls are captured, by default when the flavor captures content.
     */
    public boolean capturesToolDefinitions() {
        return orFlavorDefault(captureToolDefinitions);
    }

    /**
     * Whether the arguments and result of tool executions are captured, by default when the flavor captures content.
     */
    public boolean capturesToolCallContent() {
        return orFlavorDefault(captureToolCallContent);
    }

    /**
     * Returns where the captured content of model calls is recorded: the configured destination for the
     * opentelemetry flavor, the span attributes for the other flavors, which record it where their platform expects it.
     */
    public InferenceContentDestination contentDestination() {
        return flavor == GenAiConventionFlavor.OPENTELEMETRY ? inferenceContentDestination : InferenceContentDestination.SPAN_ATTRIBUTES;
    }

    private boolean orFlavorDefault(@Nullable Boolean value) {
        return value != null ? value : flavor.capturesContentByDefault();
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
