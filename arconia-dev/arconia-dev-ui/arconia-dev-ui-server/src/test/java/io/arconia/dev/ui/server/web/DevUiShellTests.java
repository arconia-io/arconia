package io.arconia.dev.ui.server.web;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiShell}.
 */
class DevUiShellTests {

    private static final DevUiImportMap IMPORT_MAP = DevUiImportMap.load();

    @Test
    void pageLoadsTheDevUiFromItsPath() {
        String shell = new String(DevUiShell.render("/app/a/dev-ui", IMPORT_MAP, new DevUiIcons(IMPORT_MAP), application("catalog-service", "1.4.0")), StandardCharsets.UTF_8);

        assertThat(shell)
                .containsPattern("<arconia-dev-ui base-path=\"/app/a/dev-ui\" icons-path=\"/app/a/dev-ui/_static/lucide-static/[^\"/]+/icons/\" application-name=\"catalog-service\" application-version=\"1.4.0\" application-started-at=\"\\d+\" application-logs=\"(true|false)\" application-restart=\"false\"></arconia-dev-ui>")
                .contains("<title>catalog-service · Arconia Dev UI</title>")
                .contains("<link rel=\"icon\" type=\"image/png\" href=\"/app/a/dev-ui/static/app/images/favicon.png\">")
                .contains("<script type=\"module\" src=\"/app/a/dev-ui/static/app/app.js\"></script>")
                .contains("<link rel=\"stylesheet\" href=\"/app/a/dev-ui/static/app/app.css\">")
                .containsPattern("<link rel=\"stylesheet\" href=\"/app/a/dev-ui/_static/at/vaadin/aura/[^\"]+/dist/aura.css\">")
                .contains("<script type=\"importmap\">{\"imports\":{")
                .contains("localStorage.getItem('arconia-dev-ui-color-scheme')")
                .doesNotContain("{{");
    }

    @Test
    void nameOfTheApplicationIsEscapedAndItsVersionIsEmptyWhenNotKnown() {
        String shell = new String(DevUiShell.render("/a/dev-ui", IMPORT_MAP, new DevUiIcons(IMPORT_MAP), application("<b>shop</b>", "")),
                StandardCharsets.UTF_8);

        assertThat(shell)
                .contains("<title>&lt;b&gt;shop&lt;/b&gt; · Arconia Dev UI</title>")
                .contains("application-name=\"&lt;b&gt;shop&lt;/b&gt;\" application-version=\"\"");
    }

    private static DevUiApplication application(String name, String version) {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        TestPropertyValues.of("spring.application.name=" + name, "spring.application.version=" + version).applyTo(applicationContext);
        applicationContext.refresh();
        return new DevUiApplication(applicationContext);
    }

    @Test
    void pathIsEscaped() {
        String shell = new String(DevUiShell.render("/a\"b/a/dev-ui", IMPORT_MAP, new DevUiIcons(IMPORT_MAP), application("catalog-service", "1.4.0")), StandardCharsets.UTF_8);

        assertThat(shell).contains("src=\"/a&quot;b/a/dev-ui/static/app/app.js\"");
    }

}
