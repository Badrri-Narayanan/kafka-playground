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

# Avro Schema Registry - Implemented ✅

The `spring-kafka-producer` application now uses **Avro serialization with Schema Registry**!

## What Changed

- **Serialization**: JSON → Avro binary format
- **Schema Management**: Automatic registration with Schema Registry
- **Port**: Application moved from 8081 → 8082 (Schema Registry uses 8081)
- **Type Safety**: Compile-time validation with generated Avro classes

## Benefits

- ✅ **Schema Validation** - Invalid messages rejected automatically
- ✅ **Schema Evolution** - Backward/forward compatibility as schema changes
- ✅ **Better Performance** - Avro is more compact than JSON
- ✅ **Type Safety** - Strong typing enforced by the registry
- ✅ **Documentation** - Schema serves as living documentation

## How It Works

1. **Automatic Schema Registration**: When you send your first message, the KafkaAvroSerializer automatically registers the schema with Schema Registry
2. **Schema ID Embedding**: Each message includes a schema ID in its bytes (magic byte + schema ID + data)
3. **Compatibility Checking**: Schema Registry enforces backward compatibility by default

## Quick Start with Avro

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Run the Spring Boot app (on port 8082)
cd spring-kafka-producer
./gradlew bootRun

# 3. Send a message
curl -X POST http://localhost:8082/api/messages \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","body":"Hello Avro","sender":"user1","receiver":"user2","messageId":1,"isImportant":true}'

# 4. Verify schema registration
curl http://localhost:8081/subjects
curl http://localhost:8081/subjects/text_message-value/versions/1
```

## Important: Message Compatibility

**Old JSON messages** and **new Avro messages** are incompatible!

For this playground, delete the old topic to start fresh:
```bash
docker exec -it kafka kafka-topics --delete --topic text_message --bootstrap-server localhost:9092
```

See `spring-kafka-producer/README.md` for detailed documentation.
