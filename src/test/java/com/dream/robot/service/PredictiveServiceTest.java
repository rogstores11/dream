package com.dream.robot.service;

import com.dream.robot.model.SensorData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class PredictiveServiceTest {
    private PredictiveService predictiveService;

    @BeforeEach
    void setUp() {
        predictiveService = new PredictiveService();
        ReflectionTestUtils.setField(predictiveService, "maxTemperature", 70.0);
        ReflectionTestUtils.setField(predictiveService, "maxVibration", 0.8);
        ReflectionTestUtils.setField(predictiveService, "maxCurrent", 2.0);
    }

    @Test
    void detectsAnomaly() {
        assertTrue(predictiveService.hasAnomaly(new SensorData(80, 0.1, 0.5)));
    }

    @Test
    void acceptsHealthyData() {
        assertFalse(predictiveService.hasAnomaly(new SensorData(35, 0.02, 0.3)));
    }
}
