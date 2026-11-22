package io.arconia.dev.ui.server.web;

import java.util.Map;
import java.util.TreeMap;

import io.mvnpm.importmap.Aggregator;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import tools.jackson.databind.json.JsonMapper;

/**
 * The import map of the Dev UI, which tells the browser where to load each frontend library
 * from. Each library ships its own import map in its jar, and the Dev UI merges the ones on
 * the class path.
 */
final class DevUiImportMap {

    private static final String USAGE_STATISTICS = "@vaadin/vaadin-usage-statistics/";

    private static final String THEME = "@vaadin/aura/";

    private static final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final Map<String, String> imports;

    private DevUiImportMap(Map<String, String> imports) {
        this.imports = imports;
    }

    /**
     * Merges the import maps of the libraries on the class path. The files of each library
     * are relative to the path the Dev UI is served at.
     */
    static DevUiImportMap load() {
        return new DevUiImportMap(new TreeMap<>(new Aggregator().aggregate().getImports()));
    }

    /**
     * Where the files of the library with the given import specifier are, relative to the path
     * of the Dev UI, or {@code null} when the library is not on the class path.
     */
    @Nullable
    String path(String specifier) {
        return this.imports.get(specifier);
    }

    /**
     * The import map as JSON, to write into the HTML page of a Dev UI served at the given path.
     * <p>
     * The Vaadin components collect usage statistics and send them to Vaadin when they run on
     * a development machine. The Dev UI turns that off by resolving the module that collects
     * them to the one Vaadin provides for opting out, which does nothing.
     */
    String toJson(String basePath) {
        Map<String, String> resolvedImports = new TreeMap<>();
        this.imports.forEach((specifier, path) -> resolvedImports.put(specifier, basePath + path));
        String usageStatisticsPath = resolvedImports.get(USAGE_STATISTICS);
        if (usageStatisticsPath != null) {
            resolvedImports.put(USAGE_STATISTICS + "vaadin-usage-statistics.js", usageStatisticsPath + "vaadin-usage-statistics-optout.js");
        }
        // A closing tag in the map must not end the script of the page early.
        return jsonMapper.writeValueAsString(Map.of("imports", resolvedImports)).replace("<", "\\u003c");
    }

    /**
     * The path of the stylesheet of the theme, for a Dev UI served at the given path.
     */
    String themeStylesheet(String basePath) {
        String themePath = this.imports.get(THEME);
        Assert.state(themePath != null, "The theme of the Dev UI is not on the class path");
        return basePath + themePath + "dist/aura.css";
    }

}
