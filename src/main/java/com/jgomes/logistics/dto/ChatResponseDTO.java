package com.jgomes.logistics.dto;

import java.util.List;

/**
 * @param reply     the assistant's answer
 * @param toolsUsed the tools it called to get there, in order, so the interface
 *                  can show the answer was read from the data rather than guessed
 */
public record ChatResponseDTO(String reply, List<String> toolsUsed) {
}
