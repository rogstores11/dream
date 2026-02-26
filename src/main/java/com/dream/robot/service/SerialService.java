package com.dream.robot.service;

import com.dream.robot.model.SensorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SerialService {
    private static final Logger logger = LoggerFactory.getLogger(SerialService.class);

    private volatile int latestAngle = 90;
    private volatile SensorData latestSensorData = new SensorData(30, 0.01, 0.4);

    public void sendAngle(int angle) {
        latestAngle = angle;
        logger.info("Sending angle {} to Arduino/ESP32", angle);
    }

    public int latestAngle() {
        return latestAngle;
    }

    public SensorData latestSensorData() {
        return latestSensorData;
    }

    public void updateSensorData(SensorData sensorData) {
        latestSensorData = sensorData;
    }
}
