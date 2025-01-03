package io.arconia.ai.core.tools;

import org.springframework.ai.model.function.FunctionCallback;

import io.arconia.ai.core.tools.method.MethodToolCallbackProvider;

/**
 * Provides {@link ToolCallback} instances for tools defined in different sources.
 */
public final class ToolCallbacks {

    private ToolCallbacks() {
    }

    public static FunctionCallback[] from(Class<?>... sources) {
        return MethodToolCallbackProvider.builder().sources(sources).build().getToolCallbacks();
    }

    public static FunctionCallback[] from(Object... sources) {
        return MethodToolCallbackProvider.builder().sources(sources).build().getToolCallbacks();
    }

}
