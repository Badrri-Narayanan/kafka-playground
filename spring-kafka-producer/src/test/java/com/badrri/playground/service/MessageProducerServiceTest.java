package com.badrri.playground.service;

import com.badrri.playground.testutil.TestDataFactory;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageProducerServiceTest {

    @Mock
    private KafkaTemplate<String, com.badrri.playground.avro.TextMessage> kafkaTemplate;

    @Mock
    private SendResult<String, com.badrri.playground.avro.TextMessage> sendResult;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<com.badrri.playground.avro.TextMessage> messageCaptor;

    private MessageProducerService messageProducerService;

    @BeforeEach
    void setUp() {
        messageProducerService = new MessageProducerService(kafkaTemplate);
    }

    @Test
    void shouldSendMessageSuccessfully() {
        // Given
        com.badrri.playground.avro.TextMessage avroMessage = TestDataFactory.createSampleAvroMessage();
        CompletableFuture<SendResult<String, com.badrri.playground.avro.TextMessage>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(anyString(), anyString(), any(com.badrri.playground.avro.TextMessage.class)))
                .thenReturn(future);

        // When
        messageProducerService.sendMessage(avroMessage);

        // Then
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), messageCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("text_message");
        assertThat(keyCaptor.getValue()).isEqualTo("1001");
        assertThat(messageCaptor.getValue()).isEqualTo(avroMessage);
    }

    @Test
    void shouldUseMessageIdAsKey() {
        // Given
        com.badrri.playground.avro.TextMessage avroMessage = TestDataFactory.createAvroMessage(
                "Title", "Body", "sender", "receiver", 5678, true
        );
        CompletableFuture<SendResult<String, com.badrri.playground.avro.TextMessage>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(anyString(), anyString(), any(com.badrri.playground.avro.TextMessage.class)))
                .thenReturn(future);

        // When
        messageProducerService.sendMessage(avroMessage);

        // Then
        verify(kafkaTemplate).send(eq("text_message"), eq("5678"), eq(avroMessage));
    }

    @Test
    void shouldHandleMessageWithDifferentContent() {
        // Given
        com.badrri.playground.avro.TextMessage avroMessage = TestDataFactory.createAvroMessage(
                "Important Alert",
                "System maintenance scheduled",
                "admin",
                "all-users",
                9999,
                true
        );
        CompletableFuture<SendResult<String, com.badrri.playground.avro.TextMessage>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(anyString(), anyString(), any(com.badrri.playground.avro.TextMessage.class)))
                .thenReturn(future);

        // When
        messageProducerService.sendMessage(avroMessage);

        // Then
        verify(kafkaTemplate).send(
                eq("text_message"),
                eq("9999"),
                messageCaptor.capture()
        );

        com.badrri.playground.avro.TextMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getTitle()).isEqualTo("Important Alert");
        assertThat(capturedMessage.getBody()).isEqualTo("System maintenance scheduled");
        assertThat(capturedMessage.getSender()).isEqualTo("admin");
        assertThat(capturedMessage.getReceiver()).isEqualTo("all-users");
        assertThat(capturedMessage.getMessageId()).isEqualTo(9999);
        assertThat(capturedMessage.getIsImportant()).isTrue();
    }

    @Test
    void shouldSendToCorrectTopic() {
        // Given
        com.badrri.playground.avro.TextMessage avroMessage = TestDataFactory.createSampleAvroMessage();
        CompletableFuture<SendResult<String, com.badrri.playground.avro.TextMessage>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(anyString(), anyString(), any(com.badrri.playground.avro.TextMessage.class)))
                .thenReturn(future);

        // When
        messageProducerService.sendMessage(avroMessage);

        // Then
        verify(kafkaTemplate).send(
                topicCaptor.capture(),
                anyString(),
                any(com.badrri.playground.avro.TextMessage.class)
        );

        assertThat(topicCaptor.getValue()).isEqualTo("text_message");
    }
}
