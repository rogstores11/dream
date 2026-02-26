# Modular Real-Time Digital Twin Robotic Control System (Spring Boot)

This repository contains a Spring Boot backend skeleton for a distributed robotic control architecture:

- **Unity** as digital twin and operator UI.
- **Spring Boot** as orchestration and intelligence layer.
- **Arduino/ESP32** as low-level motor and sensor interface.

## Implemented Modules

- WebSocket endpoint (`/ws/robot`) for command/state communication.
- `RobotService` for command routing and state management.
- `PriorityService` for safety-first command arbitration.
- `TaskService` with strategy-based extensible tasks (`WaveTask` sample).
- `SerialService` abstraction for hardware communication.
- `PredictiveService` for anomaly checks based on thresholds.
- REST endpoint (`POST /api/sensors`) for ingesting sensor telemetry.

## Command JSON

```json
{
  "type": "MANUAL",
  "angle": 90
}
```

`type` options:
- `EMERGENCY_STOP`
- `PREDICTIVE_OVERRIDE`
- `MANUAL`
- `TASK`

For `TASK`, include `taskName`:

```json
{
  "type": "TASK",
  "taskName": "wave"
}
```

## State JSON (example)

```json
{
  "type": "MANUAL",
  "angle": 90,
  "temperature": 30.0,
  "vibration": 0.01,
  "current": 0.4,
  "status": "RUNNING",
  "message": "Manual command accepted."
}
```

## Run

```bash
mvn spring-boot:run
```

## Next Steps

- Connect `SerialService` to real serial transport (e.g. jSerialComm).
- Add persistence for telemetry and anomaly logs.
- Add authentication for Unity clients.
- Expand task library and multi-actuator support.
