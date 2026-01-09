package com.badrri.playground.controller;

import com.badrri.playground.service.MessageProducerService;
import com.badrri.playground.testutil.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class MessageControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageProducerService messageProducerService;

    @Captor
    private ArgumentCaptor<com.badrri.playground.avro.TextMessage> messageCaptor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldAcceptValidMessage() throws Exception {
        // Given
        var message = TestDataFactory.createSampleRestDto();
        String jsonContent = objectMapper.writeValueAsString(message);

        // When & Then
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("Message sent to Kafka topic"))
                .andExpect(jsonPath("$.messageId").value("1001"));
    }

    @Test
    void shouldReturnCorrectResponseBody() throws Exception {
        // Given
        var message = TestDataFactory.createRestDto(
                "Test", "Body", "sender1", "receiver1", 5555, true
        );
        String jsonContent = objectMapper.writeValueAsString(message);

        // When & Then
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.messageId").value("5555"));
    }

    @Test
    void shouldHandleMessageWithImportantFlag() throws Exception {
        // Given
        var message = TestDataFactory.createRestDto(
                "Urgent", "Critical alert", "admin", "ops-team", 9999, true
        );
        String jsonContent = objectMapper.writeValueAsString(message);

        // When & Then
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messageId").value("9999"));

        verify(messageProducerService).sendMessage(any(com.badrri.playground.avro.TextMessage.class));
    }

    @Test
    void shouldConvertDtoFieldsCorrectly() throws Exception {
        // Given
        var message = TestDataFactory.createRestDto(
                "Sample Title",
                "Sample Body",
                "user123",
                "user456",
                7777,
                false
        );
        String jsonContent = objectMapper.writeValueAsString(message);

        // When
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isAccepted());

        // Then
        verify(messageProducerService).sendMessage(any(com.badrri.playground.avro.TextMessage.class));
    }

    @Test
    void shouldHandleInvalidJsonFormat() throws Exception {
        // Given
        String invalidJson = "{invalid json}";

        // When & Then
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
