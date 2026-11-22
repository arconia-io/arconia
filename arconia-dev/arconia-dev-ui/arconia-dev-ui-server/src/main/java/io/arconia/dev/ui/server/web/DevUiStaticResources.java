package io.arconia.dev.ui.server.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

/**
 * Serves the static resources of the Dev UI from the class path: its own files and the files
 * of the frontend libraries, which each library ships in its jar.
 */
final class DevUiStaticResources {

    /**
     * The path of the files of the Dev UI itself, relative to the path of the Dev UI.
     */
    static final String PATH = "/static/";

    /**
     * The path of the files of the frontend libraries, relative to the path of the Dev UI. It
     * is the path each library keeps its files at in its jar, and the one its import map uses.
     */
    static final String LIBRARIES_PATH = "/_static/";

    /**
     * Where the libraries are on the class path: each one keeps its files in its jar, under
     * the same path as the one they are served at.
     */
    static final String LIBRARIES_LOCATION = "META-INF/resources";

    /**
     * The extensions of the files that can be served. Anything else is not found.
     */
    private static final Set<String> EXTENSIONS = Set.of("css", "js", "json", "png", "svg", "ttf", "woff", "woff2");

    /**
     * The files of a library never change, since their path includes the version of the
     * library, so the browser can keep them.
     */
    private static final CacheControl KEEP = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    /**
     * The files of the Dev UI itself are checked on every request.
     */
    private static final CacheControl CHECK = CacheControl.noCache();

    private DevUiStaticResources() {}

    /**
     * Writes the resource at the given path, relative to the path of the Dev UI, or a not
     * found response when there is none.
     */
    static void write(String path, HttpServletResponse response) throws IOException {
        MediaType contentType = contentType(path);
        if (contentType == null || path.contains("..") || path.contains("\\")) {
            DevUiResponses.writeStatus(response, HttpServletResponse.SC_NOT_FOUND, "Not Found");
            return;
        }
        ClassPathResource resource = path.startsWith(LIBRARIES_PATH)
                ? new ClassPathResource(LIBRARIES_LOCATION + path)
                : new ClassPathResource(path.substring(1), DevUiStaticResources.class);
        if (!resource.exists()) {
            DevUiResponses.writeStatus(response, HttpServletResponse.SC_NOT_FOUND, "Not Found");
            return;
        }
        response.setContentType(contentType.toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, (path.startsWith(LIBRARIES_PATH) ? KEEP : CHECK).getHeaderValue());
        try (InputStream inputStream = resource.getInputStream()) {
            inputStream.transferTo(response.getOutputStream());
        }
    }

    /**
     * The content-type of a file that can be served, which Spring knows from its extension.
     * Text is sent as UTF-8.
     */
    private static @Nullable MediaType contentType(String path) {
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 1 || !EXTENSIONS.contains(fileName.substring(extensionIndex + 1))) {
            return null;
        }
        return MediaTypeFactory.getMediaType(fileName)
                .map(type -> "text".equals(type.getType()) ? new MediaType(type, StandardCharsets.UTF_8) : type)
                .orElse(null);
    }

}
