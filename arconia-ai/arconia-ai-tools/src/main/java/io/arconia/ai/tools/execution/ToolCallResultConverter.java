package io.arconia.ai.tools.execution;

import java.util.function.BiFunction;

import org.springframework.lang.Nullable;

/**
 * A functional interface to convert tool call results to a String
 * that can be sent back to the AI model.
 */
@FunctionalInterface
public interface ToolCallResultConverter extends BiFunction<Object, Class<?>, String> {

    /**
     * Given an Object returned by a tool, convert it
     * to a String compatible with the given class type.
     */
    String apply(@Nullable Object result, Class<?> returnType);

}
