package io.arconia.dev.services.tests;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Disables a test class or method when no container runtime is reachable, so that the tests
 * that only need a container to be created, not started, keep running without one.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DisabledIfDockerUnavailableCondition.class)
public @interface DisabledIfDockerUnavailable {
}
