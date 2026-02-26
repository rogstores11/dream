package com.dream.robot.model;

public record RobotState(
        CommandType type,
        int angle,
        double temperature,
        double vibration,
        double current,
        RobotStatus status,
        String message
) {
}
