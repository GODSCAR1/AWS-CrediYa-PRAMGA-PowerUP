package co.com.crediya.sqs.sender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs.reportes")
public record SQSSenderPropertiesReportes(
     String region,
     String queueUrl,
     String endpoint){
}
