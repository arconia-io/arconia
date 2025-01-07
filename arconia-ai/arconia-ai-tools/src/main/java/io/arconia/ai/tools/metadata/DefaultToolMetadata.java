package io.arconia.ai.tools.metadata;

import java.lang.reflect.Method;

import io.arconia.ai.tools.execution.ToolExecutionMode;
import io.arconia.ai.tools.util.ToolUtils;

/**
 * Default implementation of {@link ToolMetadata}.
 */
public record DefaultToolMetadata(ToolExecutionMode executionMode, boolean returnDirect) implements ToolMetadata {

    static DefaultToolMetadata from(Method method) {
        return DefaultToolMetadata.builder()
            .executionMode(ToolUtils.getToolExecutionMode(method))
            .returnDirect(ToolUtils.getToolReturnDirect(method))
            .build();
    }

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
