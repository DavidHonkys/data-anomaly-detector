package com.dad.consumer.service;

import com.dad.consumer.config.ConsumerProperties;
import com.dad.common.dto.DataPointMessage;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class StatisticalAnomalyDetector implements AnomalyDetector {

    private final RollingWindowStats window;
    private final ConsumerProperties props;

    public StatisticalAnomalyDetector(ConsumerProperties props) {
        this.props = props;
        this.window = new RollingWindowStats(props.windowSize());
    }

    @Override
    public DetectionResult evaluate(DataPointMessage message) {
        double val = message.value();
        var snapshot = window.snapshot();

        // Warm-up evaluation
        if (snapshot.size() < props.minSampleSize() || snapshot.stdDev() == 0.0) {
            window.add(val);
            return new DetectionResult(message, false, snapshot.mean(), snapshot.stdDev(), 0.0, snapshot.size() + 1);
        }

        // Statistical evaluation
        double zScore = (val - snapshot.mean()) / snapshot.stdDev();
        boolean isAnomaly = Math.abs(zScore) >= props.thresholdStdDev();

        // Only normal observations update the rolling window
        if (!isAnomaly) {
            window.add(val);
        }

        return new DetectionResult(
                message,
                isAnomaly,
                zScore,
                snapshot.mean(),
                snapshot.stdDev(),
                snapshot.size()
        );
    }
}

