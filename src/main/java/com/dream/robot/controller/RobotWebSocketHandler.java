package com.dream.robot.controller;

import com.dream.robot.model.RobotCommand;
import com.dream.robot.model.RobotState;
import com.dream.robot.service.RobotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RobotWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(RobotWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final RobotService robotService;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public RobotWebSocketHandler(ObjectMapper objectMapper, RobotService robotService) {
        this.objectMapper = objectMapper;
        this.robotService = robotService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        send(session, robotService.currentState());
        logger.info("Unity client connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        RobotCommand command = objectMapper.readValue(message.getPayload(), RobotCommand.class);
        RobotState response = robotService.handleCommand(command);
        broadcast(response);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        logger.info("Unity client disconnected: {}", session.getId());
    }

    private void broadcast(RobotState state) throws IOException {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                send(session, state);
            }
        }
    }

    private void send(WebSocketSession session, RobotState state) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(state)));
    }
}
