package io.arconia.dev.services.core.devui;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.ContainerInfo.ContainerPort;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.api.registration.DevServiceRegistration.Origin;
import io.arconia.dev.ui.api.page.Card;
import io.arconia.dev.ui.api.page.Status;
import io.arconia.dev.ui.api.page.StatusLevel;
import io.arconia.dev.ui.api.page.CardsPage;
import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.LinkItem;
import io.arconia.dev.ui.api.page.LogItem;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesDevUiPage}.
 */
class DevServicesDevUiPageTests {

    @Test
    void pageIsIdentifiedAsDevServices() {
        CardsPage page = DevServicesDevUiPage.create(List.of());

        assertThat(page.getId()).isEqualTo("dev-services");
        assertThat(page.getTitle()).isEqualTo("Dev Services");
        assertThat(page.getIcon()).isEqualTo("container");
        assertThat(page.getMaxColumns()).isEqualTo(2);
        assertThat(page.getCards().get()).isEmpty();
    }

    @Test
    void cardDescribesTheDevServiceAndItsContainer() {
        DevServiceRegistration registration = registration("lgtm", Origin.MANAGED)
                .title("Grafana LGTM")
                .description("Observability platform based on OpenTelemetry.")
                .documentation("https://docs.arconia.io/arconia/latest/dev-services/lgtm/")
                .propertiesPrefix("arconia.dev.services.lgtm")
                .icon("chart-spline")
                .containerInfo(() -> containerInfo("grafana/otel-lgtm", "Up 5 minutes", List.of(
                        new ContainerPort("0.0.0.0", 4318, 54318, "tcp"),
                        new ContainerPort("::", 4318, 54318, "tcp"),
                        new ContainerPort("0.0.0.0", 3000, 53000, "tcp"),
                        new ContainerPort(null, 9090, null, "tcp"))))
                .links(List.of(new DevServiceLink("Grafana", "http://localhost:53000")))
                .build();

        Card card = DevServicesDevUiPage.create(List.of(registration)).getCards().get().getFirst();

        assertThat(card.getId()).isEqualTo("lgtm");
        assertThat(card.getTitle()).isEqualTo("Grafana LGTM");
        assertThat(card.getDescription()).isEqualTo("Observability platform based on OpenTelemetry.");
        assertThat(card.getDocumentation()).isEqualTo("https://docs.arconia.io/arconia/latest/dev-services/lgtm/");
        assertThat(card.getPropertiesPrefix()).isEqualTo("arconia.dev.services.lgtm");
        assertThat(card.getIcon()).isEqualTo("chart-spline");
        assertThat(card.getStatus()).isEqualTo(new Status("Running", StatusLevel.SUCCESS));
        assertThat(card.getItems()).startsWith(
                InfoItem.builder().label("Name").value("lgtm").icon("tag").build(),
                InfoItem.builder().label("Image").value("grafana/otel-lgtm").icon("package").code(true).build(),
                InfoItem.builder().label("Ports").values(List.of("3000 → 53000", "4318 → 54318")).icon("network").code(true).build(),
                InfoItem.builder().label("Container").value("Managed by this application").icon("box").build(),
                LinkItem.of("Grafana", "http://localhost:53000"));
        assertThat(card.getItems().getLast()).isInstanceOfSatisfying(LogItem.class,
                logs -> assertThat(logs.getLabel()).isEqualTo("Logs"));
    }

    @ParameterizedTest
    @CsvSource({ "running, Running, SUCCESS", "exited, Exited, ERROR", "dead, Dead, ERROR", "created, Created, WARNING",
            "paused, Paused, WARNING", "restarting, Restarting, WARNING", "removing, Removing, WARNING" })
    void statusOfTheCardIsTheStateOfTheContainer(String state, String text, StatusLevel level) {
        DevServiceRegistration registration = registration("redis", Origin.MANAGED)
                .containerInfo(() -> ContainerInfo.builder().id("1234").imageName("redis").names(List.of("/redis"))
                        .ports(List.of()).labels(Map.of()).state(state).status("Up 5 minutes").build())
                .build();

        Card card = DevServicesDevUiPage.create(List.of(registration)).getCards().get().getFirst();

        assertThat(card.getStatus()).isEqualTo(new Status(text, level));
    }

    @Test
    void cardHasNoPortsWhenTheContainerPublishesNone() {
        DevServiceRegistration registration = registration("redis", Origin.MANAGED)
                .containerInfo(() -> containerInfo("redis", "Up 5 minutes", List.of(new ContainerPort(null, 6379, null, "tcp"))))
                .build();

        Card card = DevServicesDevUiPage.create(List.of(registration)).getCards().get().getFirst();

        assertThat(card.getItems()).extracting("label").containsExactly("Name", "Image", "Container", "Logs");
    }

    @Test
    void cardTellsWhenTheContainerWasStartedByAnotherApplication() {
        DevServiceRegistration registration = registration("redis", Origin.DISCOVERED).build();

        Card card = DevServicesDevUiPage.create(List.of(registration)).getCards().get().getFirst();

        assertThat(card.getItems()).contains(InfoItem.builder().label("Container").value("Started by another application").icon("box").build());
    }

    @Test
    void cardIsShownWhenTheContainerInformationIsNotAvailable() {
        DevServiceRegistration registration = registration("redis", Origin.MANAGED)
                .containerInfo(() -> {
                    throw new IllegalStateException("container not found");
                })
                .links(List.of(new DevServiceLink("Console", "http://localhost:8001")))
                .build();

        Card card = DevServicesDevUiPage.create(List.of(registration)).getCards().get().getFirst();

        assertThat(card.getStatus()).isEqualTo(new Status("Not available", StatusLevel.NEUTRAL));
        assertThat(card.getItems()).containsExactly(
                InfoItem.builder().label("Name").value("redis").icon("tag").build(),
                InfoItem.builder().label("Container").value("Managed by this application").icon("box").build(),
                LinkItem.of("Console", "http://localhost:8001"));
    }

    @Test
    void containerInformationIsReadEachTimeThePageIsShown() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<ContainerInfo> containerInfo = () -> {
            calls.incrementAndGet();
            return containerInfo("redis", "Up 5 minutes", List.of());
        };
        CardsPage page = DevServicesDevUiPage.create(List.of(registration("redis", Origin.MANAGED).containerInfo(containerInfo).build()));

        assertThat(calls).hasValue(0);
        page.getCards().get();
        page.getCards().get();
        assertThat(calls).hasValue(2);
    }

    @Test
    void whenSeveralDevServicesHaveTheSameNameThenTheFirstIsShown() {
        DevServiceRegistration first = registration("redis", Origin.MANAGED).title("First").build();
        DevServiceRegistration second = registration("redis", Origin.MANAGED).title("Second").build();

        List<Card> cards = DevServicesDevUiPage.create(List.of(first, second)).getCards().get();

        assertThat(cards).extracting(Card::getTitle).containsExactly("First");
    }

    private static DevServiceRegistration.Builder registration(String name, Origin origin) {
        return DevServiceRegistration.builder()
                .name(name)
                .title(name + " dev service")
                .origin(origin)
                .containerInfo(() -> containerInfo(name, "Up 5 minutes", List.of()));
    }

    private static ContainerInfo containerInfo(String imageName, String status, List<ContainerPort> ports) {
        return ContainerInfo.builder()
                .id("1234")
                .imageName(imageName)
                .names(List.of("/" + imageName))
                .ports(ports)
                .labels(Map.of())
                .state("running")
                .status(status)
                .build();
    }

}
