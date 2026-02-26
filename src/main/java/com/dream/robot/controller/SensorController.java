package com.dream.robot.controller;

import com.dream.robot.model.RobotState;
import com.dream.robot.model.SensorData;
import com.dream.robot.service.RobotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {
    private final RobotService robotService;

    public SensorController(RobotService robotService) {
        this.robotService = robotService;
    }

    @PostMapping
    public ResponseEntity<RobotState> ingest(@RequestBody SensorData sensorData) {
        return ResponseEntity.ok(robotService.ingestSensor(sensorData));
    }
}
