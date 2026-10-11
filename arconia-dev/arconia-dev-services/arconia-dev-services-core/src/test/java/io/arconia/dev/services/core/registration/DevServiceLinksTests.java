package io.arconia.dev.services.core.registration;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration.Origin;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestLinkContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServiceLinks}.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevServiceLinksTests {

    @Test
    void whenLinksDeclaredThenResolvedInOrder() {
        DevServiceSpec service = linkyService()
                .link("UI", 8080)
                .link("Docs", 8080, "/docs");

        List<DevServiceLink> links = DevServiceLinks.resolve(service, "localhost", DevServiceLinksTests::mappedPort, Origin.MANAGED);

        assertThat(links).containsExactly(
                new DevServiceLink("UI", "http://localhost:1234"),
                new DevServiceLink("Docs", "http://localhost:1234/docs"));
    }

    @Test
    void whenNoLinksDeclaredThenEmpty() {
        assertThat(DevServiceLinks.resolve(linkyService(), "localhost", DevServiceLinksTests::mappedPort, Origin.MANAGED)).isEmpty();
    }

    @Test
    void whenManagedLinkPortCannotBeResolvedThenOnlyThatLinkIsSkippedWithAWarning(CapturedOutput output) {
        DevServiceSpec service = linkyService()
                .link("UI", 8080)
                .link("Hidden", 9999);

        List<DevServiceLink> links = DevServiceLinks.resolve(service, "localhost", DevServiceLinksTests::mappedPort, Origin.MANAGED);

        // The dev service declares a port its container doesn't expose, which it can fix.
        assertThat(links).containsExactly(new DevServiceLink("UI", "http://localhost:1234"));
        assertThat(output).contains("WARN").contains("Skipping the 'Hidden' link of the 'linky' dev service");
    }

    @Test
    void whenDiscoveredLinkPortCannotBeResolvedThenOnlyThatLinkIsSkippedQuietly(CapturedOutput output) {
        DevServiceSpec service = linkyService()
                .link("UI", 8080)
                .link("Hidden", 9999);

        List<DevServiceLink> links = DevServiceLinks.resolve(service, "localhost", DevServiceLinksTests::mappedPort, Origin.DISCOVERED);

        // The application that started the container published 8080 but not 9999,
        // which is an expected condition rather than a failure.
        assertThat(links).containsExactly(new DevServiceLink("UI", "http://localhost:1234"));
        assertThat(output).doesNotContain("Skipping the 'Hidden' link");
    }

    /**
     * The port mapping of a container publishing port 8080 only.
     */
    private static int mappedPort(int port) {
        if (port != 8080) {
            throw new IllegalArgumentException("Requested port (" + port + ") is not mapped");
        }
        return 1234;
    }

    private static DevServiceSpec linkyService() {
        return new DevServiceSpec()
                .name("linky")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestLinkContainer.class, TestLinkContainer::new);
    }

}
