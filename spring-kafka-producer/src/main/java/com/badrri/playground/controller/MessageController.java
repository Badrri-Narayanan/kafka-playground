package com.badrri.playground.controller;

import com.badrri.playground.model.TextMessage;
import com.badrri.playground.service.MessageProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@Tag(name = "Message API", description = "APIs for publishing messages to Kafka")
public class MessageController {

    private final MessageProducerService messageProducerService;

    public MessageController(MessageProducerService messageProducerService) {
        this.messageProducerService = messageProducerService;
    }

    @PostMapping
    @Operation(
        summary = "Publish a message to Kafka",
        description = "Publishes a text message to the 'text_message' Kafka topic"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Message accepted for publishing"),
        @ApiResponse(responseCode = "400", description = "Invalid message format")
    })
    public ResponseEntity<Map<String, String>> publishMessage(@RequestBody TextMessage message) {
        // Convert REST DTO to Avro message
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

        Map<String, String> response = new HashMap<>();
        response.put("status", "Message sent to Kafka topic");
        response.put("messageId", message.messageId() != null ? message.messageId().toString() : "N/A");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
