package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.Map;

import com.github.dockerjava.api.model.Container;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.DevServiceLabels;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DockerContainerDiscovery}.
 */
class DockerContainerDiscoveryTests {

    @Test
    void whenNoContainersThenNoCandidates() {
        assertThat(DockerContainerDiscovery.select(List.of(), "localhost", "postgres")).isEmpty();
    }

    @Test
    void whenContainerStartedByThisApplicationThenItIsLeftOut() {
        Container own = container("own", 100L, DevServiceLabels.ownerId());
        Container other = container("other", 200L, "another-application");

        assertThat(candidateIds(List.of(own, other))).containsExactly("other");
    }

    @Test
    void whenContainerHasNoOwnerThenItIsACandidate() {
        Container container = container("unowned", 100L, null);

        assertThat(candidateIds(List.of(container))).containsExactly("unowned");
    }

    @Test
    void whenSeveralContainersThenOldestComesFirst() {
        Container newer = container("newer", 300L, "another-application");
        Container oldest = container("oldest", 100L, "yet-another-application");
        Container older = container("older", 200L, "another-application");

        assertThat(candidateIds(List.of(newer, oldest, older))).containsExactly("oldest", "older", "newer");
    }

    @Test
    void whenContainersCreatedAtTheSameTimeThenOrderedById() {
        Container second = container("bbb", 100L, "another-application");
        Container first = container("aaa", 100L, "yet-another-application");

        assertThat(candidateIds(List.of(second, first))).containsExactly("aaa", "bbb");
    }

    @Test
    void whenCreationTimeIsUnknownThenContainerComesLast() {
        Container unknown = container("unknown", null, "another-application");
        Container known = container("known", 100L, "yet-another-application");

        assertThat(candidateIds(List.of(unknown, known))).containsExactly("known", "unknown");
    }

    @Test
    void whenContainerIsReportedIncompletelyThenItIsSkipped() {
        Container incomplete = container("incomplete", 100L, "another-application");
        when(incomplete.getStatus()).thenReturn(null);
        Container complete = container("complete", 200L, "another-application");

        assertThat(candidateIds(List.of(incomplete, complete))).containsExactly("complete");
    }

    @Test
    void candidatesCarryTheHostTheyAreReachableAt() {
        Container container = container("abc", 100L, "another-application");

        assertThat(DockerContainerDiscovery.select(List.of(container), "docker.host", "postgres"))
                .singleElement()
                .satisfies(candidate -> assertThat(candidate.host()).isEqualTo("docker.host"));
    }

    private static List<String> candidateIds(List<Container> containers) {
        return DockerContainerDiscovery.select(containers, "localhost", "postgres").stream()
                .map(candidate -> candidate.containerInfo().id())
                .toList();
    }

    private static Container container(String id, Long created, String ownerId) {
        Container container = mock(Container.class);
        when(container.getId()).thenReturn(id);
        when(container.getCreated()).thenReturn(created);
        when(container.getImage()).thenReturn("postgres:latest");
        when(container.getStatus()).thenReturn("Up 2 seconds");
        when(container.getLabels()).thenReturn(ownerId != null
                ? Map.of(DevServiceLabels.NAME, "postgres", DevServiceLabels.DISCOVERABLE, "true", DevServiceLabels.OWNER, ownerId)
                : Map.of(DevServiceLabels.NAME, "postgres", DevServiceLabels.DISCOVERABLE, "true"));
        return container;
    }

}
