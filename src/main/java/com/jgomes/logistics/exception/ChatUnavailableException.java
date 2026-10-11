package com.jgomes.logistics.exception;

/**
 * The chat assistant cannot answer right now: no API key is configured, or the
 * language model provider refused or failed the request. Answered with 503, since
 * the rest of the application is unaffected and the request can be retried.
 */
public class ChatUnavailableException extends RuntimeException {

    public ChatUnavailableException(String message) {
        super(message);
    }
}
