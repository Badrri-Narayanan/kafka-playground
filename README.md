# Kafka Playground

A Docker-based Kafka environment for practicing event streaming, schema registry, and message processing.

## Prerequisites

- Docker
- Docker Compose

## Quick Start

```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| Kafka | 9092 | Kafka broker for host connections |
| Zookeeper | 2181 | Kafka coordination service |
| Schema Registry | 8081 | Schema management service |
| Kafka UI | 8080 | Web-based Kafka management interface |

## Accessing Kafka UI

Open your browser to [http://localhost:8080](http://localhost:8080)

From the UI you can:
- Create and manage topics
- Produce and consume messages
- View consumer groups and lag
- Manage schemas in Schema Registry
- Monitor broker metrics

## Connection Details

### From Your Host Machine

```properties
# Kafka
bootstrap.servers=localhost:9092

# Schema Registry
schema.registry.url=http://localhost:8081
```

### From Docker Containers

```properties
# Kafka
bootstrap.servers=kafka:29092

# Schema Registry
schema.registry.url=http://schema-registry:8081
```

## Basic Kafka Commands

### Using Kafka CLI Tools

Access the Kafka container:
```bash
docker exec -it kafka bash
```

#### Create a Topic
```bash
kafka-topics --create \
  --topic my-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

#### List Topics
```bash
kafka-topics --list --bootstrap-server localhost:9092
```

#### Produce Messages
```bash
kafka-console-producer \
  --topic my-topic \
  --bootstrap-server localhost:9092
```

#### Consume Messages
```bash
kafka-console-consumer \
  --topic my-topic \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

#### Describe Topic
```bash
kafka-topics --describe \
  --topic my-topic \
  --bootstrap-server localhost:9092
```

#### Delete Topic
```bash
kafka-topics --delete \
  --topic my-topic \
  --bootstrap-server localhost:9092
```

## Schema Registry

### Register a Schema

```bash
curl -X POST http://localhost:8081/subjects/my-topic-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"},{\"name\":\"name\",\"type\":\"string\"}]}"
  }'
```

### List All Subjects

```bash
curl http://localhost:8081/subjects
```

### Get Schema Versions

```bash
curl http://localhost:8081/subjects/my-topic-value/versions
```

### Get Specific Schema

```bash
curl http://localhost:8081/subjects/my-topic-value/versions/1
```

### Delete a Subject

```bash
curl -X DELETE http://localhost:8081/subjects/my-topic-value
```

## Programming Language Examples

### Connection Properties

**Java (Spring Boot)**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    properties:
      schema.registry.url: http://localhost:8081
```

**Python (confluent-kafka)**
```python
from confluent_kafka import Producer, Consumer

producer_config = {
    'bootstrap.servers': 'localhost:9092',
    'schema.registry.url': 'http://localhost:8081'
}

consumer_config = {
    'bootstrap.servers': 'localhost:9092',
    'group.id': 'my-group',
    'auto.offset.reset': 'earliest'
}
```

**Node.js (kafkajs)**
```javascript
const { Kafka } = require('kafkajs');

const kafka = new Kafka({
  clientId: 'my-app',
  brokers: ['localhost:9092']
});
```

## Troubleshooting

### Services Won't Start

```bash
# Check logs
docker-compose logs kafka
docker-compose logs schema-registry

# Restart with clean state
docker-compose down -v
docker-compose up -d
```

### Can't Connect to Kafka

Ensure you're using the correct address:
- **From host**: `localhost:9092`
- **From Docker**: `kafka:29092`

### Schema Registry Connection Issues

```bash
# Check if Schema Registry is healthy
curl http://localhost:8081/subjects

# Restart Schema Registry
docker-compose restart schema-registry
```

### Check Service Health

```bash
# View running containers
docker-compose ps

# Check Kafka broker status
docker exec -it kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

## Clean Up

```bash
# Stop services but keep data
docker-compose down

# Stop services and remove all data
docker-compose down -v

# Remove everything including images
docker-compose down -v --rmi all
```

## Resources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html)
- [Kafka UI GitHub](https://github.com/provectus/kafka-ui)
- [KafkaJS Documentation](https://kafka.js.org/)
- [Confluent Python Client](https://docs.confluent.io/kafka-clients/python/current/overview.html)

## License

This is a playground environment for learning and experimentation.

# Implementation of Schema Registry

I'll explain the changes needed to add Avro schema support for the text_message topic using the Schema Registry that's already running in your docker-compose setup.

  Why Add a Schema?

  Currently, you're using JSON serialization which has no schema validation. Adding an Avro schema provides:
  - Schema validation - Ensures all messages conform to the schema
  - Schema evolution - Backward/forward compatibility as your schema changes
  - Better performance - Avro is more compact than JSON
  - Type safety - Strong typing enforced by the registry

  Changes Needed

  1. Add Avro Dependencies (build.gradle)

  You'll need to add these dependencies:

  dependencies {
      // Existing dependencies...

      // Add these for Avro + Schema Registry
      implementation 'org.apache.avro:avro:1.11.3'
      implementation 'io.confluent:kafka-avro-serializer:7.6.0'

      // Add Confluent repository
  }

  repositories {
      mavenCentral()
      maven {
          url "https://packages.confluent.io/maven/"
      }
  }

  2. Create Avro Schema File

  Create src/main/avro/TextMessage.avsc:

  {
    "type": "record",
    "name": "TextMessage",
    "namespace": "com.badrri.playground.avro",
    "fields": [
      {
        "name": "title",
        "type": "string",
        "doc": "Message title or subject"
      },
      {
        "name": "body",
        "type": "string",
        "doc": "Main message content"
      },
      {
        "name": "sender",
        "type": "string",
        "doc": "Sender identifier"
      },
      {
        "name": "receiver",
        "type": "string",
        "doc": "Receiver identifier"
      },
      {
        "name": "messageId",
        "type": "int",
        "doc": "Unique message identifier"
      },
      {
        "name": "isImportant",
        "type": "boolean",
        "default": false,
        "doc": "Priority flag"
      }
    ]
  }

  3. Add Avro Gradle Plugin (build.gradle)

  Add plugin to generate Java classes from Avro schema:

  plugins {
      id 'java'
      id 'org.springframework.boot' version '4.0.1'
      id 'io.spring.dependency-management' version '1.1.7'
      id 'com.github.davidmc24.gradle.plugin.avro' version '1.9.1'  // Add this
  }

  // Configure Avro plugin
  avro {
      createSetters = true
      fieldVisibility = "PRIVATE"
      outputCharacterEncoding = "UTF-8"
      stringType = "String"
  }

  4. Update KafkaProducerConfig.java

  Change from JSON serializer to Avro serializer:

  @Configuration
  public class KafkaProducerConfig {

      @Value("${spring.kafka.bootstrap-servers}")
      private String bootstrapServers;

      @Value("${spring.kafka.properties.schema.registry.url}")
      private String schemaRegistryUrl;

      @Bean
      public ProducerFactory<String, com.badrri.playground.avro.TextMessage> producerFactory() {
          Map<String, Object> configProps = new HashMap<>();
          configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
          configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

          // Change to Avro serializer
          configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                         io.confluent.kafka.serializers.KafkaAvroSerializer.class);

          // Add Schema Registry URL
          configProps.put("schema.registry.url", schemaRegistryUrl);

          return new DefaultKafkaProducerFactory<>(configProps);
      }

      @Bean
      public KafkaTemplate<String, com.badrri.playground.avro.TextMessage> kafkaTemplate() {
          return new KafkaTemplate<>(producerFactory());
      }
  }

  5. Update application.properties

  Add Schema Registry URL (note: port conflict with your app):

  # Server - Change port since Schema Registry uses 8081
  server.port=8082

  # Kafka
  spring.kafka.bootstrap-servers=localhost:9092
  spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
  spring.kafka.producer.value-serializer=io.confluent.kafka.serializers.KafkaAvroSerializer

  # Schema Registry
  spring.kafka.properties.schema.registry.url=http://localhost:8081

  ⚠️ Important: Your Spring Boot app currently uses port 8081, but Schema Registry also uses 8081. You'll need to change your app to port 8082 (or change Schema Registry port in docker-compose).

  6. Update MessageProducerService.java

  Change the type to use generated Avro class:

  @Service
  public class MessageProducerService {

      @Autowired
      private KafkaTemplate<String, com.badrri.playground.avro.TextMessage> kafkaTemplate;

      private static final String TOPIC = "text_message";

      public void sendMessage(com.badrri.playground.avro.TextMessage message) {
          // Rest of the code stays the same
          kafkaTemplate.send(TOPIC, message.getMessageId().toString(), message)
                  .whenComplete((result, ex) -> {
                      // ...
                  });
      }
  }

  7. Update MessageController.java

  You have two options:

  Option A: Keep the current Java record for the REST API and convert to Avro:

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> sendMessage(@RequestBody TextMessage message) {
      // Convert Java record to Avro
      com.badrri.playground.avro.TextMessage avroMessage =
          com.badrri.playground.avro.TextMessage.newBuilder()
              .setTitle(message.title())
              .setBody(message.body())
              .setSender(message.sender())
              .setReceiver(message.receiver())
              .setMessageId(message.messageId())
              .setIsImportant(message.isImportant())
              .build();

      messageProducerService.sendMessage(avroMessage);
      // ...
  }

  Option B: Use the Avro-generated class directly in the controller.

  8. Build Process

  After adding the Avro plugin, run:

  ./gradlew generateAvroJava
  ./gradlew build

  This generates Java classes from your .avsc schema file in build/generated-main-avro-java/.

  Summary of Changes
  ┌────────────────────────┬────────────────────────────┬───────────────────────────────────────────┐
  │       Component        │        Change Type         │                Description                │
  ├────────────────────────┼────────────────────────────┼───────────────────────────────────────────┤
  │ build.gradle           │ Add dependencies + plugin  │ Avro, Schema Registry client, Avro plugin │
  ├────────────────────────┼────────────────────────────┼───────────────────────────────────────────┤
  │ Schema file            │ Create new                 │ src/main/avro/TextMessage.avsc            │
  ├────────────────────────┼────────────────────────────┼───────────────────────────────────────────┤
  │ KafkaProducerConfig    │ Update serializer          │ JSON → KafkaAvroSerializer                │
  ├────────────────────────┼────────────────────────────┼───────────────────────────────────────────┤
  │ application.properties │ Add property + change port │ Schema Registry URL + port 8082           │
  ├────────────────────────┼────────────────────────────┼───────────────────────────────────────────┤
  │ MessageProducerService │ Update type                │ Use generated Avro class                  │
  ├────────────────────────┼────────────────────────────┼───────────────────────────────────────────┤
  │ MessageController      │ Add conversion             │ Java record → Avro message                │
  └────────────────────────┴────────────────────────────┴───────────────────────────────────────────┘
  Benefits After Migration

  1. Schema is registered in Schema Registry (viewable at http://localhost:8081/subjects)
  2. Automatic validation - Invalid messages rejected
  3. Schema evolution - Can update schema with compatibility rules
  4. Better tooling - Kafka UI will show schema info
  5. Type safety - Compile-time validation
