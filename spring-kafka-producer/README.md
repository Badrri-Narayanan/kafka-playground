# Spring Kafka Producer

A Spring Boot application that publishes text messages to a Kafka topic with **Avro serialization** and Schema Registry integration.

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

The application will start on **http://localhost:8082**

## API Documentation

### Scalar API Reference ✅

We use **Scalar** as an alternative to Swagger UI (which isn't compatible with Spring Boot 3.5.9 yet).

Access the interactive API documentation at:
- **API Docs UI:** http://localhost:8082/api-docs.html
- **OpenAPI Spec:** http://localhost:8082/openapi.yaml

Scalar provides:
- 🎨 Modern, beautiful interface
- 🚀 Try out API requests directly
- 📝 Complete request/response examples
- 🔍 Search functionality
- 📱 Mobile-friendly design

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

## Testing the API

### Quick Test Script

Run the provided test script to send multiple test messages:

```bash
./test-api.sh
```

This will send 3 test messages and show verification steps.

### Manual Testing with cURL

```bash
curl -X POST http://localhost:8082/api/messages \
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

- Spring Boot 3.5.9
- Spring Kafka
- Spring Web MVC
- Apache Avro 1.11.3
- Confluent Kafka Avro Serializer 7.6.0
- Java 21
- Gradle 8.14.3

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

### Port 8082 already in use

The application runs on port 8082 to avoid conflict with Schema Registry (port 8081).
Change the port in `application.properties` if needed.

### Swagger UI Not Loading

If Swagger UI doesn't load, check:
1. Application is running: `curl http://localhost:8082/actuator/health` (if actuator is enabled)
2. Try the direct URL: http://localhost:8082/swagger-ui/index.html
3. Check application logs for SpringDoc initialization messages

**Alternative:** Use the `test-api.sh` script or curl commands to test the API.

## Avro Schema Registry Integration

This application now uses **Avro serialization** with Schema Registry for better schema validation and evolution.

### How Schema Registration Works

The schema is **automatically registered** when you send the first message:

1. Spring app creates an Avro TextMessage object
2. KafkaAvroSerializer extracts the schema from the message
3. Serializer checks Schema Registry for the schema
4. If not found, it registers the schema at `text_message-value`
5. Schema Registry returns a schema ID (e.g., 1)
6. Serializer embeds schema ID in the message bytes
7. Message is sent to Kafka

**No manual registration needed!** The serializer handles everything automatically.

### Verify Schema Registration

After sending your first message:

```bash
# List all registered schemas
curl http://localhost:8081/subjects

# Get schema versions
curl http://localhost:8081/subjects/text_message-value/versions

# View the actual schema
curl http://localhost:8081/subjects/text_message-value/versions/1
```

### Message Format Compatibility

**Important:** Avro and JSON messages are incompatible!

- **Old JSON messages** in the topic cannot be read by Avro deserializers
- **New Avro messages** have a different binary format (magic byte + schema ID + data)

**For this playground environment**, delete the old topic to start fresh:

```bash
docker exec -it kafka kafka-topics --delete \
  --topic text_message \
  --bootstrap-server localhost:9092
```

The topic will be auto-created when you send the first Avro message.

### Schema Evolution

You can modify the Avro schema over time:

1. Update `src/main/avro/TextMessage.avsc`
2. Rebuild: `./gradlew generateAvroJava build`
3. Send a message with the new schema
4. Schema Registry validates compatibility (backward mode by default)
5. If compatible, registers as version 2

**Backward compatibility** (default) means:
- New consumers can read old messages
- Safe to add optional fields with defaults
- Cannot remove required fields

## Next Steps

- Add consumer application with Avro deserialization
- Implement error handling and retry logic
- Add unit and integration tests
- Explore schema evolution scenarios
- Add message validation at REST layer
