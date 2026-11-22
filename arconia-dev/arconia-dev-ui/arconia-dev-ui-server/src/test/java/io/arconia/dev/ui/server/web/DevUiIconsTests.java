package io.arconia.dev.ui.server.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiIcons}.
 */
class DevUiIconsTests {

    private final DevUiIcons icons = new DevUiIcons(DevUiImportMap.load());

    @Test
    void iconsAreServedFromTheIconLibrary() {
        assertThat(this.icons.path("/app/a/dev-ui")).matches("/app/a/dev-ui/_static/lucide-static/[^/]+/icons/");
    }

    @Test
    void iconIsServedAtItsPath() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        DevUiStaticResources.write(this.icons.path("") + "container.svg", response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("image/svg+xml");
    }

    @Test
    void iconOfTheIconSetExists() {
        assertThat(this.icons.has("container")).isTrue();
        assertThat(this.icons.has(DevUiIcons.FALLBACK)).isTrue();
    }

    @Test
    void iconThatIsNotInTheIconSetDoesNotExist() {
        assertThat(this.icons.has("no-such-icon")).isFalse();
    }

}
