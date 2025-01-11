package io.arconia.ai.tools.metadata;

import io.arconia.ai.tools.execution.ToolExecutionMode;

/**
 * Default implementation of {@link ToolMetadata}.
 */
public record DefaultToolMetadata(ToolExecutionMode executionMode, boolean returnDirect) implements ToolMetadata {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ToolExecutionMode executionMode = ToolExecutionMode.BLOCKING;

        private boolean returnDirect = false;

        private Builder() {}

        public Builder executionMode(ToolExecutionMode executionMode) {
            this.executionMode = executionMode;
            return this;
        }

        public Builder returnDirect(boolean returnDirect) {
            this.returnDirect = returnDirect;
            return this;
        }

        public DefaultToolMetadata build() {
            return new DefaultToolMetadata(executionMode, returnDirect);
        }

    }

}
