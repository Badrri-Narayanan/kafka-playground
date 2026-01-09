package com.badrri.playground.testutil;

import com.badrri.playground.model.TextMessage;

public class TestDataFactory {

    public static TextMessage createSampleRestDto() {
        return new TextMessage(
                "Test Title",
                "Test Body Content",
                "sender123",
                "receiver456",
                1001,
                false
        );
    }

    public static TextMessage createRestDto(String title, String body, String sender,
                                            String receiver, Integer messageId, Boolean isImportant) {
        return new TextMessage(title, body, sender, receiver, messageId, isImportant);
    }

    public static com.badrri.playground.avro.TextMessage createSampleAvroMessage() {
        return com.badrri.playground.avro.TextMessage.newBuilder()
                .setTitle("Test Title")
                .setBody("Test Body Content")
                .setSender("sender123")
                .setReceiver("receiver456")
                .setMessageId(1001)
                .setIsImportant(false)
                .build();
    }

    public static com.badrri.playground.avro.TextMessage createAvroMessage(String title, String body,
                                                                             String sender, String receiver,
                                                                             int messageId, boolean isImportant) {
        return com.badrri.playground.avro.TextMessage.newBuilder()
                .setTitle(title)
                .setBody(body)
                .setSender(sender)
                .setReceiver(receiver)
                .setMessageId(messageId)
                .setIsImportant(isImportant)
                .build();
    }
}
