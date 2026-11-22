package io.arconia.dev.ui.server.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.Sanitizer;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.core.env.PropertySource;

/**
 * Tells which properties hold a value not meant for everyone looking at the screen. The Dev UI
 * hides such a value until the developer asks to see it.
 * <p>
 * The decision is the one of the sanitizer of Spring Boot, which the Actuator endpoints use.
 * It follows the sanitizing functions the application defines and, after them, the rules of
 * Spring Boot for what is likely to be sensitive: credentials, going by the name of the
 * property, and the few properties known to carry them. Spring Boot also has a rule for
 * every property named like a URL. Here, a URL is only sensitive when it carries credentials,
 * since most of them don't and are what a developer looks for.
 */
final class SensitiveProperties {

    private static final String URL_WITH_CREDENTIALS = ".*://[^/\\s]+:[^/\\s]+@.*";

    private final Sanitizer sanitizer;

    SensitiveProperties(Collection<SanitizingFunction> applicationFunctions) {
        List<SanitizingFunction> functions = new ArrayList<>(applicationFunctions);
        functions.add(SanitizingFunction.sanitizeValue().ifLikelyCredential().ifLikelySensitiveProperty().ifVcapServices());
        functions.add(SanitizingFunction.sanitizeValue().ifValueStringMatches(URL_WITH_CREDENTIALS));
        this.sanitizer = new Sanitizer(functions);
    }

    /**
     * Whether the sanitizer would change the given value of a property, in which case the
     * value is sensitive. The value itself is kept, for when the developer asks to see it.
     */
    boolean isSensitive(@Nullable PropertySource<?> propertySource, String name, Object value) {
        Object sanitizedValue = this.sanitizer.sanitize(new SanitizableData(propertySource, name, value), true);
        return !value.equals(sanitizedValue);
    }

}
