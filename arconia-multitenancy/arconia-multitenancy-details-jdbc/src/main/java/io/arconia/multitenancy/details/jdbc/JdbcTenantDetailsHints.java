package io.arconia.multitenancy.details.jdbc;

import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Makes the JDBC schema scripts available in a GraalVM native image.
 */
class JdbcTenantDetailsHints implements RuntimeHintsRegistrar {

    static final String SCHEMA_SCRIPTS_PATTERN = "io/arconia/multitenancy/details/jdbc/autoconfigure/schema-*.sql";

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.resources().registerPattern(SCHEMA_SCRIPTS_PATTERN);
    }

}
