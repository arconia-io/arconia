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
     * How to capture input and output message content in an inference observation.
     * The actual default depends on the configured flavor and is set by
     * {@link OpenTelemetryAiConventionsEnvironmentPostProcessor}.
     */
    private CaptureContentFormat captureContent = CaptureContentFormat.NONE;

    /**
     * Whether to include tool definitions in an inference observation.
     * The actual default depends on the configured flavor and is set by
     * {@link OpenTelemetryAiConventionsEnvironmentPostProcessor}.
     */
    private boolean includeToolDefinitions = false;

    /**
     * Whether to include tool content (arguments and result) in a tool execution observation.
     * The actual default depends on the configured flavor and is set by
     * {@link OpenTelemetryAiConventionsEnvironmentPostProcessor}.
     */
    private boolean includeToolCallContent = false;

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

    public CaptureContentFormat getCaptureContent() {
        return captureContent;
    }

    public void setCaptureContent(CaptureContentFormat captureContent) {
        this.captureContent = captureContent;
    }

    public boolean isIncludeToolDefinitions() {
        return includeToolDefinitions;
    }

    public void setIncludeToolDefinitions(boolean includeToolDefinitions) {
        this.includeToolDefinitions = includeToolDefinitions;
    }

    public boolean isIncludeToolCallContent() {
        return includeToolCallContent;
    }

    public void setIncludeToolCallContent(boolean includeToolCallContent) {
        this.includeToolCallContent = includeToolCallContent;
    }

    /**
     * Whether content capture is enabled (any mode other than {@link CaptureContentFormat#NONE}).
     * Useful for flavors that use a boolean include/exclude model (LangSmith, OpenLLMetry).
     */
    public boolean isContentCaptureEnabled() {
        return captureContent != CaptureContentFormat.NONE;
    }

    /**
     * Format for capturing input/output message content in inference observations.
     */
    public enum CaptureContentFormat {

        /**
         * Do not capture message content.
         */
        NONE,

        /**
         * Capture message content as span attributes (high cardinality).
         */
        SPAN_ATTRIBUTES,

        /**
         * Capture message content as span events.
         */
        SPAN_EVENTS

    }

}
