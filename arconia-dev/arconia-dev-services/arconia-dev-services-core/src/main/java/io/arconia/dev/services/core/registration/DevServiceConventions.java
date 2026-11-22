package io.arconia.dev.services.core.registration;

import org.jspecify.annotations.Nullable;

/**
 * Common conventions for resolving metadata about Dev Services, such as where their
 * documentation is published.
 */
final class DevServiceConventions {

    /**
     * The documentation of the latest release. Each dev service has a page named after it.
     */
    private static final String DOCUMENTATION_URL = "https://docs.arconia.io/arconia/latest/dev-services/";

    private static final String ARCONIA_PACKAGE = "io.arconia.dev.services.";

    private DevServiceConventions() {}

    /**
     * The page about the dev service with the given name in the documentation of Arconia, when
     * it is one of the dev services of Arconia.
     */
    static @Nullable String documentation(@Nullable Class<?> registrarType, String name) {
        return isArconia(registrarType) ? DOCUMENTATION_URL + name + "/" : null;
    }

    private static boolean isArconia(@Nullable Class<?> registrarType) {
        return registrarType != null && registrarType.getPackageName().startsWith(ARCONIA_PACKAGE);
    }

}
