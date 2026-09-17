package com.dad.consumer.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dad.common.dto.DataPointMessage;
import com.dad.consumer.config.ConsumerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

class StatisticalAnomalyDetectorTest {

    private StatisticalAnomalyDetector detector;

    @BeforeEach
    void setUp() {
        // Window: 20, Threshold: 3.0, Min sample: 5
        ConsumerProperties props = new ConsumerProperties("test-queue", 20, 3.0, 5);
        detector = new StatisticalAnomalyDetector(props);
    }

    @Test
    void shouldWarmUpWithoutTriggeringAnomalies() {
        for (int i = 0; i < 4; i++) {
            var res = detector.evaluate(point(1000.0));
            assertFalse(res.isAnomaly());
        }
    }

    @Test
    void shouldDetectOutlierBeyondThreshold() {
        // Feed window with data: mean = 50.0 (+/- 1.0)
        for (int i = 0; i < 15; i++) {
            detector.evaluate(point(50.0 + (i % 2 == 0 ? 1.0 : -1.0)));
        }

        // Send anomaly
        var anomalyResult = detector.evaluate(point(95.0));

        assertTrue(anomalyResult.isAnomaly());
        assertTrue(Math.abs(anomalyResult.zScore()) >= 3.0);
    }

    private DataPointMessage point(double value) {
        return new DataPointMessage(UUID.randomUUID().toString(), value, Instant.now());
    }
}
