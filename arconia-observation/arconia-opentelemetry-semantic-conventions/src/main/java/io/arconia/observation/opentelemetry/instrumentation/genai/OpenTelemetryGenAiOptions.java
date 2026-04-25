package io.arconia.observation.opentelemetry.instrumentation.genai;

public class OpenTelemetryGenAiOptions {

    private final Inference inference = new Inference();

    private final ToolExecution toolExecution = new ToolExecution();

    public Inference getInference() {
        return inference;
    }

    public ToolExecution getToolExecution() {
        return toolExecution;
    }

    public static class Inference {

        /**
         * How to capture input and output message content in an inference observation.
         */
        private CaptureContentFormat captureContent = CaptureContentFormat.NONE;

        /**
         * Whether to include the tool definitions in an inference observation.
         */
        private boolean includeToolDefinitions = false;

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
         * Capture message content as span attributes.
         */
        SPAN_ATTRIBUTES,

        /**
         * Capture message content as span events.
         */
        SPAN_EVENTS

    }

    public static class ToolExecution {

        /**
         * Whether to include the tool content (arguments and result) in a tool execution observation.
         */
        private boolean includeContent = false;

        public boolean isIncludeContent() {
            return includeContent;
        }

        public void setIncludeContent(boolean includeContent) {
            this.includeContent = includeContent;
        }

    }

}
