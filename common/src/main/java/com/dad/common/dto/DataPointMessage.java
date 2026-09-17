package com.dad.common.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public record DataPointMessage(
        String id,
        double value,
        Instant timestamp
) implements Serializable {

    public DataPointMessage {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("value must be a finite number");
        }
    }
}
