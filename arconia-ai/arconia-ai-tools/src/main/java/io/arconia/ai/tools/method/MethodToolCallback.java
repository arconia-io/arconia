package io.arconia.ai.tools.method;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import io.arconia.ai.tools.ToolCallback;
import io.arconia.ai.tools.definition.ToolDefinition;
import io.arconia.ai.tools.execution.DefaultToolCallResultConverter;
import io.arconia.ai.tools.execution.ToolCallResultConverter;
import io.arconia.ai.tools.execution.ToolExecutionException;
import io.arconia.ai.tools.json.JsonParser;
import io.arconia.ai.tools.metadata.ToolMetadata;

/**
 * A {@link ToolCallback} implementation to invoke methods as tools.
 */
public class MethodToolCallback implements ToolCallback {

    private static final Logger logger = LoggerFactory.getLogger(MethodToolCallback.class);

    private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();

    private final ToolDefinition toolDefinition;

    private final ToolMetadata toolMetadata;

    private final Method toolMethod;

    private final Object toolObject;

    private final ToolCallResultConverter toolCallResultConverter;

    public MethodToolCallback(ToolDefinition toolDefinition, ToolMetadata toolMetadata, Method toolMethod, Object toolObject, @Nullable ToolCallResultConverter toolCallResultConverter) {
        Assert.notNull(toolDefinition, "toolDefinition cannot be null");
        Assert.notNull(toolMetadata, "toolMetadata cannot be null");
        Assert.notNull(toolMethod, "toolMethod cannot be null");
        Assert.notNull(toolObject, "toolObject cannot be null");
        this.toolDefinition = toolDefinition;
        this.toolMetadata = toolMetadata;
        this.toolMethod = toolMethod;
        this.toolObject = toolObject;
        this.toolCallResultConverter = toolCallResultConverter != null ? toolCallResultConverter : DEFAULT_RESULT_CONVERTER;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return toolMetadata;
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        Assert.hasText(toolInput, "toolInput cannot be null or empty");

        logger.debug("Starting execution of tool: {}", toolDefinition.name());

        validateToolContextSupport(toolContext);

        Map<String, Object> toolArguments = extractToolArguments(toolInput);

        Object[] methodArguments = buildMethodArguments(toolArguments, toolContext);

        Object result = callMethod(methodArguments);

        logger.debug("Successful execution of tool: {}", toolDefinition.name());

        Class<?> returnType = toolMethod.getReturnType();

        return toolCallResultConverter.apply(result, returnType);
    }

    private void validateToolContextSupport(@Nullable ToolContext toolContext) {
        var isToolContextRequired = toolContext != null && !CollectionUtils.isEmpty(toolContext.getContext());
        var isToolContextAcceptedByMethod = Stream.of(toolMethod.getParameterTypes())
            .anyMatch(type -> ClassUtils.isAssignable(type, ToolContext.class));
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

    @Nullable
    private Object callMethod(Object[] methodArguments) {
        if (isObjectNotPublic() || isMethodNotPublic()) {
            toolMethod.setAccessible(true);
        }

        Object result;
        try {
            result = toolMethod.invoke(toolObject, methodArguments);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
        } catch (InvocationTargetException ex) {
            throw new ToolExecutionException(toolDefinition, ex.getCause());
        }
        return result;
    }

    private boolean isObjectNotPublic() {
        return !Modifier.isPublic(toolObject.getClass().getModifiers());
    }

    private boolean isMethodNotPublic() {
        return !Modifier.isPublic(toolMethod.getModifiers());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ToolDefinition toolDefinition;

        private ToolMetadata toolMetadata;

        private Method toolMethod;

        private Object toolObject;

        private ToolCallResultConverter toolCallResultConverter;

        private Builder() {}

        public Builder toolDefinition(ToolDefinition toolDefinition) {
            this.toolDefinition = toolDefinition;
            return this;
        }

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

        public Builder toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
            this.toolCallResultConverter = toolCallResultConverter;
            return this;
        }

        public MethodToolCallback build() {
            return new MethodToolCallback(toolDefinition, toolMetadata, toolMethod, toolObject, toolCallResultConverter);
        }

    }

}
