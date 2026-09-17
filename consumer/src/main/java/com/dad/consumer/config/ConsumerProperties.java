package com.dad.consumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.consumer")
public record ConsumerProperties(
        String queueName,
        int windowSize,
        double thresholdStdDev,
        int minSampleSize
) {}
