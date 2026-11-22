package io.arconia.dev.ui.server;

import java.time.Duration;
import java.util.UUID;

import com.microsoft.playwright.Playwright;

import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A Playwright server in a container, for the browser tests to drive a browser without one
 * installed on the machine that runs them. The image brings the browsers and what they need
 * from the system, but not Playwright itself, which the container fetches when it starts.
 * Client and server must run the same version of Playwright, and the browsers in the image
 * are the ones that version expects, so the version of the Playwright library on the class
 * path picks both the image and the server.
 */
final class PlaywrightServerContainer extends GenericContainer<PlaywrightServerContainer> {

    static final String VERSION = playwrightVersion();

    private static final int PORT = 3000;

    private static final long SHARED_MEMORY_SIZE = 1024L * 1024 * 1024;

    // The server has no authentication, so it is served at a path hard to guess.
    private final String path = "/" + UUID.randomUUID();

    PlaywrightServerContainer() {
        super(DockerImageName.parse("mcr.microsoft.com/playwright").withTag("v" + VERSION + "-noble"));
        this.withExposedPorts(PORT);
        this.withCommand("/bin/sh", "-c",
                "npx -y playwright@%s run-server --port %d --host 0.0.0.0 --path %s".formatted(VERSION, PORT, this.path));
        this.withCreateContainerCmdModifier(command -> {
            command.withUser("pwuser").withWorkingDir("/home/pwuser");
            command.getHostConfig().withInit(true);
        });
        // Chromium keeps a lot in shared memory and crashes when the default amount runs out.
        this.withSharedMemorySize(SHARED_MEMORY_SIZE);
        this.waitingFor(Wait.forLogMessage(".*Listening on .*\\n", 1).withStartupTimeout(Duration.ofMinutes(2)));
    }

    /**
     * The address the Playwright library connects to.
     */
    String wsEndpoint() {
        return "ws://%s:%d%s".formatted(getHost(), getMappedPort(PORT), this.path);
    }

    private static String playwrightVersion() {
        String version = Playwright.class.getPackage().getImplementationVersion();
        Assert.state(version != null, "The version of the Playwright library cannot be found in its manifest");
        return version;
    }

}
