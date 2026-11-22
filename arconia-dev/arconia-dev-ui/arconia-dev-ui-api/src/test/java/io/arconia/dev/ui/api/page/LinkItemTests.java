package io.arconia.dev.ui.api.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link LinkItem}.
 */
class LinkItemTests {

    @ParameterizedTest
    @ValueSource(strings = { "http://localhost:3000", "https://localhost:3000/dashboards?orgId=1", "HTTP://127.0.0.1" })
    void urlCanBeHttpOrHttps(String url) {
        assertThat(LinkItem.of("Grafana", url).getUrl()).isEqualTo(url);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/swagger-ui.html", "/h2-console", "/actuator/health?details=true" })
    void urlCanBePathOfTheApplication(String url) {
        LinkItem link = LinkItem.of("Console", url);

        assertThat(link.getUrl()).isEqualTo(url);
    }

    @ParameterizedTest
    @ValueSource(strings = { "javascript:alert(1)", "file:///etc/passwd", "localhost:3000", "relative", "//evil.example.com", "http://", "http://local host" })
    void whenUrlIsNeitherHttpNorPathOfTheApplicationThenThrow(String url) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LinkItem.of("Grafana", url))
                .withMessage("url '%s' of the link 'Grafana' must be an HTTP or HTTPS URL, or a path that starts with a slash", url);
    }

    @Test
    void whenLabelIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LinkItem.of("", "http://localhost:3000"))
                .withMessage("label cannot be null or empty");
    }

    @Test
    void whenUrlIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LinkItem.of("Grafana", ""))
                .withMessage("url of the link 'Grafana' cannot be null or empty");
    }

}
