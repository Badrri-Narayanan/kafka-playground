package com.badrri.playground.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Text message to be sent to Kafka")
public record TextMessage(
        @Schema(description = "Title of the message", example = "Test")
        String title,

        @Schema(description = "Body of the message", example = "This is a test message")
        String body,

        @Schema(description = "Sender of the message", example = "user 1")
        String sender,

        @Schema(description = "Receiver of the message", example = "user 2")
        String receiver,

        @Schema(description = "Unique message identifier", example = "35")
        Integer messageId,

        @Schema(description = "Flag indicating if message is important", example = "false")
        Boolean isImportant
) {
}
