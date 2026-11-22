package io.arconia.dev.ui.server.frontend;

import java.nio.file.Path;
import java.util.Map;

import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

/**
 * The browser of the browser tests, for {@code @UsePlaywright}: Chromium in a Playwright server
 * container, started once and shared by every test class. The browser reaches the application
 * on {@code localhost} through the test itself, so the Dev UI sees the requests as coming from
 * the local machine, as it does when a developer opens it. A test that fails leaves a trace of
 * what the browser did under {@code build/playwright}.
 */
public final class DevUiBrowser implements OptionsFactory {

    private static final PlaywrightServerContainer server = startServer();

    @Override
    public Options getOptions() {
        return new Options()
                // The browsers are in the container: the library would download its own otherwise.
                .setPlaywrightCreateOptions(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")))
                .setWsEndpoint(server.wsEndpoint())
                .setConnectOptions(new BrowserType.ConnectOptions().setExposeNetwork("<loopback>"))
                .setTrace(Options.Trace.RETAIN_ON_FAILURE)
                .setOutputDir(Path.of("build", "playwright"));
    }

    private static PlaywrightServerContainer startServer() {
        PlaywrightServerContainer container = new PlaywrightServerContainer();
        container.start();
        return container;
    }

}
