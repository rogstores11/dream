package com.dream.robot.service;

import com.dream.robot.model.CommandType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PriorityService {
    private static final Map<CommandType, Integer> PRIORITY = Map.of(
            CommandType.EMERGENCY_STOP, 1,
            CommandType.PREDICTIVE_OVERRIDE, 2,
            CommandType.MANUAL, 3,
            CommandType.TASK, 4
    );

    public boolean canOverride(CommandType incoming, CommandType current) {
        if (current == null) {
            return true;
        }
        return PRIORITY.get(incoming) <= PRIORITY.get(current);
    }
}
