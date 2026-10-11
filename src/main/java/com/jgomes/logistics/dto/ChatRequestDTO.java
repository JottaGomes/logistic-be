package com.jgomes.logistics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The conversation so far, ending with the user's new question. The backend keeps
 * no chat state: the browser sends the history each time, and the backend trims it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {

    @NotEmpty(message = "must not be empty")
    @Size(max = 50, message = "must be at most 50 messages")
    private List<@Valid ChatMessageDTO> messages;
}
