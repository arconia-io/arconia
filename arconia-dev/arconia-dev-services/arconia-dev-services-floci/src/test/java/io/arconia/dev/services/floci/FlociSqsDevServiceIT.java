package io.arconia.dev.services.floci;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Floci Dev Service with Spring Cloud AWS SQS.
 */
@EnabledIfDockerAvailable
class FlociSqsDevServiceIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(
                    FlociDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class,
                    CredentialsProviderAutoConfiguration.class,
                    RegionProviderAutoConfiguration.class,
                    AwsAutoConfiguration.class,
                    SqsAutoConfiguration.class
            ));

    @Test
    void shouldSendAndReceiveMessage() {
        contextRunner.run(context -> {
            var sqsClient = context.getBean(SqsAsyncClient.class);

            String queueUrl = sqsClient.createQueue(b -> b.queueName("dust-readings")).get().queueUrl();
            sqsClient.sendMessage(b -> b.queueUrl(queueUrl).messageBody("""
                    {"event":"dust.surge","location":"svalbard","observer":"lord_asriel"}
                    """));

            var messages = sqsClient.receiveMessage(b -> b.queueUrl(queueUrl).maxNumberOfMessages(1)).get().messages();
            assertThat(messages).hasSize(1);
            assertThat(messages.getFirst().body()).contains("lord_asriel");
        });
    }

}
