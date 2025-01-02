package io.arconia.ai.core.tools;

import org.springframework.ai.model.function.FunctionCallback;

/**
 * Wrapper for {@link FunctionCallback} to identify tools in Spring AI.
 */
public interface ToolCallback extends FunctionCallback {

    /**
     * Creates a new {@link FunctionCallback.Builder} instance.
     */
    static ArconiaToolCallbackBuilder builder() {
        return new ArconiaToolCallbackBuilder();
    }

}
