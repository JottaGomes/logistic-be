package com.jgomes.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One turn of the conversation as the browser holds it.
 *
 * Only "user" and "assistant" are accepted: the system prompt and tool results
 * are added by the backend, and letting a client send them would let it rewrite
 * the assistant's instructions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    @NotBlank(message = "must not be blank")
    @Pattern(regexp = "user|assistant", message = "must be user or assistant")
    private String role;

    @NotBlank(message = "must not be blank")
    @Size(max = 4000, message = "must be at most 4000 characters")
    private String content;
}
