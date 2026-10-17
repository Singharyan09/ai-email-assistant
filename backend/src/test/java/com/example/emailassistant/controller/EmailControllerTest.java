package com.example.emailassistant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.emailassistant.client.AiProviderException;
import com.example.emailassistant.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmailController.class)
class EmailControllerTest {

    private static final String EMAIL = "{\"subject\":\"Hi\",\"body\":\"Lunch at noon?\",\"tone\":\"friendly\"}";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EmailService emailService;

    @Test
    void summarizeReturnsResult() throws Exception {
        when(emailService.summarize(any())).thenReturn("- Lunch at noon");

        mvc.perform(post("/api/email/summarize").contentType(MediaType.APPLICATION_JSON).content(EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("- Lunch at noon"));
    }

    @Test
    void replyReturnsResult() throws Exception {
        when(emailService.draftReply(any())).thenReturn("Sounds good!");

        mvc.perform(post("/api/email/reply").contentType(MediaType.APPLICATION_JSON).content(EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Sounds good!"));
    }

    @Test
    void providerFailureReturns502WithMessage() throws Exception {
        when(emailService.summarize(any())).thenThrow(new AiProviderException("AI provider returned HTTP 404: nope"));

        mvc.perform(post("/api/email/summarize").contentType(MediaType.APPLICATION_JSON).content(EMAIL))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("AI provider returned HTTP 404: nope"));
    }

    @Test
    void emptyBodyReturns400WithMessage() throws Exception {
        mvc.perform(post("/api/email/summarize").contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email body must not be empty"));
    }
}
