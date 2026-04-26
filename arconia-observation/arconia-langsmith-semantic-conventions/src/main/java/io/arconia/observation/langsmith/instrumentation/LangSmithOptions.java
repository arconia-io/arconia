package io.arconia.observation.langsmith.instrumentation;

public class LangSmithOptions {

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
         * Whether to include input and output message content in an inference observation.
         */
        private boolean includeContent = true;

        /**
         * Whether to include the tool definitions in an inference observation.
         */
        private boolean includeToolDefinitions = true;

        public boolean isIncludeContent() {
            return includeContent;
        }

        public void setIncludeContent(boolean includeContent) {
            this.includeContent = includeContent;
        }

        public boolean isIncludeToolDefinitions() {
            return includeToolDefinitions;
        }

        public void setIncludeToolDefinitions(boolean includeToolDefinitions) {
            this.includeToolDefinitions = includeToolDefinitions;
        }

    }

    public static class ToolExecution {

        /**
         * Whether to include the tool content (arguments and result) in a tool execution observation.
         */
        private boolean includeContent = true;

        public boolean isIncludeContent() {
            return includeContent;
        }

        public void setIncludeContent(boolean includeContent) {
            this.includeContent = includeContent;
        }

    }

}
