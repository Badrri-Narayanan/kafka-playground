package com.badrri.playground.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class MessageProducerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerService.class);
    private static final String TOPIC_NAME = "text_message";

    private final KafkaTemplate<String, com.badrri.playground.avro.TextMessage> kafkaTemplate;

    public MessageProducerService(KafkaTemplate<String, com.badrri.playground.avro.TextMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(com.badrri.playground.avro.TextMessage message) {
        String key = String.valueOf(message.getMessageId());

        CompletableFuture<SendResult<String, com.badrri.playground.avro.TextMessage>> future =
            kafkaTemplate.send(TOPIC_NAME, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Sent message=[{}] with offset=[{}]",
                    message,
                    result.getRecordMetadata().offset());
            } else {
                logger.error("Unable to send message=[{}] due to: {}",
                    message,
                    ex.getMessage());
            }
        });
    }
}
