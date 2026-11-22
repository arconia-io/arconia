package io.arconia.dev.ui.server.error;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.arconia.dev.ui.server.error.ApplicationEndpoints.Endpoint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiErrorPage}.
 */
class DevUiErrorPageTests {

    @Test
    void welcomePageExplainsHowToAddFirstEndpointWhenThereIsNone() {
        String page = DevUiErrorPage.welcome(List.of(), "/shop").html();

        assertThat(page)
                .contains("<h1>Your application is running</h1>")
                .contains("Add a first endpoint")
                .contains("@GetMapping(&quot;/&quot;)")
                .contains("<a class=\"dev-ui\" href=\"/shop/a/dev-ui/\">Open the Dev UI</a>")
                .contains("<link rel=\"stylesheet\" href=\"/shop/a/dev-ui/static/app/error-pages.css\">");
    }

    @Test
    void welcomePageInvitesToOpenTheDevUiAndOtherPagesDoNot() {
        assertThat(DevUiErrorPage.welcome(List.of(), "/shop").html())
                .contains("<div class=\"welcome\">")
                .contains("its configuration, its logs, and the services it uses.</p>")
                .contains("<a class=\"open\" href=\"/shop/a/dev-ui/\">Open the Dev UI</a>");
        assertThat(DevUiErrorPage.notFound("/boks", List.of(), "").html())
                .doesNotContain("class=\"welcome\"")
                .doesNotContain("class=\"open\"");
    }

    @Test
    void welcomePageListsTheEndpointsOfTheApplicationWhenThereAreSome() {
        String page = DevUiErrorPage.welcome(List.of(new Endpoint("GET", "/books"), new Endpoint("POST", "/books")), "/shop").html();

        assertThat(page)
                .contains("Endpoints of this application")
                .contains("<span class=\"methods\">GET</span><a href=\"/shop/books\">/books</a>")
                .contains("<span class=\"methods\">POST</span><code>/books</code>")
                .doesNotContain("Add a first endpoint");
    }

    @Test
    void notFoundPageNamesThePathAndListsTheEndpoints() {
        String page = DevUiErrorPage.notFound("/boks", List.of(new Endpoint("", "/books/{id}")), "").html();

        assertThat(page)
                .contains("<h1>Nothing here</h1>")
                .contains("The application serves nothing at <code>/boks</code>.")
                .contains("<span class=\"methods\">ANY</span><code>/books/{id}</code>")
                .contains("<a class=\"dev-ui\" href=\"/a/dev-ui/\">Open the Dev UI</a>");
    }

    @Test
    void errorPageHasButtonToCopyTheStackTraceWhichIsHiddenUntilItsScriptRuns() {
        String page = DevUiErrorPage.error(HttpStatus.INTERNAL_SERVER_ERROR, "GET", "/books",
                new IllegalStateException("The catalog is closed"), List.of(), "/shop").html();

        assertThat(page)
                .contains("<button type=\"button\" class=\"copy\" aria-label=\"Copy the stack trace\" title=\"Copy the stack trace\" hidden>")
                .contains("<script type=\"module\" src=\"/shop/a/dev-ui/static/app/error-pages.js\"></script>");
    }

    @Test
    void errorPageWithoutAnExceptionHasNoScript() {
        String page = DevUiErrorPage.error(HttpStatus.NOT_ACCEPTABLE, "GET", "/books", null, List.of(), "").html();

        assertThat(page).doesNotContain("<script").doesNotContain("class=\"copy\"");
    }

    @Test
    void errorPageShowsTheExceptionAndItsStackTrace() {
        String page = DevUiErrorPage.error(HttpStatus.INTERNAL_SERVER_ERROR, "GET", "/books",
                new IllegalStateException("The catalog is closed"), List.of(), "").html();

        assertThat(page)
                .contains("<h1>500 Internal Server Error</h1>")
                .contains("<code>GET /books</code> failed.")
                .contains("<div class=\"failure\"><h2>java.lang.IllegalStateException</h2><p class=\"message\">The catalog is closed</p></div>")
                .contains("<details class=\"trace\" open><summary>Stack trace</summary><pre tabindex=\"0\" aria-label=\"Stack trace\">java.lang.IllegalStateException: The catalog is closed")
                .contains("at io.arconia.dev.ui.server.error.DevUiErrorPageTests.errorPageShowsTheExceptionAndItsStackTrace")
                .doesNotContain("<input");
    }

    @Test
    void errorPageStartsFromTheRootCauseAndOffersTheOriginalOrder() {
        Exception exception = new IllegalStateException("The catalog is closed", new IllegalArgumentException("No such shelf"));

        String page = DevUiErrorPage.error(HttpStatus.INTERNAL_SERVER_ERROR, "GET", "/books", exception, List.of(), "").html();

        assertThat(page)
                .contains("<h2>java.lang.IllegalArgumentException</h2>")
                .contains("<p class=\"message\">No such shelf</p>")
                .contains("<input type=\"radio\" name=\"trace\" id=\"trace-root-cause\" checked><label for=\"trace-root-cause\">Root cause first</label>")
                .contains("<input type=\"radio\" name=\"trace\" id=\"trace-original\"><label for=\"trace-original\">Original order</label>")
                .contains("aria-label=\"Stack trace, root cause first\">java.lang.IllegalArgumentException: No such shelf")
                .contains("aria-label=\"Stack trace, original order\">java.lang.IllegalStateException: The catalog is closed");
    }

    @Test
    void errorPageTellsTheFramesOfTheApplicationApart() {
        String page = DevUiErrorPage.error(HttpStatus.INTERNAL_SERVER_ERROR, "GET", "/books",
                new IllegalStateException("The catalog is closed"), List.of("io.arconia.dev.ui.server"), "").html();

        assertThat(page)
                .contains("<span class=\"application\">\tat io.arconia.dev.ui.server.error.DevUiErrorPageTests.errorPageTellsTheFramesOfTheApplicationApart")
                .contains("<span class=\"library\">\tat ");
    }

    @Test
    void stackTraceIsClosedForRequestThatTheApplicationRejected() {
        String page = DevUiErrorPage.error(HttpStatus.BAD_REQUEST, "POST", "/books",
                new IllegalArgumentException("The title is missing"), List.of(), "").html();

        assertThat(page)
                .contains("<h1>400 Bad Request</h1>")
                .contains("<code>POST /books</code> failed.")
                .contains("<p class=\"message\">The title is missing</p>")
                .contains("<details class=\"trace\"><summary>Stack trace</summary>");
    }

    @Test
    void pageForTheErrorPathOpenedDirectlySaysThatNothingFailed() {
        String page = DevUiErrorPage.nothingFailed("/error", "").html();

        assertThat(page)
                .contains("<h1>Nothing failed</h1>")
                .contains("<code>/error</code> is where Spring Boot shows the error of a request that failed.")
                .doesNotContain("<pre");
    }

    @Test
    void errorPageHasNoExceptionWhenThereIsNone() {
        String page = DevUiErrorPage.error(HttpStatus.FORBIDDEN, "GET", "/admin", null, List.of(), "").html();

        assertThat(page).contains("<h1>403 Forbidden</h1>").doesNotContain("<pre");
    }

    @Test
    void textFromTheRequestAndTheApplicationIsEscaped() {
        String script = "<script>alert(1)</script>";

        assertThat(DevUiErrorPage.notFound("/" + script, List.of(new Endpoint("GET", "/" + script)), "").html())
                .doesNotContain(script);
        assertThat(DevUiErrorPage.error(HttpStatus.BAD_REQUEST, "<b>", "/" + script, new IllegalArgumentException(script), List.of(), "").html())
                .doesNotContain(script);
    }

    @Test
    void pageIsWrittenAsHtml() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        DevUiErrorPage.welcome(List.of(), "").render(Map.of(), new MockHttpServletRequest(), response);

        assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentAsString()).startsWith("<!DOCTYPE html>");
    }

}
