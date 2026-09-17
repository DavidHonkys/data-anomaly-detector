package com.dad.consumer.service;

import com.dad.common.dto.DataPointMessage;

public interface AnomalyDetector {
    DetectionResult evaluate(DataPointMessage message);

    record DetectionResult(
            DataPointMessage message,
            boolean isAnomaly,
            double zScore,
            double currentMean,
            double currentStdDev,
            int sampleSize
    ) {}
}
