package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import java.util.Properties;

import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.info.GitProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link GitResourceContributor}.
 */
class GitResourceContributorTests {

    private final ResourceBuilder resourceBuilder = mock(ResourceBuilder.class);

    @Test
    void shouldContributeAllAttributesWhenAvailable() {
        GitResourceContributor contributor = new GitResourceContributor(gitProperties(
                "main", "9d59409acf479dfa0df1aa568182e43e43df8bbe", "https://github.com/arconia-io/arconia.git"));

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(GitResourceContributor.VCS_REF_HEAD_REVISION, "9d59409acf479dfa0df1aa568182e43e43df8bbe");
        verify(resourceBuilder).put(GitResourceContributor.VCS_REF_HEAD_NAME, "main");
        verify(resourceBuilder).put(GitResourceContributor.VCS_REF_HEAD_TYPE, "branch");
        verify(resourceBuilder).put(GitResourceContributor.VCS_REPOSITORY_URL_FULL, "https://github.com/arconia-io/arconia");
        verify(resourceBuilder).put(GitResourceContributor.VCS_REPOSITORY_NAME, "arconia");
    }

    @Test
    void shouldContributeNothingWhenNoGitInformation() {
        GitResourceContributor contributor = new GitResourceContributor(new GitProperties(new Properties()));

        contributor.contribute(resourceBuilder);

        verifyNoInteractions(resourceBuilder);
    }

    @ParameterizedTest
    @CsvSource({
            "https://github.com/arconia-io/arconia.git, https://github.com/arconia-io/arconia",
            "https://github.com/arconia-io/arconia, https://github.com/arconia-io/arconia",
            "https://github.com/arconia-io/arconia/, https://github.com/arconia-io/arconia",
            "https://user:token@github.com/arconia-io/arconia.git, https://github.com/arconia-io/arconia",
            "http://git.example.com:8080/group/sub/repo.git, http://git.example.com:8080/group/sub/repo",
            "git@github.com:arconia-io/arconia.git, https://github.com/arconia-io/arconia",
            "github.com:arconia-io/arconia, https://github.com/arconia-io/arconia",
            "ssh://git@gitlab.com:2222/group/repo.git, https://gitlab.com/group/repo"
    })
    void shouldConvertRemoteToRepositoryUrl(String remoteUrl, String repositoryUrl) {
        assertThat(GitResourceContributor.toRepositoryUrl(remoteUrl)).isEqualTo(repositoryUrl);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/home/user/repo.git", "file:///home/user/repo.git", "C:/Users/me/repo.git", "https://github.com/", "not a url" })
    void shouldSkipRemotesThatAreNotRepositoryUrls(String remoteUrl) {
        assertThat(GitResourceContributor.toRepositoryUrl(remoteUrl)).isNull();
    }

    private static GitProperties gitProperties(String branch, String commitId, String remoteUrl) {
        Properties properties = new Properties();
        properties.setProperty("branch", branch);
        properties.setProperty("commit.id", commitId);
        properties.setProperty("remote.origin.url", remoteUrl);
        return new GitProperties(properties);
    }

}
