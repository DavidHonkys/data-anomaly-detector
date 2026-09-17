package com.dad.producer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.producer")
public record ProducerProperties(
        String exchangeName,
        String routingKey,
        String queueName,
        long publishIntervalMs,
        Distribution distribution,
        Anomaly anomaly
) {
    public record Distribution(double mean, double stdDev) {}
    public record Anomaly(double rate, double deviationFactor) {}
}
