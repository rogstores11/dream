package com.dream.robot.service;

import com.dream.robot.model.CommandType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriorityServiceTest {
    private final PriorityService priorityService = new PriorityService();

    @Test
    void higherPriorityCanOverride() {
        assertTrue(priorityService.canOverride(CommandType.EMERGENCY_STOP, CommandType.MANUAL));
    }

    @Test
    void lowerPriorityCannotOverride() {
        assertFalse(priorityService.canOverride(CommandType.TASK, CommandType.PREDICTIVE_OVERRIDE));
    }
}
