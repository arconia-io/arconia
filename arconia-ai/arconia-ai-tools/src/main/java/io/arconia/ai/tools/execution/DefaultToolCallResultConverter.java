package io.arconia.ai.tools.execution;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.arconia.ai.tools.json.JsonParser;

/**
 * A default implementation of {@link ToolCallResultConverter}.
 */
public class DefaultToolCallResultConverter implements ToolCallResultConverter {

    @Override
    public String apply(@Nullable Object result, Class<?> returnType) {
        Assert.notNull(returnType, "returnType cannot be null");
        if (returnType == Void.TYPE) {
            return "Done";
        } else {
            return JsonParser.toJson(result);
        }
    }

}
