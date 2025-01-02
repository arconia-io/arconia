package io.arconia.ai.core.tools.method;

import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.ai.core.tools.Tool;
import io.arconia.ai.core.tools.ToolCallback;
import io.arconia.ai.core.tools.ToolCallbackResolver;

/**
 * A {@link ToolCallbackResolver} that resolves {@link ToolCallback} instances from
 * methods annotated with {@link Tool}.
 */
public class MethodToolCallbackResolver implements ToolCallbackResolver {

    private static final Logger logger = LoggerFactory.getLogger(MethodToolCallbackResolver.class);

    @Nullable
    private final Object targetObject;

    private final Class<?> targetType;

    private MethodToolCallbackResolver(@Nullable Object targetObject, Class<?> targetType) {
        Assert.notNull(targetType, "targetType cannot be null");
        this.targetObject = targetObject;
        this.targetType = targetType;
    }

    @Override
    public FunctionCallback[] getToolCallbacks() {
        if (targetObject != null) {
            return getToolCallbacksFromObject();
        }
        return getToolCallbacksFromType();
    }

    private FunctionCallback[] getToolCallbacksFromObject() {
        return Stream.of(ReflectionUtils.getDeclaredMethods(targetType))
            .filter(method -> method.isAnnotationPresent(Tool.class))
            .map(method -> ToolCallback.builder()
                .method(method)
                .name(getToolName(method.getAnnotation(Tool.class), method.getName()))
                .description(getToolDescription(method.getAnnotation(Tool.class), method.getName()))
                .schemaType(method.getAnnotation(Tool.class).schemaType())
                .targetClass(targetType)
                .targetObject(targetObject)
                .build())
            .toArray(FunctionCallback[]::new);
    }

    private FunctionCallback[] getToolCallbacksFromType() {
        return Stream.of(ReflectionUtils.getDeclaredMethods(targetType))
            .filter(method -> method.isAnnotationPresent(Tool.class))
            .filter(method -> Modifier.isStatic(method.getModifiers()))
            .map(method -> ToolCallback.builder()
                .method(method)
                .name(getToolName(method.getAnnotation(Tool.class), method.getName()))
                .description(getToolDescription(method.getAnnotation(Tool.class), method.getName()))
                .schemaType(method.getAnnotation(Tool.class).schemaType())
                .targetClass(targetType)
                .build())
            .toArray(FunctionCallback[]::new);
    }

    private static String getToolName(Tool tool, String methodName) {
        return StringUtils.hasText(tool.name()) ? tool.name() : methodName;
    }

    private static String getToolDescription(Tool tool, String methodName) {
        return StringUtils.hasText(tool.value()) ? tool.value() : methodName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Object targetObject;

        private Class<?> targetType;

        public Builder object(Object targetObject) {
            Assert.isNull(this.targetType, "only one of object or type can be set");
            this.targetObject = targetObject;
            this.targetType = targetObject.getClass();
            return this;
        }

        public Builder type(Class<?> targetType) {
            Assert.isNull(this.targetType, "only one of object or type can be set");
            this.targetType = targetType;
            try {
                this.targetObject = ReflectionUtils.accessibleConstructor(targetType).newInstance();
            }
            catch (Exception ex) {
                logger.warn(
                        "Failed to create instance of target type. Only @Tool-annotated static methods will be considered.",
                        ex);
            }
            return this;
        }

        public MethodToolCallbackResolver build() {
            return new MethodToolCallbackResolver(targetObject, targetType);
        }

    }

}
