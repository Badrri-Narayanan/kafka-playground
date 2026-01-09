package com.badrri.playground.config;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:19092",
        "spring.kafka.properties.schema.registry.url=http://localhost:18081"
})
class KafkaProducerConfigTest {

    @Autowired
    private ProducerFactory<String, com.badrri.playground.avro.TextMessage> producerFactory;

    @Autowired
    private KafkaTemplate<String, com.badrri.playground.avro.TextMessage> kafkaTemplate;

    @Test
    void shouldCreateProducerFactory() {
        assertThat(producerFactory).isNotNull();
    }

    @Test
    void shouldCreateKafkaTemplate() {
        assertThat(kafkaTemplate).isNotNull();
    }

    @Test
    void shouldConfigureCorrectSerializers() {
        var config = producerFactory.getConfigurationProperties();

        assertThat(config.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG))
                .isEqualTo(StringSerializer.class);
        assertThat(config.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG))
                .isEqualTo(KafkaAvroSerializer.class);
    }

    @Test
    void shouldInjectBootstrapServers() {
        var config = producerFactory.getConfigurationProperties();

        assertThat(config.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
                .isEqualTo("localhost:19092");
    }

    @Test
    void shouldInjectSchemaRegistryUrl() {
        var config = producerFactory.getConfigurationProperties();

        assertThat(config.get("schema.registry.url"))
                .isEqualTo("http://localhost:18081");
    }
}
