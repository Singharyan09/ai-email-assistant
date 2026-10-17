package com.example.emailassistant.service;

import com.example.emailassistant.client.AiClient;
import com.example.emailassistant.dto.EmailRequest;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final String SUMMARY_PROMPT =
            "You summarize emails. Reply with 2-4 short bullet points covering the key points "
                    + "and any action items. Do not add commentary.";

    private static final String REPLY_PROMPT =
            "You draft email replies on behalf of the user. Write only the reply body, "
                    + "with no subject line. Keep it concise and match the requested tone.";

    private final AiClient aiClient;

    public EmailService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public String summarize(EmailRequest request) {
        return aiClient.complete(SUMMARY_PROMPT, format(request));
    }

    public String draftReply(EmailRequest request) {
        String tone = (request.tone() == null || request.tone().isBlank()) ? "professional" : request.tone();
        return aiClient.complete(REPLY_PROMPT + " Tone: " + tone + ".", format(request));
    }

    private String format(EmailRequest request) {
        String subject = request.subject() == null ? "(no subject)" : request.subject();
        return "Subject: " + subject + "\n\n" + request.body();
    }
}
