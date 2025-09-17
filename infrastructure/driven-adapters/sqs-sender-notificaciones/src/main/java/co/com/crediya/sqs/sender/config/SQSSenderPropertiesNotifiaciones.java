package co.com.crediya.sqs.sender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs.notificaciones")
public record SQSSenderPropertiesNotifiaciones(
     String region,
     String queueUrl,
     String endpoint){
}
