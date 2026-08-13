package io.arconia.multitenancy.details.jdbc;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.hint.ResourcePatternHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JdbcTenantDetailsHints}.
 */
@DisabledInNativeImage
class JdbcTenantDetailsHintsTests {

    @Test
    void shouldRegisterResources() {
        RuntimeHints hints = register();
        assertThat(hints.resources().resourcePatternHints()).hasSize(1)
            .anySatisfy(include(JdbcTenantDetailsHints.SCHEMA_SCRIPTS_PATTERN));
    }

    @Test
    void registeredPatternShouldMatchBundledScripts() throws Exception {
        var resources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:" + JdbcTenantDetailsHints.SCHEMA_SCRIPTS_PATTERN);

        assertThat(resources).isNotEmpty();
        assertThat(resources).extracting("filename")
            .contains("schema-h2.sql", "schema-postgresql.sql", "schema-drop-h2.sql", "schema-drop-postgresql.sql");
    }

    private RuntimeHints register() {
        RuntimeHints hints = new RuntimeHints();
        new JdbcTenantDetailsHints().registerHints(hints, getClass().getClassLoader());
        return hints;
    }

    private Consumer<ResourcePatternHints> include(String... patterns) {
        return (hint) -> assertThat(hint.getIncludes()).map(ResourcePatternHint::getPattern).contains(patterns);
    }

}
