package co.com.crediya.sqs.sender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs.capacidad-endeudamiento")
public record SQSSenderPropertiesCapacidadEndeudamiento(
     String region,
     String queueUrl,
     String endpoint){
}
