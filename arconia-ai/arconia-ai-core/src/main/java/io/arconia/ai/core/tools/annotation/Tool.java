package io.arconia.ai.core.tools.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.ai.model.function.FunctionCallback;

/**
 * Marks a method as a tool in Spring AI.
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {

    /**
     * The name of the tool. If not provided, the method name will be used.
     */
    String name() default "";

    /**
     * The description of the tool. If not provided, the method name will be used.
     */
    String value() default "";

    /**
     * The schema type of the tool. JSON Schema will work for most cases.
     * Vertex AI requires OpenAPI Schema.
     */
    FunctionCallback.SchemaType schemaType() default FunctionCallback.SchemaType.JSON_SCHEMA;

}
