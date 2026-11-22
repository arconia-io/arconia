package io.arconia.dev.ui.server.error;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;
import org.springframework.web.util.HtmlUtils;

import io.arconia.dev.ui.server.error.ApplicationEndpoints.Endpoint;
import io.arconia.dev.ui.server.web.DevUiServlet;

/**
 * A page that the Dev UI shows in place of the whitelabel error page: the welcome page, the
 * page for a path that doesn't exist, or the page for a failed request.
 * <p>
 * The page is plain HTML written here, with no template engine, so that it can be shown
 * whatever the state of the application and of the rest of the Dev UI. It is complete without
 * a script. The one it has adds the button that copies a stack trace.
 */
final class DevUiErrorPage implements View {

    /**
     * The button that copies a stack trace, with the icon it has at rest and the one it has
     * for a moment after copying.
     */
    private static final String COPY_BUTTON = """
            <button type="button" class="copy" aria-label="Copy the stack trace" title="Copy the stack trace" hidden>
            <svg class="copy-icon" viewBox="0 0 24 24" aria-hidden="true"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/>\
            <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>
            <svg class="copied-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>
            </button>
            """;

    private static final String CONTENT_TYPE = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8).toString();

    private static final String CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy";

    private final String title;

    private final String lead;

    private final String content;

    private final String contextPath;

    /**
     * Whether the page welcomes the developer, with an invitation to open the Dev UI, or
     * reports that something is missing or failed.
     */
    private final boolean welcome;

    private DevUiErrorPage(String title, String lead, String content, String contextPath) {
        this(title, lead, content, contextPath, false);
    }

    private DevUiErrorPage(String title, String lead, String content, String contextPath, boolean welcome) {
        this.title = title;
        this.lead = lead;
        this.content = content;
        this.contextPath = contextPath;
        this.welcome = welcome;
    }

    /**
     * The page for the root path of an application that has nothing there yet.
     */
    static DevUiErrorPage welcome(List<Endpoint> endpoints, String contextPath) {
        String content = endpoints.isEmpty() ? firstEndpoint() : endpoints("Endpoints of this application", endpoints, contextPath);
        return new DevUiErrorPage("Your application is running",
                "Nothing is served at the root path yet. " + (endpoints.isEmpty()
                        ? "Add a first endpoint, and this page makes way for it."
                        : "Add an endpoint for it, or open one of the endpoints below."),
                content, contextPath, true);
    }

    /**
     * The page for a path that the application doesn't serve.
     */
    static DevUiErrorPage notFound(String path, List<Endpoint> endpoints, String contextPath) {
        String content = endpoints.isEmpty() ? "" : endpoints("Endpoints of this application", endpoints, contextPath);
        return new DevUiErrorPage("Nothing here",
                "The application serves nothing at <code>" + escape(path) + "</code>.", content, contextPath);
    }

    /**
     * The page for the error path of Spring Boot when it is opened directly. Spring Boot sends
     * a failed request there to show its error, and no request failed in this case, so a page
     * about a failure would be misleading.
     */
    static DevUiErrorPage nothingFailed(String errorPath, String contextPath) {
        return new DevUiErrorPage("Nothing failed",
                "<code>" + escape(errorPath) + "</code> is where Spring Boot shows the error of a request that failed. "
                        + "No request failed here, so there is nothing to show.",
                "", contextPath);
    }

    /**
     * The page for a request that failed, with the exception that made it fail when there is
     * one. The exception that explains the failure comes first: the root cause, with its
     * message.
     * <p>
     * Its stack trace follows. It is open for a failure of the application, and closed for a
     * request the application rejected, where the message usually says it all. When the root
     * cause is wrapped in other exceptions, the stack trace can also be read in the order Java
     * prints it. Both choices are made with plain HTML and CSS, so that they work without a script.
     * A button copies the stack trace on screen. It needs a script, and is hidden without it.
     */
    static DevUiErrorPage error(HttpStatus status, String method, String path, @Nullable Throwable exception,
            List<String> applicationPackages, String contextPath) {
        StringBuilder content = new StringBuilder();
        if (exception != null) {
            DevUiStackTrace stackTrace = new DevUiStackTrace(exception, applicationPackages);
            Throwable rootCause = stackTrace.rootCause();
            content.append("<section><div class=\"failure\"><h2>").append(escape(rootCause.getClass().getName())).append("</h2>");
            if (rootCause.getMessage() != null) {
                content.append("<p class=\"message\">").append(escape(rootCause.getMessage())).append("</p>");
            }
            content.append("</div>");
            content.append(status.is4xxClientError() ? "<details class=\"trace\">" : "<details class=\"trace\" open>")
                    .append("<summary>Stack trace</summary>");
            if (stackTrace.hasCause()) {
                content.append("""
                        <input type="radio" name="trace" id="trace-root-cause" checked><label for="trace-root-cause">Root cause first</label>
                        <input type="radio" name="trace" id="trace-original"><label for="trace-original">Original order</label>
                        <pre class="root-cause" tabindex="0" aria-label="Stack trace, root cause first">%s</pre>
                        <pre class="original" tabindex="0" aria-label="Stack trace, original order">%s</pre>
                        """.formatted(stackTrace.rootCauseFirst(), stackTrace.original()));
            } else {
                content.append("<pre tabindex=\"0\" aria-label=\"Stack trace\">").append(stackTrace.original()).append("</pre>");
            }
            content.append(COPY_BUTTON).append("</details></section>")
                    .append("<script type=\"module\" src=\"").append(escape(contextPath)).append(DevUiServlet.BASE_PATH)
                    .append("/static/app/error-pages.js\"></script>");
        }
        return new DevUiErrorPage(status.value() + " " + status.getReasonPhrase(),
                "<code>" + escape(method) + " " + escape(path) + "</code> failed.", content.toString(), contextPath);
    }

    private static String firstEndpoint() {
        return """
                <section>
                <h2>Add a first endpoint</h2>
                <p>Create a controller in the package of your application, next to its main class:</p>
                <pre tabindex="0" aria-label="Example of a controller">%s</pre>
                <p>Then restart the application and reload this page.</p>
                </section>
                """.formatted(escape("""
                @RestController
                class HomeController {

                    @GetMapping("/")
                    String home() {
                        return "Hello from my application!";
                    }

                }"""));
    }

    private static String endpoints(String heading, List<Endpoint> endpoints, String contextPath) {
        StringBuilder list = new StringBuilder("<section><h2>").append(escape(heading)).append("</h2><ul class=\"endpoints\">");
        for (Endpoint endpoint : endpoints) {
            String path = escape(endpoint.path());
            list.append("<li><span class=\"methods\">").append(escape(endpoint.methods().isEmpty() ? "ANY" : endpoint.methods())).append("</span>");
            if (endpoint.canBeOpened()) {
                list.append("<a href=\"").append(escape(contextPath)).append(path).append("\">").append(path).append("</a>");
            } else {
                list.append("<code>").append(path).append("</code>");
            }
            list.append("</li>");
        }
        return list.append("</ul></section>").toString();
    }

    private static String escape(String text) {
        return HtmlUtils.htmlEscape(text);
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        byte[] page = html().getBytes(StandardCharsets.UTF_8);
        response.setContentType(CONTENT_TYPE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
        // Like the Dev UI, the page may not be shown inside a frame by another page.
        response.setHeader(CONTENT_SECURITY_POLICY_HEADER, "frame-ancestors 'none'");
        response.setContentLength(page.length);
        response.getOutputStream().write(page);
    }

    String html() {
        String devUiPath = escape(this.contextPath) + DevUiServlet.BASE_PATH;
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <link rel="icon" href="%s/static/app/images/favicon.png">
                    <link rel="stylesheet" href="%s/static/app/brand.css">
                    <link rel="stylesheet" href="%s/static/app/error-pages.css">
                </head>
                <body>
                    <header>
                        <a class="arconia-brand" href="%s/" aria-label="Arconia Dev UI">
                            <span class="arconia-brand-logo" role="img" aria-label="Arconia"></span>
                            <span class="arconia-brand-product">Dev UI</span>
                        </a>
                        <a class="dev-ui" href="%s/">Open the Dev UI</a>
                    </header>
                    %s
                </body>
                </html>
                """.formatted(escape(this.title), devUiPath, devUiPath, devUiPath, devUiPath, devUiPath, body(devUiPath));
    }

    /**
     * What the page says. The welcome page says it in a band of its own, with what the Dev UI
     * is for and a button to open it, above what the application has to offer.
     */
    private String body(String devUiPath) {
        if (!this.welcome) {
            return """
                    <main>
                        <h1>%s</h1>
                        <p class="lead">%s</p>
                        %s
                    </main>""".formatted(escape(this.title), this.lead, this.content);
        }
        return """
                <div class="welcome">
                    <div>
                        <h1>%s</h1>
                        <p class="lead">%s</p>
                        <p>The Arconia Dev UI shows what your application is made of while you develop it: \
                its configuration, its logs, and the services it uses.</p>
                        <a class="open" href="%s/">Open the Dev UI</a>
                    </div>
                </div>
                <main>
                    %s
                </main>""".formatted(escape(this.title), this.lead, devUiPath, this.content);
    }

}
