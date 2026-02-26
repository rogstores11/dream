package com.dream.robot.task;

import com.dream.robot.service.SerialService;

public interface RobotTask {
    String name();

    void execute(SerialService serialService);
}
