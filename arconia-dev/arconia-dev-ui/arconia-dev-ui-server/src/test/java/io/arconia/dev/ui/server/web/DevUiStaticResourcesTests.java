package io.arconia.dev.ui.server.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiStaticResources}.
 */
class DevUiStaticResourcesTests {

    @Test
    void fileOfTheDevUiIsCheckedOnEveryRequest() throws Exception {
        MockHttpServletResponse response = write("/static/app/app.css");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("text/css;charset=UTF-8");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-cache");
        assertThat(response.getContentAsString()).contains("color-scheme");
    }

    @Test
    void fileOfLibraryIsKeptByTheBrowser() throws Exception {
        MockHttpServletResponse response = write("/_static/dedupe-mixin/index.js");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("text/javascript;charset=UTF-8");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("max-age=31536000, public, immutable");
    }

    @Test
    void fontOfTheDevUiIsServed() throws Exception {
        MockHttpServletResponse response = write("/static/app/fonts/league-spartan-variable.woff2");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("font/woff2");
    }

    @Test
    void imagesOfTheDevUiAreServed() throws Exception {
        assertThat(write("/static/app/images/arconia-horizontal.svg").getContentType()).isEqualTo("image/svg+xml");
        assertThat(write("/static/app/images/favicon.png").getContentType()).isEqualTo("image/png");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/static/app/missing.js",
            "/static/app/fonts/league-spartan-OFL.txt",
            "/static/app/",
            "/static/app",
            "/static/../shell.html",
            "/static/app/../../shell.html",
            "/static/app/..\\..\\shell.html",
            "/static/../DevUiServlet.class",
            "/static/app/.js",
            "/_static/dedupe-mixin/LICENSE",
            "/_static/../spring.factories",
            "/_static/missing/index.js" })
    void anythingElseIsNotFound(String path) throws Exception {
        MockHttpServletResponse response = write(path);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).isEqualTo("Not Found");
    }

    private static MockHttpServletResponse write(String path) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        DevUiStaticResources.write(path, response);
        return response;
    }

}
