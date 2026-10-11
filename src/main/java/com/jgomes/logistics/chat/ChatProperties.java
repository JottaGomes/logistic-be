package com.jgomes.logistics.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings for the shipment chat assistant (prefix {@code app.chat}).
 *
 * The assistant calls Groq's OpenAI-compatible chat completions API. Without an
 * API key the rest of the application works normally and only /api/chat answers
 * 503, so the key is never required to build, test or run the project.
 */
@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {

    /** Groq API key. Read from the GROQ_API_KEY environment variable; never commit a real value. */
    private String apiKey = "";

    private String baseUrl = "https://api.groq.com/openai/v1";

    /** Any Groq model with tool use; see https://console.groq.com/docs/tool-use. */
    private String model = "openai/gpt-oss-120b";

    /**
     * Most model round trips one question may take. Each round can call tools;
     * the cap stops a model that keeps calling them from looping on the API bill.
     */
    private int maxToolRounds = 5;

    /** Conversation turns sent with each question, oldest dropped first. */
    private int maxHistory = 20;

    private Duration timeout = Duration.ofSeconds(30);

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getMaxToolRounds() { return maxToolRounds; }
    public void setMaxToolRounds(int maxToolRounds) { this.maxToolRounds = maxToolRounds; }
    public int getMaxHistory() { return maxHistory; }
    public void setMaxHistory(int maxHistory) { this.maxHistory = maxHistory; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
