package io.arconia.ai.core.tools;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.function.DefaultCommonCallbackInvokingSpec;
import org.springframework.ai.model.function.DefaultFunctionCallbackBuilder;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionInvokingFunctionCallback;
import org.springframework.ai.model.function.MethodInvokingFunctionCallback;
import org.springframework.ai.util.ParsingUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Builds a {@link ToolCallback} instance. Implementation based on
 * {@link DefaultFunctionCallbackBuilder}.
 */
public class ArconiaToolCallbackBuilder implements FunctionCallback.Builder {

    private final static Logger logger = LoggerFactory.getLogger(ArconiaToolCallbackBuilder.class);

    @Override
    public <I, O> FunctionCallback.FunctionInvokingSpec<I, O> function(String name, Function<I, O> function) {
        return new ArconiaFunctionInvokingSpec<>(name, function);
    }

    @Override
    public <I, O> FunctionCallback.FunctionInvokingSpec<I, O> function(String name,
            BiFunction<I, ToolContext, O> biFunction) {
        return new ArconiaFunctionInvokingSpec<>(name, biFunction);
    }

    @Override
    public <O> FunctionCallback.FunctionInvokingSpec<Void, O> function(String name, Supplier<O> supplier) {
        Function<Void, O> function = input -> supplier.get();
        return new ArconiaFunctionInvokingSpec<>(name, function).inputType(Void.class);
    }

    @Override
    public <I> FunctionCallback.FunctionInvokingSpec<I, Void> function(String name, Consumer<I> consumer) {
        Function<I, Void> function = (I input) -> {
            consumer.accept(input);
            return null;
        };
        return new ArconiaFunctionInvokingSpec<>(name, function);
    }

    @Override
    public FunctionCallback.MethodInvokingSpec method(String methodName, Class<?>... argumentTypes) {
        throw new UnsupportedOperationException("Use the 'method(Method method)' method instead");
    }

    /**
     * Create a {@link FunctionCallback.MethodInvokingSpec} for the given method.
     */
    public FunctionCallback.MethodInvokingSpec method(Method method) {
        return new ArconiaMethodInvokingSpec(method);
    }

    private String generateDescription(String fromName) {
        String generatedDescription = ParsingUtils.reConcatenateCamelCase(fromName, " ");

        logger.info("Description is not set! A best effort attempt to generate a description:'{}' from the:'{}'",
                generatedDescription, fromName);
        logger.info("It is recommended to set the Description explicitly! Use the 'description()' method!");

        return generatedDescription;
    }

    /**
     * Arconia {@link FunctionCallback.FunctionInvokingSpec} implementation.
     */
    final class ArconiaFunctionInvokingSpec<I, O>
            extends DefaultCommonCallbackInvokingSpec<FunctionCallback.FunctionInvokingSpec<I, O>>
            implements FunctionCallback.FunctionInvokingSpec<I, O> {

        private final String name;

        private Type inputType;

        private final BiFunction<I, ToolContext, O> biFunction;

        private final Function<I, O> function;

        private ArconiaFunctionInvokingSpec(String name, BiFunction<I, ToolContext, O> biFunction) {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.notNull(biFunction, "biFunction cannot be null");
            this.name = name;
            this.biFunction = biFunction;
            this.function = null;
        }

        private ArconiaFunctionInvokingSpec(String name, Function<I, O> function) {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.notNull(function, "function cannot be null");
            this.name = name;
            this.biFunction = null;
            this.function = function;
        }

        @Override
        public FunctionCallback.FunctionInvokingSpec<I, O> inputType(Class<?> inputType) {
            Assert.notNull(inputType, "inputType cannot be null");
            this.inputType = inputType;
            return this;
        }

        @Override
        public FunctionCallback.FunctionInvokingSpec<I, O> inputType(ParameterizedTypeReference<?> inputType) {
            Assert.notNull(inputType, "inputType cannot be null");
            this.inputType = inputType.getType();
            return this;
        }

        @Override
        public FunctionCallback build() {
            Assert.notNull(this.getObjectMapper(), "objectMapper cannot be null");
            Assert.hasText(this.name, "name cannot be null or empty");
            Assert.notNull(this.getResponseConverter(), "responseConverter cannot be null");
            Assert.notNull(this.inputType, "inputType cannot be null");

            if (this.getInputTypeSchema() == null) {
                boolean upperCaseTypeValues = schemaType == FunctionCallback.SchemaType.OPEN_API_SCHEMA;
                this.inputTypeSchema = ModelOptionsUtils.getJsonSchema(this.inputType, upperCaseTypeValues);
            }

            BiFunction<I, ToolContext, O> finalBiFunction = (this.biFunction != null) ? this.biFunction
                    : (request, context) -> this.function.apply(request);

            try {
                var constructor = FunctionInvokingFunctionCallback.class.getDeclaredConstructor(String.class,
                        String.class, String.class, Type.class, Function.class, ObjectMapper.class, BiFunction.class);
                constructor.setAccessible(true);
                return constructor.newInstance(this.name, this.getDescriptionExt(), this.getInputTypeSchema(),
                        this.inputType, this.getResponseConverter(), this.getObjectMapper(), finalBiFunction);
            }
            catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to create FunctionInvokingFunctionCallback instance", ex);
            }
        }

        private String getDescriptionExt() {
            if (StringUtils.hasText(this.getDescription())) {
                return this.getDescription();
            }
            return generateDescription(this.name);
        }

    }

    /**
     * Arconia {@link FunctionCallback.MethodInvokingSpec} implementation.
     */
    final class ArconiaMethodInvokingSpec extends DefaultCommonCallbackInvokingSpec<FunctionCallback.MethodInvokingSpec>
            implements FunctionCallback.MethodInvokingSpec {

        private final Method method;

        private String name;

        private Class<?> targetClass;

        private Object targetObject;

        private ArconiaMethodInvokingSpec(Method method) {
            Assert.notNull(method, "method cannot be null");
            this.method = method;
        }

        @Override
        public FunctionCallback.MethodInvokingSpec name(String name) {
            Assert.hasText(name, "name cannot be null or empty");
            this.name = name;
            return this;
        }

        @Override
        public FunctionCallback.MethodInvokingSpec targetClass(Class<?> targetClass) {
            Assert.notNull(targetClass, "targetClass cannot be null");
            this.targetClass = targetClass;
            return this;
        }

        @Override
        public FunctionCallback.MethodInvokingSpec targetObject(Object methodObject) {
            Assert.notNull(methodObject, "methodObject cannot be null");
            this.targetObject = methodObject;
            this.targetClass = methodObject.getClass();
            return this;
        }

        @Override
        public FunctionCallback build() {
            Assert.isTrue(this.targetClass != null || this.targetObject != null,
                    "targetClass or targetObject cannot be null");

            try {
                var constructor = MethodInvokingFunctionCallback.class.getDeclaredConstructor(Object.class,
                        Method.class, String.class, ObjectMapper.class, String.class, Function.class);
                constructor.setAccessible(true);
                return constructor.newInstance(this.targetObject, method, this.getDescriptionExt(),
                        this.getObjectMapper(), this.name, this.getResponseConverter());
            }
            catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to create MethodInvokingFunctionCallback instance", ex);
            }
        }

        private String getDescriptionExt() {
            if (StringUtils.hasText(this.getDescription())) {
                return this.getDescription();
            }
            return generateDescription(StringUtils.hasText(this.name) ? this.name : this.method.getName());
        }

    }

}
