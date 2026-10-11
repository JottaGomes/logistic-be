package com.jgomes.logistics.controller;

import com.jgomes.logistics.chat.ChatService;
import com.jgomes.logistics.dto.ApiResponse;
import com.jgomes.logistics.dto.ChatRequestDTO;
import com.jgomes.logistics.dto.ChatResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Questions about shipments, answered by a language model over read-only tools.
 * Behind the same authentication as the rest of /api, since its answers carry the
 * same data.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponseDTO>> ask(@Valid @RequestBody ChatRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(service.reply(request.getMessages())));
    }
}
