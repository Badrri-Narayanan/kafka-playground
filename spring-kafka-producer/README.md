# Spring Kafka Producer

A Spring Boot application that publishes text messages to a Kafka topic with Swagger UI for API documentation.

## Prerequisites

- Java 21
- Docker and Docker Compose (for running Kafka)
- Gradle

## Getting Started

### 1. Start Kafka Infrastructure

Make sure Kafka is running using the docker-compose in the parent directory:

```bash
cd ..
docker-compose up -d
```

### 2. Build the Application

```bash
./gradlew build
```

### 3. Run the Application

```bash
./gradlew bootRun
```

The application will start on **http://localhost:8081**

## API Documentation

### Swagger UI

Access the interactive API documentation at:
**http://localhost:8081/swagger-ui.html**

### OpenAPI Spec

The OpenAPI specification is available at:
**http://localhost:8081/api-docs**

## API Endpoints

### POST /api/messages

Publishes a text message to the `text_message` Kafka topic.

**Request Body:**
```json
{
  "title": "Test",
  "body": "This is a test message",
  "sender": "user 1",
  "receiver": "user 2",
  "messageId": 35,
  "isImportant": false
}
```

**Response:**
```json
{
  "status": "Message sent to Kafka topic",
  "messageId": "35"
}
```

**Status Code:** 202 Accepted

## Testing with cURL

```bash
curl -X POST http://localhost:8081/api/messages \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test",
    "body": "This is a test message",
    "sender": "user 1",
    "receiver": "user 2",
    "messageId": 35,
    "isImportant": false
  }'
```

## Configuration

The application is configured via `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8081

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Swagger Configuration
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

## Verifying Messages in Kafka

### Using Kafka UI

1. Open http://localhost:8080
2. Navigate to Topics
3. Click on `text_message` topic
4. View the messages

### Using Kafka CLI

```bash
# Access Kafka container
docker exec -it kafka bash

# Consume messages from the topic
kafka-console-consumer \
  --topic text_message \
  --bootstrap-server localhost:9092 \
  --from-beginning \
  --property print.key=true \
  --property print.value=true
```

## Project Structure

```
src/main/java/com/badrri/playground/
├── PlaygroundApplication.java       # Main application class
├── config/
│   └── KafkaProducerConfig.java    # Kafka producer configuration
├── controller/
│   └── MessageController.java      # REST API controller
├── model/
│   └── TextMessage.java            # Message DTO
└── service/
    └── MessageProducerService.java # Kafka producer service
```

## Dependencies

- Spring Boot 4.0.1
- Spring Kafka
- Spring Web MVC
- SpringDoc OpenAPI (Swagger) 2.7.0
- Java 21

## Troubleshooting

### Application won't connect to Kafka

Ensure Kafka is running:
```bash
docker-compose ps
```

### Check application logs

```bash
./gradlew bootRun --info
```

### Port 8081 already in use

Change the port in `application.properties`:
```properties
server.port=8082
```

## Next Steps

- Add message validation
- Implement error handling
- Add consumer application
- Integrate Schema Registry for Avro serialization
- Add unit and integration tests
