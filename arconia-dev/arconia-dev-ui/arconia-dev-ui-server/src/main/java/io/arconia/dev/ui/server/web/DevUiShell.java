package io.arconia.dev.ui.server.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.util.HtmlUtils;

import io.arconia.dev.ui.server.logging.ApplicationLogs;

/**
 * The HTML page that loads the Dev UI in the browser.
 */
final class DevUiShell {

    private DevUiShell() {}

    /**
     * Renders the page for a Dev UI served at the given path, context path included. The page
     * carries the name of the application, in its title too, so that the Dev UI of one
     * application can be told from the one of another.
     */
    static byte[] render(String basePath, DevUiImportMap importMap, DevUiIcons icons, DevUiApplication application) {
        String version = application.version();
        return loadTemplate()
                .replace("{{applicationName}}", HtmlUtils.htmlEscape(application.name()))
                .replace("{{applicationVersion}}", HtmlUtils.htmlEscape((version != null) ? version : ""))
                .replace("{{applicationStartedAt}}", String.valueOf(application.startedAt()))
                .replace("{{applicationLogs}}", String.valueOf(ApplicationLogs.get().isCapturing()))
                .replace("{{applicationRestart}}", String.valueOf(application.canRestart()))
                .replace("{{basePath}}", HtmlUtils.htmlEscape(basePath))
                .replace("{{iconsPath}}", HtmlUtils.htmlEscape(icons.path(basePath)))
                .replace("{{staticPath}}", HtmlUtils.htmlEscape(basePath + DevUiStaticResources.PATH))
                .replace("{{themeStylesheet}}", HtmlUtils.htmlEscape(importMap.themeStylesheet(basePath)))
                .replace("{{importMap}}", importMap.toJson(basePath))
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String loadTemplate() {
        try (InputStream inputStream = new ClassPathResource("shell.html", DevUiShell.class).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot load the Dev UI shell", ex);
        }
    }

}
