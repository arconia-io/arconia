package io.arconia.dev.ui.server.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Internal;
import io.arconia.dev.ui.api.page.DevUiPage;

/**
 * Serves the Dev UI.
 */
@Internal
public final class DevUiServlet extends HttpServlet {

    /**
     * The path of the Dev UI, relative to the context path of the application.
     */
    public static final String BASE_PATH = "/a/dev-ui";

    /**
     * The path of the pages, relative to the path of the Dev UI. The frontend shows the page
     * itself, so the same HTML page is served for all of them.
     */
    private static final String PAGES_PATH = "/pages/";

    /**
     * The header of the responses of the API that carries when the application started.
     */
    static final String STARTED_AT_HEADER = "Arconia-Dev-UI-Started-At";

    private static final String SHELL_CONTENT_TYPE = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8).toString();

    private static final String CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy";

    private static final Logger logger = LoggerFactory.getLogger(DevUiServlet.class);

    private final Collection<DevUiPage> pages;

    private final DevUiLogs logs;

    private final DevUiApplication application;

    private @Nullable DevUiApi api;

    private byte @Nullable [] shell;

    public DevUiServlet(Collection<DevUiPage> pages, DevUiLogs logs, DevUiApplication application) {
        Assert.notNull(pages, "pages cannot be null");
        Assert.notNull(logs, "logs cannot be null");
        Assert.notNull(application, "application cannot be null");
        this.pages = pages;
        this.logs = logs;
        this.application = application;
    }

    /**
     * Prepares what the Dev UI serves, once the context path of the application is known.
     * <p>
     * When that fails, for example because a frontend library is not on the class path, the
     * Dev UI is not available and says so in the application log and to the browser. It must
     * not stop the application from starting.
     */
    @Override
    public void init() {
        try {
            String basePath = getServletContext().getContextPath() + BASE_PATH;
            DevUiImportMap importMap = DevUiImportMap.load();
            DevUiIcons icons = new DevUiIcons(importMap);
            this.shell = DevUiShell.render(basePath, importMap, icons, this.application);
            this.api = new DevUiApi(new DevUiPages(this.pages, icons), this.logs, this.application);
        } catch (RuntimeException | LinkageError ex) {
            logger.warn("The Dev UI is not available because it could not be initialized. The application is not affected.", ex);
        }
    }

    /**
     * Handles every request itself. The default handling of the unsupported methods and
     * {@code sendError} would hand the response over to the error handling of the application.
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        DevUiApi api = this.api;
        byte[] shell = this.shell;
        if (api == null || shell == null) {
            DevUiResponses.writeStatus(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "The Dev UI is not available. See the application log for the reason.");
            return;
        }
        String method = request.getMethod();
        boolean get = HttpMethod.GET.matches(method);
        String path = request.getPathInfo();
        if (HttpMethod.POST.matches(method) && path != null && path.startsWith(DevUiApi.ACTIONS_PATH)) {
            // What changes something in the application is asked for with a POST request.
            response.setHeader(STARTED_AT_HEADER, String.valueOf(this.application.startedAt()));
            api.act(path, response);
            return;
        }
        if (!get && !HttpMethod.HEAD.matches(method)) {
            response.setHeader(HttpHeaders.ALLOW, HttpMethod.GET + ", " + HttpMethod.HEAD);
            DevUiResponses.writeStatus(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
            return;
        }
        if (path == null) {
            // The frontend resolves its pages against the path of the Dev UI ending with a slash.
            response.sendRedirect(request.getContextPath() + BASE_PATH + "/");
            return;
        }
        if (path.equals("/") || path.startsWith(PAGES_PATH)) {
            writeShell(shell, HttpServletResponse.SC_OK, response);
            return;
        }
        if (path.startsWith(DevUiApi.PATH)) {
            if (!get) {
                // A request without a body to answer must not start reading a page or a log.
                response.setHeader(HttpHeaders.ALLOW, HttpMethod.GET.name());
                DevUiResponses.writeStatus(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
                return;
            }
            // The frontend finds out from any response that the application was restarted.
            response.setHeader(STARTED_AT_HEADER, String.valueOf(this.application.startedAt()));
            api.write(path, request, response);
            return;
        }
        if (path.startsWith(DevUiStaticResources.PATH) || path.startsWith(DevUiStaticResources.LIBRARIES_PATH)) {
            DevUiStaticResources.write(path, response);
            return;
        }
        if (get && !isFile(path)) {
            // An address that someone typed or followed, and that leads nowhere: the Dev UI says
            // so itself, with its navigation as the way back.
            writeShell(shell, HttpServletResponse.SC_NOT_FOUND, response);
            return;
        }
        DevUiResponses.writeStatus(response, HttpServletResponse.SC_NOT_FOUND, "Not Found");
    }

    /**
     * Whether the path names a file, which a page asks for, as opposed to a page itself.
     */
    private static boolean isFile(String path) {
        return path.substring(path.lastIndexOf('/') + 1).contains(".");
    }

    private static void writeShell(byte[] shell, int status, HttpServletResponse response) throws IOException {
        response.setStatus(status);
        response.setContentType(SHELL_CONTENT_TYPE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue());
        // No page may show the Dev UI inside a frame, where it could get a click meant for itself.
        response.setHeader(CONTENT_SECURITY_POLICY_HEADER, "frame-ancestors 'none'");
        response.setContentLength(shell.length);
        response.getOutputStream().write(shell);
    }

}
