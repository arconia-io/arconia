package io.arconia.dev.services.ollama;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Whether Ollama native connection is unavailable when running the application in dev mode.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnOllamaNativeUnavailable.class)
public @interface ConditionalOnOllamaNativeUnavailable {
}
