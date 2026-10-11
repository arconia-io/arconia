package io.arconia.dev.services.core.registration;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServiceDefinition.Discovery;
import io.arconia.dev.services.core.registration.DevServiceDefinition.LinkDefinition;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestConnectionDetails;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link DevServiceDefinition}.
 */
class DevServiceDefinitionTests {

    @Test
    void whenLinksGivenThenTheyAreCopied() {
        List<LinkDefinition> links = new ArrayList<>(List.of(new LinkDefinition("UI", 8080, "")));

        DevServiceDefinition definition = definition(links);
        links.clear();

        assertThat(definition.links()).containsExactly(new LinkDefinition("UI", 8080, ""));
    }

    @Test
    void whenNameIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DevServiceDefinition("", "Test Dev Service", null, TestDevServicesProperties.DEFAULT,
                        TestPostgresContainer.class, TestPostgresContainer::new, true, null, null, null, List.of(), List.of()))
                .withMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenLinksAreNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> definition(null))
                .withMessageContaining("links cannot be null");
    }

    @Test
    void whenDiscoveryTypeIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Discovery(null, container -> new TestConnectionDetails(container.host())))
                .withMessageContaining("type cannot be null");
    }

    @Test
    void whenDiscoveryFactoryIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Discovery(TestConnectionDetails.class, null))
                .withMessageContaining("factory cannot be null");
    }

    @Test
    void whenLinkResolvedThenUrlPointsAtTheMappedPort() {
        LinkDefinition link = new LinkDefinition("Docs", 8080, "/docs");

        assertThat(link.resolve("localhost", 1234)).isEqualTo(new DevServiceLink("Docs", "http://localhost:1234/docs"));
    }

    @Test
    void whenLinkLabelIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LinkDefinition(null, 8080, ""))
                .withMessageContaining("label cannot be null");
    }

    private static DevServiceDefinition definition(List<LinkDefinition> links) {
        return new DevServiceDefinition("postgres", "Test Dev Service", null, TestDevServicesProperties.DEFAULT,
                TestPostgresContainer.class, TestPostgresContainer::new, true, null, null, null, List.of(), links);
    }

}
