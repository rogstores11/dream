package com.dream.robot.config;

import com.dream.robot.controller.RobotWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final RobotWebSocketHandler robotWebSocketHandler;

    public WebSocketConfig(RobotWebSocketHandler robotWebSocketHandler) {
        this.robotWebSocketHandler = robotWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(robotWebSocketHandler, "/ws/robot").setAllowedOrigins("*");
    }
}
