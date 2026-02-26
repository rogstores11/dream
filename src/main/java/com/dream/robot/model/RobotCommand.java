package com.dream.robot.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RobotCommand(
        @NotNull CommandType type,
        @Min(0) @Max(180) Integer angle,
        String taskName
) {
}
