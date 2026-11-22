package io.arconia.dev.ui.server.web;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import io.arconia.dev.ui.api.page.CardsPage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiServlet}.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevUiServletTests {

    private final BasicJsonTester json = new BasicJsonTester(getClass());

    private final DevUiServlet servlet = new DevUiServlet(List.of(
            CardsPage.builder().id("dev-services").title("Dev Services").cards(List::of).build()), new DevUiLogs(), application());

    @BeforeEach
    void initializeServlet() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        servletContext.setContextPath("/app");
        this.servlet.init(new MockServletConfig(servletContext));
    }

    @Test
    void requestWithoutTrailingSlashIsRedirected() throws Exception {
        MockHttpServletResponse response = get(null);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("/app/a/dev-ui/");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/", "/pages/dev-services", "/pages/unknown" })
    void shellIsServedForTheDevUiAndItsPages(String pathInfo) throws Exception {
        MockHttpServletResponse response = get(pathInfo);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-cache");
        assertThat(response.getHeader("Content-Security-Policy")).isEqualTo("frame-ancestors 'none'");
        assertThat(response.getContentAsString())
                .contains("<title>catalog-service · Arconia Dev UI</title>")
                .contains("src=\"/app/a/dev-ui/static/app/app.js\"");
    }

    @Test
    void staticResourceIsServed() throws Exception {
        MockHttpServletResponse response = get("/static/app/app.js");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("text/javascript;charset=UTF-8");
    }

    @Test
    void apiIsServed() throws Exception {
        MockHttpServletResponse response = get("/api/pages/dev-services");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(this.json.from(response.getContentAsString())).isStrictlyEqualToJson("""
                { "id": "dev-services", "title": "Dev Services", "type": "cards", "icon": "layout-grid", "columns": 3, "cards": [] }
                """);
    }

    @Test
    void libraryFileIsServed() throws Exception {
        MockHttpServletResponse response = get("/_static/dedupe-mixin/index.js");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("text/javascript;charset=UTF-8");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/unknown", "/page/dev-services", "/unknown/" })
    void unknownAddressIsAnsweredByTheDevUiItselfAsNotFound(String pathInfo) throws Exception {
        MockHttpServletResponse response = get(pathInfo);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("src=\"/app/a/dev-ui/static/app/app.js\"");
        assertThat(response.getErrorMessage()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/favicon.ico", "/unknown.js", "/missing/logo.svg" })
    void unknownFileIsNotFound(String pathInfo) throws Exception {
        MockHttpServletResponse response = get(pathInfo);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).isEqualTo("Not Found");
    }

    @Test
    void unsupportedMethodIsNotAllowed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", DevUiServlet.BASE_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.servlet.service(request, response);

        assertThat(response.getStatus()).isEqualTo(405);
        assertThat(response.getHeader("Allow")).isEqualTo("GET, HEAD");
        assertThat(response.getContentAsString()).isEqualTo("Method Not Allowed");
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    void restartIsNotPossibleWithoutDevTools() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", DevUiServlet.BASE_PATH);
        request.setPathInfo("/api/actions/restart");
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.servlet.service(request, response);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(this.json.from(response.getContentAsString())).isStrictlyEqualToJson("""
                {
                  "error": {
                    "status": 409,
                    "message": "The application cannot be restarted: Spring Boot DevTools is not there or its restart support is not in use"
                  }
                }
                """);
        assertThat(get("/").getContentAsString()).contains("application-restart=\"false\"");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/actions/unknown", "/api/actions/" })
    void unknownActionIsNotFound(String pathInfo) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", DevUiServlet.BASE_PATH);
        request.setPathInfo(pathInfo);
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.servlet.service(request, response);

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/pages", "/", "/static/app/app.js" })
    void onlyActionsCanBePosted(String pathInfo) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", DevUiServlet.BASE_PATH);
        request.setPathInfo(pathInfo);
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.servlet.service(request, response);

        assertThat(response.getStatus()).isEqualTo(405);
    }

    @Test
    void responsesOfTheApiTellWhenTheApplicationStarted() throws Exception {
        MockHttpServletResponse response = get("/api/pages");

        assertThat(response.getHeader("Arconia-Dev-UI-Started-At")).matches("\\d{13}");
        assertThat(get("/").getContentAsString()).contains("application-started-at=\"" + response.getHeader("Arconia-Dev-UI-Started-At") + "\"");
        assertThat(get("/static/app/app.js").getHeader("Arconia-Dev-UI-Started-At")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/pages", "/api/pages/dev-services", "/api/pages/dev-services/logs" })
    void apiOnlyAnswersRequestsThatReadItsResponse(String pathInfo) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("HEAD", DevUiServlet.BASE_PATH);
        request.setPathInfo(pathInfo);
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.servlet.service(request, response);

        assertThat(response.getStatus()).isEqualTo(405);
        assertThat(response.getHeader("Allow")).isEqualTo("GET");
    }

    @Test
    void whenTheDevUiCannotBeInitializedThenItIsNotAvailableAndTheApplicationStarts(CapturedOutput output) throws Exception {
        DevUiServlet brokenServlet = new DevUiServlet(List.of(), new DevUiLogs(), application());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new WithoutImportMaps(classLoader));
        try {
            brokenServlet.init(new MockServletConfig(new MockServletContext()));
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        MockHttpServletRequest request = new MockHttpServletRequest("GET", DevUiServlet.BASE_PATH);
        request.setPathInfo("/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        brokenServlet.service(request, response);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).isEqualTo("The Dev UI is not available. See the application log for the reason.");
        assertThat(response.getErrorMessage()).isNull();
        assertThat(output).contains("The Dev UI is not available because it could not be initialized")
                .contains("The icon library of the Dev UI is not on the class path");
    }

    /**
     * A class path where the frontend libraries are missing: none of their import maps is found.
     */
    private static final class WithoutImportMaps extends ClassLoader {

        private WithoutImportMaps(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return name.equals("META-INF/importmap.json") ? Collections.emptyEnumeration() : super.getResources(name);
        }

    }

    private static DevUiApplication application() {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        TestPropertyValues.of("spring.application.name=catalog-service").applyTo(applicationContext);
        applicationContext.refresh();
        return new DevUiApplication(applicationContext);
    }

    private MockHttpServletResponse get(String pathInfo) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", DevUiServlet.BASE_PATH);
        request.setContextPath("/app");
        request.setServletPath(DevUiServlet.BASE_PATH);
        request.setPathInfo(pathInfo);
        MockHttpServletResponse response = new MockHttpServletResponse();
        this.servlet.service(request, response);
        return response;
    }

}
