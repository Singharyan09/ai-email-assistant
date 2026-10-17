package com.example.emailassistant.controller;

import com.example.emailassistant.dto.EmailRequest;
import com.example.emailassistant.dto.EmailResponse;
import com.example.emailassistant.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/summarize")
    public EmailResponse summarize(@Valid @RequestBody EmailRequest request) {
        return new EmailResponse(emailService.summarize(request));
    }

    @PostMapping("/reply")
    public EmailResponse reply(@Valid @RequestBody EmailRequest request) {
        return new EmailResponse(emailService.draftReply(request));
    }
}
