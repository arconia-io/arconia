package io.arconia.dev.services.core.registration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServiceConventions}.
 */
class DevServiceConventionsTests {

    @Test
    void devServiceOfArconiaHasItsPageInTheDocumentationOfArconia() {
        assertThat(DevServiceConventions.documentation(DevServiceConventionsTests.class, "otel-collector"))
                .isEqualTo("https://docs.arconia.io/arconia/latest/dev-services/otel-collector/");
    }

    @Test
    void devServiceFromElsewhereStatesItsOwn() {
        assertThat(DevServiceConventions.documentation(String.class, "fancydb")).isNull();
        assertThat(DevServiceConventions.documentation(null, "fancydb")).isNull();
    }

}
