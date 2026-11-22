package io.arconia.dev.ui.server.web;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiImportMap}.
 */
class DevUiImportMapTests {

    private static final String BASE_PATH = "/app/a/dev-ui";

    private static final String LIBRARIES_PATH = BASE_PATH + "/_static/";

    private static final String APP_PATH = BASE_PATH + "/static/app/";

    private static final String APP_LOCATION = "io/arconia/dev/ui/server/web/static/app/";

    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/|^[ \\t]*//[^\\n]*$", Pattern.DOTALL | Pattern.MULTILINE);

    private static final Pattern IMPORT = Pattern.compile(
            "(?:^|[;}\\s])(?:import|export)\\s*(?:[\\w*{}\\s,$]+?\\s*from\\s*)?['\"]([^'\"]+)['\"]|import\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)");

    private final DevUiImportMap importMap = DevUiImportMap.load();

    @Test
    void librariesAreResolvedAgainstThePath() {
        assertThat(imports()).isNotEmpty().allSatisfy((specifier, target) -> assertThat(target).startsWith(LIBRARIES_PATH));
    }

    /**
     * Follows the imports of the frontend, starting from its own scripts and going through
     * the ones of the libraries, the way the browser does. An import that the import map
     * doesn't resolve, or that leads to a file that is not there, stops the Dev UI from
     * loading. That is what happens when the new version of a library depends on a library
     * that is not among the dependencies of the Dev UI.
     */
    @Test
    void everyImportOfTheFrontendLeadsToFileOnTheClassPath() throws Exception {
        Map<String, String> imports = imports();
        Deque<String> toVisit = new ArrayDeque<>();
        for (Resource script : new PathMatchingResourcePatternResolver().getResources("classpath:" + APP_LOCATION + "*.js")) {
            toVisit.add(APP_PATH + script.getFilename());
        }
        assertThat(toVisit).isNotEmpty();
        Set<String> visited = new HashSet<>(toVisit);
        List<String> broken = new ArrayList<>();

        while (!toVisit.isEmpty()) {
            String path = toVisit.remove();
            Matcher matcher = IMPORT.matcher(COMMENT.matcher(read(path)).replaceAll(""));
            while (matcher.find()) {
                String specifier = (matcher.group(1) != null) ? matcher.group(1) : matcher.group(2);
                String target = resolve(specifier, path, imports);
                if (target == null || !resource(target).exists()) {
                    broken.add("'%s' imported by %s".formatted(specifier, path));
                } else if (target.endsWith(".js") && visited.add(target)) {
                    toVisit.add(target);
                }
            }
        }

        assertThat(broken).isEmpty();
        assertThat(visited).anyMatch(path -> path.contains("/vaadin/card/")).anyMatch(path -> path.contains("/lit-html/"));
    }

    /**
     * Resolves an import the way the browser does: a relative one against the script that
     * imports it, any other one through the entry of the import map that matches it exactly
     * or the longest entry for a directory that it starts with.
     */
    private static String resolve(String specifier, String importer, Map<String, String> imports) {
        if (specifier.startsWith("./") || specifier.startsWith("../")) {
            return URI.create(importer).resolve(specifier).getPath();
        }
        if (imports.containsKey(specifier)) {
            return imports.get(specifier);
        }
        return imports.keySet().stream()
                .filter(prefix -> prefix.endsWith("/") && specifier.startsWith(prefix))
                .max(Comparator.comparingInt(String::length))
                .map(prefix -> imports.get(prefix) + specifier.substring(prefix.length()))
                .orElse(null);
    }

    private static Resource resource(String path) {
        return path.startsWith(APP_PATH) ? new ClassPathResource(APP_LOCATION + path.substring(APP_PATH.length()))
                : new ClassPathResource("META-INF/resources" + path.substring(BASE_PATH.length()));
    }

    private static String read(String path) throws IOException {
        return resource(path).getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void usageStatisticsAreNotCollected() {
        assertThat(imports().get("@vaadin/vaadin-usage-statistics/vaadin-usage-statistics.js"))
                .startsWith(LIBRARIES_PATH)
                .endsWith("/vaadin-usage-statistics-optout.js");
    }

    @Test
    void libraryIsFoundByItsImportSpecifier() {
        assertThat(this.importMap.path("lit/")).startsWith("/_static/lit/");
        assertThat(this.importMap.path("no-such-library/")).isNull();
    }

    @Test
    void closingTagCannotEndTheScript() {
        assertThat(this.importMap.toJson("</script>")).doesNotContain("<");
    }

    @Test
    void themeStylesheetIsOnTheClassPath() {
        String themeStylesheet = this.importMap.themeStylesheet(BASE_PATH);

        assertThat(themeStylesheet).startsWith(LIBRARIES_PATH).endsWith("/dist/aura.css");
        String resource = "META-INF/resources/_static/" + themeStylesheet.substring(LIBRARIES_PATH.length());
        assertThat(new ClassPathResource(resource).exists()).isTrue();
    }

    private Map<String, String> imports() {
        Map<String, Map<String, String>> json = JsonMapper.builder().build()
                .readValue(this.importMap.toJson(BASE_PATH), new TypeReference<>() {});
        return json.get("imports");
    }

}
