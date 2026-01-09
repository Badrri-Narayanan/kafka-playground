#!/bin/bash

# Test script for Kafka Producer API
# Usage: ./test-api.sh

echo "Testing Kafka Producer API..."
echo ""

# Test 1: Send a simple message
echo "Test 1: Sending a simple message"
curl -X POST http://localhost:8082/api/messages \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Message",
    "body": "This is a test message body",
    "sender": "user123",
    "receiver": "user456",
    "messageId": 1001,
    "isImportant": false
  }'
echo -e "\n"

# Test 2: Send an important message
echo "Test 2: Sending an important message"
curl -X POST http://localhost:8082/api/messages \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Urgent Alert",
    "body": "This is an urgent message",
    "sender": "admin",
    "receiver": "ops-team",
    "messageId": 2001,
    "isImportant": true
  }'
echo -e "\n"

# Test 3: Send another message with different ID
echo "Test 3: Sending another message"
curl -X POST http://localhost:8082/api/messages \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Regular Update",
    "body": "Just a regular status update",
    "sender": "system",
    "receiver": "monitoring",
    "messageId": 3001,
    "isImportant": false
  }'
echo -e "\n"

echo ""
echo "Tests completed!"
echo ""
echo "Verification:"
echo "1. Open Kafka UI: http://localhost:8080"
echo "2. Navigate to Topics -> text_message"
echo "3. View the messages sent above"
echo ""
echo "Schema Registry:"
echo "curl http://localhost:8081/subjects"
echo "curl http://localhost:8081/subjects/text_message-value/versions/1"
