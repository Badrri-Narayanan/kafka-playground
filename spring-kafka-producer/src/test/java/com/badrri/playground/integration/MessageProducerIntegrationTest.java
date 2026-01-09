package com.badrri.playground.integration;

import com.badrri.playground.service.MessageProducerService;
import com.badrri.playground.testutil.TestDataFactory;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"text_message"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"}
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://test"
})
@DirtiesContext
class MessageProducerIntegrationTest {

    @Autowired
    private MessageProducerService messageProducerService;

    @Test
    void shouldSendMessageToEmbeddedKafka() throws Exception {
        // Given
        com.badrri.playground.avro.TextMessage avroMessage = TestDataFactory.createSampleAvroMessage();

        BlockingQueue<ConsumerRecord<String, com.badrri.playground.avro.TextMessage>> records =
                new LinkedBlockingQueue<>();

        // Set up consumer
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumerProps.put("schema.registry.url", "mock://test");
        consumerProps.put("specific.avro.reader", "true");

        DefaultKafkaConsumerFactory<String, com.badrri.playground.avro.TextMessage> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProps = new ContainerProperties("text_message");
        KafkaMessageListenerContainer<String, com.badrri.playground.avro.TextMessage> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProps);

        container.setupMessageListener((MessageListener<String, com.badrri.playground.avro.TextMessage>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, 1);

        // When
        messageProducerService.sendMessage(avroMessage);

        // Then
        ConsumerRecord<String, com.badrri.playground.avro.TextMessage> received =
                records.poll(10, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo("1001");
        assertThat(received.value().getTitle()).isEqualTo("Test Title");
        assertThat(received.value().getBody()).isEqualTo("Test Body Content");
        assertThat(received.value().getSender()).isEqualTo("sender123");
        assertThat(received.value().getReceiver()).isEqualTo("receiver456");
        assertThat(received.value().getMessageId()).isEqualTo(1001);
        assertThat(received.value().getIsImportant()).isFalse();

        container.stop();
    }

    @Test
    void shouldUseCorrectTopic() throws Exception {
        // Given
        com.badrri.playground.avro.TextMessage avroMessage = TestDataFactory.createAvroMessage(
                "Topic Test", "Testing topic routing", "sender", "receiver", 2002, false
        );

        BlockingQueue<ConsumerRecord<String, com.badrri.playground.avro.TextMessage>> records =
                new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-2");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumerProps.put("schema.registry.url", "mock://test");
        consumerProps.put("specific.avro.reader", "true");

        DefaultKafkaConsumerFactory<String, com.badrri.playground.avro.TextMessage> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProps = new ContainerProperties("text_message");
        KafkaMessageListenerContainer<String, com.badrri.playground.avro.TextMessage> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProps);

        container.setupMessageListener((MessageListener<String, com.badrri.playground.avro.TextMessage>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, 1);

        // When
        messageProducerService.sendMessage(avroMessage);

        // Then
        ConsumerRecord<String, com.badrri.playground.avro.TextMessage> received =
                records.poll(10, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.topic()).isEqualTo("text_message");
        assertThat(received.key()).isEqualTo("2002");

        container.stop();
    }
}
