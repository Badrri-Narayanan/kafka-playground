package com.badrri.playground.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.badrri.playground.testutil.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
    partitions = 1,
    topics = {"text_message"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9094", "port=9094"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.properties.schema.registry.url=mock://test"
})
@DirtiesContext
class MessageControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldPublishMessageViaRestEndpoint() throws Exception {
        // Given
        var message = TestDataFactory.createRestDto(
            "Integration Test", "Full stack test", "api", "kafka", 3003, true
        );
        String jsonContent = objectMapper.writeValueAsString(message);

        BlockingQueue<ConsumerRecord<String, com.badrri.playground.avro.TextMessage>> records =
            new LinkedBlockingQueue<>();

        // Set up consumer
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "integration-test-group");
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
        mockMvc.perform(post("/api/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("Message sent to Kafka topic"))
            .andExpect(jsonPath("$.messageId").value("3003"));

        // Then
        ConsumerRecord<String, com.badrri.playground.avro.TextMessage> received =
            records.poll(10, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo("3003");
        assertThat(received.value().getTitle()).isEqualTo("Integration Test");
        assertThat(received.value().getBody()).isEqualTo("Full stack test");
        assertThat(received.value().getSender()).isEqualTo("api");
        assertThat(received.value().getReceiver()).isEqualTo("kafka");
        assertThat(received.value().getMessageId()).isEqualTo(3003);
        assertThat(received.value().getIsImportant()).isTrue();

        container.stop();
    }

    @Test
    void shouldReturnAcceptedStatus() throws Exception {
        // Given
        var message = TestDataFactory.createSampleRestDto();
        String jsonContent = objectMapper.writeValueAsString(message);

        // When & Then
        mockMvc.perform(post("/api/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.messageId").exists());
    }
}
