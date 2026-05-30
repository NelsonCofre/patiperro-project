package com.patiperro.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.Instant;

@Data
public class ChatTypingEvent {

	@NotNull
	@Positive
	private Integer idReserva;

	@NotBlank
	private String sender;

	@NotNull
	private Boolean isTyping;

	private Instant timestamp;
}
