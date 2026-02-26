package com.dream.robot.task;

import com.dream.robot.service.SerialService;
import org.springframework.stereotype.Component;

@Component
public class WaveTask implements RobotTask {
    @Override
    public String name() {
        return "wave";
    }

    @Override
    public void execute(SerialService serialService) {
        int[] sequence = {40, 120, 40, 120, 90};
        for (int angle : sequence) {
            serialService.sendAngle(angle);
        }
    }
}
