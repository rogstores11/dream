package com.dream.robot.service;

import com.dream.robot.task.RobotTask;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskService {
    private final Map<String, RobotTask> taskRegistry;

    public TaskService(java.util.List<RobotTask> tasks) {
        this.taskRegistry = tasks.stream().collect(Collectors.toMap(RobotTask::name, Function.identity()));
    }

    public boolean execute(String taskName, SerialService serialService) {
        return Optional.ofNullable(taskRegistry.get(taskName))
                .map(task -> {
                    task.execute(serialService);
                    return true;
                })
                .orElse(false);
    }
}
