package io.arconia.ai.core.tools.method;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import io.arconia.ai.core.tools.ToolCallback;
import io.arconia.ai.core.tools.json.JsonParser;
import io.arconia.ai.core.tools.metadata.ToolMetadata;

/**
 * A {@link ToolCallback} implementation to invoke methods as tools.
 */
public class MethodToolCallback implements ToolCallback {

    private final ToolMetadata toolMetadata;

    private final Method toolMethod;

    @Nullable
    private final Object toolObject;

    public MethodToolCallback(ToolMetadata toolMetadata, Method toolMethod, @Nullable Object toolObject) {
        Assert.notNull(toolMetadata, "toolMetadata cannot be null");
        Assert.notNull(toolMethod, "toolMethod cannot be null");
        if (!Modifier.isStatic(toolMethod.getModifiers())) {
            Assert.notNull(toolObject, "toolObject cannot be null for non-static method");
        }
        this.toolMetadata = toolMetadata;
        this.toolMethod = toolMethod;
        this.toolObject = toolObject;
    }

    public ToolMetadata getToolMetadata() {
        return toolMetadata;
    }

    @Override
    public String getName() {
        return toolMetadata.name();
    }

    @Override
    public String getDescription() {
        return toolMetadata.description();
    }

    @Override
    public String getInputTypeSchema() {
        return toolMetadata.inputTypeSchema();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        Assert.hasText(toolInput, "toolInput cannot be null or empty");

        validateToolContextSupport(toolContext);

        Map<String, Object> toolArguments = extractToolArguments(toolInput);

        Object[] methodArguments = buildMethodArguments(toolArguments, toolContext);

        Object result = ReflectionUtils.invokeMethod(toolMethod, toolObject, methodArguments);

        Class<?> returnType = toolMethod.getReturnType();

        return formatResult(result, returnType);
    }

    private void validateToolContextSupport(@Nullable ToolContext toolContext) {
        var isToolContextRequired = toolContext != null && !CollectionUtils.isEmpty(toolContext.getContext());
        var isToolContextAcceptedByMethod = Stream.of(toolMethod.getGenericParameterTypes())
            .anyMatch(type -> ClassUtils.isAssignable(type.getClass(), ToolContext.class));
        if (isToolContextRequired && !isToolContextAcceptedByMethod) {
            throw new IllegalArgumentException("ToolContext is not supported by the method as an argument");
        }
    }

    private Map<String, Object> extractToolArguments(String toolInput) {
        return JsonParser.fromJson(toolInput, new TypeReference<>() {});
    }

    // Based on the implementation in MethodInvokingFunctionCallback.
    private Object[] buildMethodArguments(Map<String, Object> toolInputArguments, @Nullable ToolContext toolContext) {
        return Stream.of(toolMethod.getParameters()).map(parameter -> {
            if (parameter.getType().isAssignableFrom(ToolContext.class)) {
                return toolContext;
            }
            Object rawArgument = toolInputArguments.get(parameter.getName());
            return buildTypedArgument(rawArgument, parameter.getType());
        }).toArray();
    }

    @Nullable
    private Object buildTypedArgument(@Nullable Object value, Class<?> type) {
        if (value == null) {
            return null;
        }
        return JsonParser.toTypedObject(value, type);
    }

    // Based on the implementation in MethodInvokingFunctionCallback.
    private String formatResult(@Nullable Object result, Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return "Done";
        } else if (returnType == String.class) {
            return result != null ? (String) result : "";
        } else {
            return JsonParser.toJson(result);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ToolMetadata toolMetadata;

        private Method toolMethod;

        private Object toolObject;

        private Builder() {}

        public Builder toolMetadata(ToolMetadata toolMetadata) {
            this.toolMetadata = toolMetadata;
            return this;
        }

        public Builder toolMethod(Method toolMethod) {
            this.toolMethod = toolMethod;
            return this;
        }

        public Builder toolObject(Object toolObject) {
            this.toolObject = toolObject;
            return this;
        }

        public MethodToolCallback build() {
            return new MethodToolCallback(toolMetadata, toolMethod, toolObject);
        }

    }

}
