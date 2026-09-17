package com.dad.consumer.service;

import com.dad.common.dto.DataPointMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MetricConsumerListener {

    private static final Logger log = LoggerFactory.getLogger(MetricConsumerListener.class);

    private final AnomalyDetector anomalyDetector;

    public MetricConsumerListener(AnomalyDetector anomalyDetector) {
        this.anomalyDetector = anomalyDetector;
    }

    @RabbitListener(queues = "${app.consumer.queue-name}")
    public void onDataPoint(DataPointMessage message) {
        var result = anomalyDetector.evaluate(message);

        if (result.isAnomaly()) {
            log.warn("[{}] Data point: {} | Status: ANOMALY DETECTED! | Z-score: {} | ALERT: Significant deviation detected.",
                    message.timestamp(), message.value(), result.zScore());
            log.warn("[Rolling window]  Current mean: {} | Standard deviation: {} | Size: {} .",
                    result.currentMean(), result.currentStdDev(), result.sampleSize());
        } else {
            log.info("[{}] Data point: {} | Status: OK | Z-score: {}",
                    message.timestamp(), message.value(), result.zScore());
        }
    }
}