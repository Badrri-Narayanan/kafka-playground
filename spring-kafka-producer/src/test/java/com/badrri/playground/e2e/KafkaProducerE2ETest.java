package com.badrri.playground.e2e;

import com.badrri.playground.testutil.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class KafkaProducerE2ETest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.properties.schema.registry.url", () -> "mock://e2e-test");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void shouldSendMessageEndToEnd() throws Exception {
        // Given
        var message = TestDataFactory.createRestDto(
                "E2E Test", "End-to-end validation", "client", "server", 5005, true
        );

        BlockingQueue<ConsumerRecord<String, com.badrri.playground.avro.TextMessage>> records =
                new LinkedBlockingQueue<>();

        // Set up consumer
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "e2e-test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumerProps.put("schema.registry.url", "mock://e2e-test");
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request = new HttpEntity<>(message, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/messages",
                request,
                Map.class
        );

        // Then - Verify HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("Message sent to Kafka topic");
        assertThat(response.getBody().get("messageId")).isEqualTo("5005");

        // Then - Verify message in Kafka
        ConsumerRecord<String, com.badrri.playground.avro.TextMessage> received =
                records.poll(15, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo("5005");
        assertThat(received.topic()).isEqualTo("text_message");

        com.badrri.playground.avro.TextMessage avroMessage = received.value();
        assertThat(avroMessage.getTitle()).isEqualTo("E2E Test");
        assertThat(avroMessage.getBody()).isEqualTo("End-to-end validation");
        assertThat(avroMessage.getSender()).isEqualTo("client");
        assertThat(avroMessage.getReceiver()).isEqualTo("server");
        assertThat(avroMessage.getMessageId()).isEqualTo(5005);
        assertThat(avroMessage.getIsImportant()).isTrue();

        container.stop();
    }

    @Test
    void shouldProduceMessagesInCorrectFormat() throws Exception {
        // Given
        var message = TestDataFactory.createAvroMessage(
                "Format Test", "Testing Avro wire format", "producer", "consumer", 6006, false
        );

        BlockingQueue<ConsumerRecord<String, com.badrri.playground.avro.TextMessage>> records =
                new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "e2e-format-test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumerProps.put("schema.registry.url", "mock://e2e-test");
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
        var restDto = TestDataFactory.createRestDto(
                message.getTitle().toString(),
                message.getBody().toString(),
                message.getSender().toString(),
                message.getReceiver().toString(),
                message.getMessageId(),
                message.getIsImportant()
        );

        HttpHeaders headers2 = new HttpHeaders();
        headers2.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request2 = new HttpEntity<>(restDto, headers2);

        restTemplate.postForEntity("http://localhost:" + port + "/api/messages", request2, Map.class);

        // Then
        ConsumerRecord<String, com.badrri.playground.avro.TextMessage> received =
                records.poll(15, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.value()).isNotNull();
        assertThat(received.value().getTitle()).isEqualTo("Format Test");

        container.stop();
    }
}
