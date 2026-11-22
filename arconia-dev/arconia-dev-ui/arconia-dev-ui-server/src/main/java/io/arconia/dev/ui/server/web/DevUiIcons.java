package io.arconia.dev.ui.server.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;

/**
 * The icons of the Dev UI, which are the ones of the Lucide icon set. The library ships each
 * icon as an SVG file in its jar, named after the icon.
 */
final class DevUiIcons {

    /**
     * The icon shown in place of one that the icon set doesn't have.
     */
    static final String FALLBACK = "file";

    private static final String LIBRARY = "lucide-static/";

    private final String path;

    private final Map<String, Boolean> known = new ConcurrentHashMap<>();

    /**
     * Finds where the icon library keeps its icons. Its import map points at the directory of
     * its scripts, and the icons are in the directory next to it.
     */
    DevUiIcons(DevUiImportMap importMap) {
        String scriptsPath = importMap.path(LIBRARY);
        Assert.state(scriptsPath != null && scriptsPath.endsWith("/dist/"), "The icon library of the Dev UI is not on the class path");
        this.path = scriptsPath.substring(0, scriptsPath.length() - "dist/".length()) + "icons/";
    }

    /**
     * The path the icons are served at, for a Dev UI served at the given path.
     */
    String path(String basePath) {
        return basePath + this.path;
    }

    /**
     * Whether the icon set has an icon with the given name. The answer is remembered, since
     * the same few names are asked about for every item of every page.
     */
    boolean has(String name) {
        return this.known.computeIfAbsent(name,
                icon -> new ClassPathResource(DevUiStaticResources.LIBRARIES_LOCATION + this.path + icon + ".svg").exists());
    }

}
