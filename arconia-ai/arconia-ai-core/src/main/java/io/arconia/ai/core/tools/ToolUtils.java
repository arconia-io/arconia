package io.arconia.ai.core.tools;

import java.util.stream.Stream;

import org.springframework.ai.model.function.FunctionCallback;

/**
 * Miscellaneous tool utility methods. Mainly for internal use within the framework.
 */
public final class ToolUtils {

    public static boolean hasDuplicateToolNames(FunctionCallback... functionCallbacks) {
        return Stream.of(functionCallbacks)
            .map(FunctionCallback::getName)
            .distinct()
            .count() != functionCallbacks.length;
    }

}
