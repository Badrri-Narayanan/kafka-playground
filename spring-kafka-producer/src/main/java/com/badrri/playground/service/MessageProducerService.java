package com.badrri.playground.service;

import com.badrri.playground.model.TextMessage;
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

    private final KafkaTemplate<String, TextMessage> kafkaTemplate;

    public MessageProducerService(KafkaTemplate<String, TextMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(TextMessage message) {
        String key = message.messageId() != null ? message.messageId().toString() : "unknown";

        CompletableFuture<SendResult<String, TextMessage>> future =
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
