package com.patiperro.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class ChatMessageInbound {

	@NotBlank
	@Size(max = 160)
	private String sender;

	@NotBlank
	@Size(max = 4000)
	private String content;

	private Instant timestamp;

	@NotNull
	@Positive
	private Integer idReserva;

	@NotNull
	@Positive
	private Integer idUsuario;
}
