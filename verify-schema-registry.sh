#!/bin/bash

echo "=== Schema Registry Verification Script ==="
echo ""

echo "Step 1: Check if Schema Registry is running"
echo "-------------------------------------------"
curl -s http://localhost:8081/subjects | jq '.' 2>/dev/null || echo "Schema Registry not accessible or no schemas registered yet"
echo ""

echo "Step 2: After sending your first Avro message, run these commands:"
echo "-------------------------------------------------------------------"
echo ""

echo "# List all registered schemas"
echo "curl http://localhost:8081/subjects | jq '.'"
echo ""

echo "# Get schema details for text_message topic"
echo "curl http://localhost:8081/subjects/text_message-value/versions | jq '.'"
echo ""

echo "# Get the actual schema content"
echo "curl http://localhost:8081/subjects/text_message-value/versions/1 | jq '.'"
echo ""

echo "Step 3: Compare message formats"
echo "--------------------------------"
echo ""
echo "To see the difference between JSON and Avro messages:"
echo "1. Send a message using the Avro producer (port 8082)"
echo "2. View messages in Kafka UI (localhost:8080)"
echo "3. Notice the Avro messages show the schema information"
echo ""

echo "Step 4: Delete old JSON messages (if needed)"
echo "---------------------------------------------"
echo "docker exec -it kafka kafka-topics --delete --topic text_message --bootstrap-server localhost:9092"
echo ""

echo "Step 5: View Avro message bytes (advanced)"
echo "-------------------------------------------"
echo "docker exec -it kafka kafka-console-consumer \\"
echo "  --topic text_message \\"
echo "  --bootstrap-server localhost:9092 \\"
echo "  --from-beginning \\"
echo "  --property print.key=true \\"
echo "  --property print.value=true \\"
echo "  --max-messages 1"
echo ""
echo "Note: Avro messages will show as binary (starting with magic byte 0x00 + schema ID)"
echo ""
