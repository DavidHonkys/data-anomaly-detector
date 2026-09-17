package com.dad.consumer.service;

import java.util.concurrent.locks.ReentrantLock;

public class RollingWindowStats {

    private final int capacity;
    private final double[] buffer;
    private final ReentrantLock lock = new ReentrantLock();

    private int head = 0;
    private int size = 0;

    public RollingWindowStats(int capacity) {
        if (capacity <= 1) {
            throw new IllegalArgumentException("Window capacity must be greater than 1");
        }
        this.capacity = capacity;
        this.buffer = new double[capacity];
    }

    public void add(double value) {
        lock.lock();
        try {
            buffer[head] = value;
            head = (head + 1) % capacity;
            if (size < capacity) {
                size++;
            }
        } finally {
            lock.unlock();
        }
    }

    public Snapshot snapshot() {
        lock.lock();
        try {
            if (size == 0) {
                return new Snapshot(0, 0.0, 0.0);
            }

            double sum = 0.0;
            for (int i = 0; i < size; i++) {
                sum += buffer[i];
            }
            double mean = sum / size;

            if (size < 2) {
                return new Snapshot(size, mean, 0.0);
            }

            double sumSquaredDiffs = 0.0;
            for (int i = 0; i < size; i++) {
                double diff = buffer[i] - mean;
                sumSquaredDiffs += diff * diff;
            }

            // Sample standard deviation (Bessel's correction)
            double variance = sumSquaredDiffs / (size - 1);
            double stdDev = Math.sqrt(variance);

            return new Snapshot(size, mean, stdDev);
        } finally {
            lock.unlock();
        }
    }

    public record Snapshot(int size, double mean, double stdDev) {}
}
