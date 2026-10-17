package com.example.emailassistant.client;

/**
 * A failed AI provider call. The message is safe to show to the user and never contains the API key.
 */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message) {
        super(message);
    }
}
