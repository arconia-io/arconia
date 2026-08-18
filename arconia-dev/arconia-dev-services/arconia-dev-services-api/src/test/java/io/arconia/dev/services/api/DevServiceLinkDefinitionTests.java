package io.arconia.dev.services.api;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DevServiceLinkDefinition}.
 */
class DevServiceLinkDefinitionTests {

    @Test
    void whenIdIsNullThenThrow() {
        assertThatThrownBy(() -> DevServiceLinkDefinition.builder().id(null).label("Grafana").port(3000).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be null or empty");
    }

    @Test
    void whenIdIsNotALabelSafeIdentifierThenThrow() {
        assertThatThrownBy(() -> DevServiceLinkDefinition.builder().id("My Console").label("Grafana").port(3000).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id must contain only lowercase letters, digits, and dashes");
    }

    @Test
    void whenLabelIsEmptyThenThrow() {
        assertThatThrownBy(() -> DevServiceLinkDefinition.builder().id("grafana").label("").port(3000).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label cannot be null or empty");
    }

    @Test
    void whenLabelContainsControlCharactersThenThrow() {
        assertThatThrownBy(() -> DevServiceLinkDefinition.builder().id("grafana").label("Graf\nana").port(3000).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label cannot contain control characters");
    }

    @Test
    void whenSchemeIsNotAValidUriSchemeThenThrow() {
        assertThatThrownBy(() -> DevServiceLinkDefinition.builder()
                .id("grafana").label("Grafana").scheme("http://").port(3000).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scheme is not a valid URI scheme");
    }

    @Test
    void whenPortIsOutOfRangeThenThrow() {
        assertThatThrownBy(() -> DevServiceLinkDefinition.builder().id("grafana").label("Grafana").port(0).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port must be between 1 and 65535");
        assertThatThrownBy(() -> DevServiceLinkDefinition.builder().id("grafana").label("Grafana").port(65536).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port must be between 1 and 65535");
    }

    @Test
    void whenPathDoesNotStartWithSlashThenThrow() {
        assertThatThrownBy(() -> DevServiceLinkDefinition.builder()
                .id("grafana").label("Grafana").port(3000).path("console").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path must be empty or start with '/'");
    }

    @Test
    void whenOnlyRequiredFieldsAreGivenThenDefaultsApply() {
        var definition = DevServiceLinkDefinition.builder().id("grafana").label("Grafana").port(3000).build();

        assertThat(definition.scheme()).isEqualTo(DevServiceLinkDefinition.DEFAULT_SCHEME);
        assertThat(definition.path()).isEmpty();
    }

    @Test
    void whenResolvedThenUrlUsesTheMappedPort() {
        var definition = DevServiceLinkDefinition.builder()
                .id("artemis").label("Artemis Management Console").port(8161).path("/console").build();

        var link = definition.toLink("localhost", 32769);

        assertThat(link.id()).isEqualTo("artemis");
        assertThat(link.label()).isEqualTo("Artemis Management Console");
        assertThat(link.url()).isEqualTo("http://localhost:32769/console");
    }

    @Test
    void whenResolvedWithACustomSchemeThenUrlUsesIt() {
        var definition = DevServiceLinkDefinition.builder()
                .id("console").label("Console").scheme("https").port(8443).build();

        assertThat(definition.toLink("localhost", 32770).url()).isEqualTo("https://localhost:32770");
    }

}
