package io.arconia.dev.services.core.devui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import io.arconia.core.annotations.Internal;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.ContainerInfo.ContainerPort;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.api.registration.DevServiceRegistration.Origin;
import io.arconia.dev.ui.api.page.Card;
import io.arconia.dev.ui.api.page.CardItem;
import io.arconia.dev.ui.api.page.CardsPage;
import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.LinkItem;
import io.arconia.dev.ui.api.page.LogItem;
import io.arconia.dev.ui.api.page.Status;
import io.arconia.dev.ui.api.page.StatusLevel;

/**
 * The page of the Dev UI that shows the dev services of the application, with one card for
 * each of them.
 */
@Internal
public final class DevServicesDevUiPage {

    private static final Logger logger = LoggerFactory.getLogger(DevServicesDevUiPage.class);

    private DevServicesDevUiPage() {}

    /**
     * Creates the page for the given dev services. When several of them have the same name,
     * the first one is shown, as the Actuator endpoint does.
     */
    public static CardsPage create(Collection<DevServiceRegistration> registrations) {
        Map<String, DevServiceRegistration> registrationsByName = new LinkedHashMap<>();
        registrations.forEach(registration -> registrationsByName.putIfAbsent(registration.getName(), registration));
        return CardsPage.builder()
                .id("dev-services")
                .title("Dev Services")
                .icon("container")
                .maxColumns(2)
                .cards(() -> registrationsByName.values().stream().map(DevServicesDevUiPage::card).toList())
                .build();
    }

    private static Card card(DevServiceRegistration registration) {
        List<CardItem> items = new ArrayList<>();
        items.add(InfoItem.builder()
                .label("Name")
                .value(registration.getName())
                .icon("tag")
                .build());

        ContainerInfo containerInfo = containerInfo(registration);
        if (containerInfo != null) {
            items.add(InfoItem.builder()
                    .label("Image")
                    .value(containerInfo.getImageName())
                    .icon("package")
                    .code(true)
                    .build());
            List<String> ports = publishedPorts(containerInfo);
            if (!ports.isEmpty()) {
                items.add(InfoItem.builder()
                        .label("Ports")
                        .values(ports)
                        .icon("network")
                        .code(true)
                        .build());
            }
        }

        items.add(InfoItem.builder()
                .label("Container")
                .value((registration.getOrigin() == Origin.MANAGED) ? "Managed by this application" : "Started by another application")
                .icon("box")
                .build());

        for (DevServiceLink link : registration.getLinks()) {
            items.add(LinkItem.of(link.label(), link.url()));
        }

        if (containerInfo != null) {
            String containerId = containerInfo.getId();
            items.add(LogItem.of("Logs", lines -> ContainerLogs.follow(containerId, lines)));
        }

        return Card.builder()
                .id(registration.getName())
                .title(registration.getTitle())
                .description(registration.getDescription())
                .icon(registration.getIcon())
                .documentation(registration.getDocumentation())
                .propertiesPrefix(registration.getPropertiesPrefix())
                .status(status(containerInfo))
                .items(items)
                .build();
    }

    /**
     * The state of the container as the container runtime reports it. A running container is
     * what a dev service is expected to be, a container that has stopped is not, and any other
     * state is one that the container is passing through or was put in on purpose.
     */
    private static Status status(@Nullable ContainerInfo containerInfo) {
        if (containerInfo == null) {
            return new Status("Not available", StatusLevel.NEUTRAL);
        }
        String state = containerInfo.getState();
        StatusLevel level = switch (state.toLowerCase(Locale.ROOT)) {
            case "running" -> StatusLevel.SUCCESS;
            case "exited", "dead" -> StatusLevel.ERROR;
            default -> StatusLevel.WARNING;
        };
        return new Status(StringUtils.capitalize(state), level);
    }

    /**
     * The information about the container is read when the page is shown. It is not available
     * when the container is gone, for example after it has been removed manually.
     */
    private static @Nullable ContainerInfo containerInfo(DevServiceRegistration registration) {
        try {
            return registration.getContainerInfo().get();
        } catch (RuntimeException ex) {
            logger.debug("Dev Service '{}': failed to resolve container information", registration.getName(), ex);
            return null;
        }
    }

    /**
     * The ports of the container that are published on the host, each one followed by the
     * host port. A port published on several host addresses is listed once.
     */
    private static List<String> publishedPorts(ContainerInfo containerInfo) {
        return containerInfo.getPorts().stream()
                .filter(port -> port.privatePort() != null && port.publicPort() != null)
                .sorted(Comparator.comparingInt(containerPort -> Objects.requireNonNull(containerPort.privatePort())))
                .map(DevServicesDevUiPage::portMapping)
                .distinct()
                .toList();
    }

    private static String portMapping(ContainerPort port) {
        return port.privatePort() + " → " + port.publicPort();
    }

}
