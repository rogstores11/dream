package com.dream.robot.service;

import com.dream.robot.model.CommandType;
import com.dream.robot.model.RobotCommand;
import com.dream.robot.model.RobotState;
import com.dream.robot.model.RobotStatus;
import com.dream.robot.model.SensorData;
import org.springframework.stereotype.Service;

@Service
public class RobotService {
    private final PriorityService priorityService;
    private final SerialService serialService;
    private final TaskService taskService;
    private final PredictiveService predictiveService;

    private volatile CommandType activeControl = null;
    private volatile RobotStatus robotStatus = RobotStatus.STOPPED;

    public RobotService(PriorityService priorityService,
                        SerialService serialService,
                        TaskService taskService,
                        PredictiveService predictiveService) {
        this.priorityService = priorityService;
        this.serialService = serialService;
        this.taskService = taskService;
        this.predictiveService = predictiveService;
    }

    public synchronized RobotState handleCommand(RobotCommand command) {
        if (!priorityService.canOverride(command.type(), activeControl)) {
            return state("Ignored command due to lower priority.");
        }

        activeControl = command.type();

        return switch (command.type()) {
            case EMERGENCY_STOP, PREDICTIVE_OVERRIDE -> {
                robotStatus = RobotStatus.STOPPED;
                serialService.sendAngle(0);
                yield state("Robot stopped by " + command.type());
            }
            case MANUAL -> {
                int targetAngle = command.angle() == null ? serialService.latestAngle() : command.angle();
                serialService.sendAngle(targetAngle);
                robotStatus = RobotStatus.RUNNING;
                yield state("Manual command accepted.");
            }
            case TASK -> {
                boolean executed = taskService.execute(command.taskName(), serialService);
                robotStatus = executed ? RobotStatus.RUNNING : RobotStatus.WARNING;
                yield state(executed ? "Task executed." : "Task not found.");
            }
        };
    }

    public synchronized RobotState ingestSensor(SensorData sensorData) {
        serialService.updateSensorData(sensorData);
        if (predictiveService.hasAnomaly(sensorData)) {
            activeControl = CommandType.PREDICTIVE_OVERRIDE;
            robotStatus = RobotStatus.WARNING;
            serialService.sendAngle(0);
            return state("Predictive anomaly detected. Emergency stop applied.");
        }
        return state("Sensor update accepted.");
    }

    public RobotState currentState() {
        return state("Current state snapshot.");
    }

    private RobotState state(String message) {
        SensorData sensorData = serialService.latestSensorData();
        return new RobotState(
                activeControl,
                serialService.latestAngle(),
                sensorData.temperature(),
                sensorData.vibration(),
                sensorData.current(),
                robotStatus,
                message
        );
    }
}
