package com.dream.robot.service;

import com.dream.robot.model.SensorData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PredictiveService {
    @Value("${predictive.temperature-threshold:70}")
    private double maxTemperature;

    @Value("${predictive.vibration-threshold:0.8}")
    private double maxVibration;

    @Value("${predictive.current-threshold:2.0}")
    private double maxCurrent;

    public boolean hasAnomaly(SensorData data) {
        return data.temperature() > maxTemperature
                || data.vibration() > maxVibration
                || data.current() > maxCurrent;
    }
}
