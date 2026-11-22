package io.arconia.dev.ui.server.web;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.info.JavaInfo;
import org.springframework.boot.info.OsInfo;
import org.springframework.boot.info.ProcessInfo;
import org.springframework.boot.info.ProcessInfo.MemoryInfo.MemoryUsageInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import io.arconia.core.annotations.Internal;
import io.arconia.dev.ui.api.page.Card;
import io.arconia.dev.ui.api.page.CardItem;
import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.LinkItem;

/**
 * The application the Dev UI is part of: what identifies it, and the overview of it that the
 * Dev UI opens on.
 */
@Internal
public final class DevUiApplication {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter TIME_OF_DAY_FORMAT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private static final long MEGABYTE = 1024L * 1024;

    private final ApplicationContext applicationContext;

    private final AtomicBoolean restarting = new AtomicBoolean();

    public DevUiApplication(ApplicationContext applicationContext) {
        Assert.notNull(applicationContext, "applicationContext cannot be null");
        this.applicationContext = applicationContext;
    }

    /**
     * The name of the application. An application that sets none is known by the name of its
     * main class, so that a developer running several of them can still tell them apart.
     */
    String name() {
        String name = environment().getProperty("spring.application.name");
        if (StringUtils.hasText(name)) {
            return name;
        }
        Class<?> mainClass = mainClass();
        return (mainClass != null) ? mainClass.getSimpleName() : "Application";
    }

    /**
     * The version of the application, when it is known. It usually isn't when the application
     * is run from an IDE, since it comes from the build.
     */
    @Nullable
    String version() {
        String version = environment().getProperty("spring.application.version");
        if (StringUtils.hasText(version)) {
            return version;
        }
        BuildProperties build = this.applicationContext.getBeanProvider(BuildProperties.class).getIfAvailable();
        return (build != null) ? build.getVersion() : null;
    }

    /**
     * When the application started, in milliseconds since the epoch. It tells one run of the
     * application from the next: the value changes when the application is restarted, as
     * Spring Boot DevTools does after a change in the code.
     */
    long startedAt() {
        return this.applicationContext.getStartupDate();
    }

    /**
     * Whether the application can be restarted from the Dev UI, which takes Spring Boot
     * DevTools.
     */
    boolean canRestart() {
        return DevToolsRestart.isAvailable(this.applicationContext.getClassLoader());
    }

    /**
     * Restarts the application, once: asking again while it is restarting changes nothing. The
     * restarted application is another one, which can be restarted in turn.
     */
    void restart() {
        if (this.restarting.compareAndSet(false, true)) {
            DevToolsRestart.restart();
        }
    }

    /**
     * The overview of the application as it is now. What it says about the web server comes
     * from the request that asks for it.
     */
    Overview overview(HttpServletRequest request) {
        JavaInfo java = new JavaInfo();
        List<Fact> facts = List.of(
                new Fact("Spring Boot", SpringBootVersion.getVersion()),
                new Fact("Java", java.getVersion()),
                new Fact("Port", String.valueOf(request.getLocalPort())),
                new Fact("Profiles", profiles()));
        return new Overview(name(), version(), startTime(Instant.ofEpochMilli(startedAt()), LocalDate.now()), facts,
                List.of(applicationCard(), springCard(request), javaCard(java), systemCard()));
    }

    private Card applicationCard() {
        List<CardItem> items = new ArrayList<>();
        items.add(InfoItem.of("Name", name()));
        String version = version();
        if (version != null) {
            items.add(InfoItem.of("Version", version));
        }
        BuildProperties build = this.applicationContext.getBeanProvider(BuildProperties.class).getIfAvailable();
        if (build != null) {
            addIfKnown(items, "Group", build.getGroup());
            addIfKnown(items, "Artifact", build.getArtifact());
            if (build.getTime() != null) {
                items.add(InfoItem.of("Built", TIME_FORMAT.format(build.getTime())));
            }
        }
        GitProperties git = this.applicationContext.getBeanProvider(GitProperties.class).getIfAvailable();
        if (git != null) {
            addIfKnown(items, "Branch", git.getBranch());
            addIfKnown(items, "Commit", git.getShortCommitId());
        }
        Class<?> mainClass = mainClass();
        if (mainClass != null) {
            items.add(InfoItem.of("Main class", mainClass.getName()));
        }
        if (AutoConfigurationPackages.has(this.applicationContext)) {
            items.add(InfoItem.of("Packages", AutoConfigurationPackages.get(this.applicationContext)));
        }
        return Card.builder().id("application").title("Application").icon("app-window").items(items).build();
    }

    private Card springCard(HttpServletRequest request) {
        Environment environment = environment();
        List<CardItem> items = new ArrayList<>();
        items.add(InfoItem.of("Spring Boot", SpringBootVersion.getVersion()));
        addIfKnown(items, "Arconia", DevUiApplication.class.getPackage().getImplementationVersion());
        items.add(InfoItem.of("Mode", "Development"));
        items.add(InfoItem.of("Profiles", profiles()));
        items.add(InfoItem.of("Web server", request.getServletContext().getServerInfo()));
        items.add(InfoItem.of("Port", String.valueOf(request.getLocalPort())));
        items.add(InfoItem.of("Context path", request.getContextPath().isEmpty() ? "/" : request.getContextPath()));
        items.add(InfoItem.of("Virtual threads", environment.getProperty("spring.threads.virtual.enabled", Boolean.class, false) ? "Enabled" : "Disabled"));
        items.add(InfoItem.of("DevTools", DevToolsRestart.isPresent(this.applicationContext.getClassLoader()) ? "Present" : "Not present"));
        items.add(LinkItem.of("Spring Boot documentation", "https://docs.spring.io/spring-boot/index.html"));
        items.add(LinkItem.of("Arconia documentation", "https://docs.arconia.io"));
        return Card.builder().id("spring").title("Spring").icon("leaf").items(items).build();
    }

    private static Card javaCard(JavaInfo java) {
        return Card.builder().id("java").title("Java").icon("coffee").items(List.of(
                InfoItem.of("Version", java.getVersion()),
                InfoItem.of("Vendor", java.getVendor().getName()),
                InfoItem.of("Runtime", java.getRuntime().getName() + " " + java.getRuntime().getVersion()),
                InfoItem.of("Virtual machine", java.getJvm().getName() + " " + java.getJvm().getVersion()))).build();
    }

    private static Card systemCard() {
        OsInfo os = new OsInfo();
        ProcessInfo process = new ProcessInfo();
        MemoryUsageInfo heap = process.getMemory().getHeap();
        String heapMaximum = (heap.getMax() > 0) ? " of " + heap.getMax() / MEGABYTE + " MB" : "";
        return Card.builder().id("system").title("System").icon("laptop").items(List.of(
                InfoItem.of("Operating system", os.getName() + " " + os.getVersion()),
                InfoItem.of("Architecture", os.getArch()),
                InfoItem.of("Processors", String.valueOf(process.getCpus())),
                InfoItem.of("Process ID", String.valueOf(process.getPid())),
                InfoItem.of("Heap memory", heap.getUsed() / MEGABYTE + " MB used" + heapMaximum))).build();
    }

    private static void addIfKnown(List<CardItem> items, String label, @Nullable String value) {
        if (StringUtils.hasText(value)) {
            items.add(InfoItem.of(label, value));
        }
    }

    private Environment environment() {
        return this.applicationContext.getEnvironment();
    }

    private String profiles() {
        String[] profiles = environment().getActiveProfiles();
        return (profiles.length > 0) ? String.join(", ", profiles) : "None";
    }

    /**
     * The class the application is defined by, which is the one annotated as a Spring Boot
     * application.
     */
    private @Nullable Class<?> mainClass() {
        for (String beanName : this.applicationContext.getBeanNamesForAnnotation(SpringBootConfiguration.class)) {
            Class<?> type = this.applicationContext.getType(beanName);
            if (type != null) {
                return ClassUtils.getUserClass(type);
            }
        }
        return null;
    }

    /**
     * When the application started, as a time of day, with the date when that was not today.
     * A point in time stays true for as long as the page is open, which a duration would not.
     */
    static String startTime(Instant start, LocalDate today) {
        String time = TIME_OF_DAY_FORMAT.format(start);
        if (LocalDate.ofInstant(start, ZoneId.systemDefault()).equals(today)) {
            return time;
        }
        return DAY_FORMAT.format(start) + ", " + time;
    }

    /**
     * The overview of the application: what identifies it, when it started, the facts worth
     * seeing at a glance, and the details as cards.
     */
    record Overview(String name, @Nullable String version, String startedAt, List<Fact> facts, List<Card> cards) {}

    record Fact(String label, String value) {}

}
