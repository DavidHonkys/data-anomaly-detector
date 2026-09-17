package com.dad.producer.service;

import com.dad.common.dto.DataPointMessage;
import com.dad.producer.config.ProducerProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

@Service
public class DataGeneratorService {

    private final ProducerProperties props;
    private final RandomGenerator random;

    public DataGeneratorService(ProducerProperties props) {
        this.props = props;
        RandomGenerator rng;
        try {
            rng = RandomGenerator.getDefault();
        } catch (Exception e) {
            // Failsafe for stripped minimal container images
            rng = ThreadLocalRandom.current();
        }
        this.random = rng;
    }

    public GeneratedPoint generateNext() {
        double mean = props.distribution().mean();
        double stdDev = props.distribution().stdDev();
        boolean isAnomaly = random.nextDouble() < props.anomaly().rate();

        double value;
        if (isAnomaly) {
            double sign = random.nextBoolean() ? 1.0 : -1.0;
            double offset = sign * props.anomaly().deviationFactor() * stdDev;
            value = mean + offset + (random.nextGaussian() * stdDev);
        } else {
            value = random.nextGaussian(mean, stdDev);
        }

        DataPointMessage message = new DataPointMessage(
                UUID.randomUUID().toString(),
                value,
                Instant.now()
        );

        return new GeneratedPoint(message, isAnomaly);
    }

    public record GeneratedPoint(DataPointMessage message, boolean intentionalAnomaly) { }
}
