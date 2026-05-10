package io.arconia.dev.services.floci;

import java.util.Map;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.dynamodb.DynamoDbAutoConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Floci Dev Service with Spring Cloud AWS DynamoDB.
 */
@EnabledIfDockerAvailable
class FlociDynamoDbDevServiceIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(
                    FlociDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class,
                    CredentialsProviderAutoConfiguration.class,
                    RegionProviderAutoConfiguration.class,
                    AwsAutoConfiguration.class,
                    DynamoDbAutoConfiguration.class
            ));

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateTableAndPutItem() {
        contextRunner.run(context -> {
            var dynamoClient = context.getBean(DynamoDbClient.class);

            dynamoClient.createTable(b -> b
                    .tableName("Daemons")
                    .attributeDefinitions(a -> a.attributeName("id").attributeType(ScalarAttributeType.S))
                    .keySchema(k -> k.attributeName("id").keyType(KeyType.HASH))
                    .billingMode(BillingMode.PAY_PER_REQUEST));

            dynamoClient.putItem(b -> b
                    .tableName("Daemons")
                    .item(Map.of(
                            "id", AttributeValue.fromS("pantalaimon"),
                            "form", AttributeValue.fromS("pine_marten"),
                            "person", AttributeValue.fromS("lyra_belacqua"))));

            var item = dynamoClient.getItem(b -> b
                    .tableName("Daemons")
                    .key(Map.of("id", AttributeValue.fromS("pantalaimon")))).item();

            assertThat(item.get("form").s()).isEqualTo("pine_marten");
            assertThat(item.get("person").s()).isEqualTo("lyra_belacqua");
        });
    }

}
