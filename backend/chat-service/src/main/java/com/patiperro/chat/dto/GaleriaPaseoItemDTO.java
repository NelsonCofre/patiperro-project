package com.patiperro.chat.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class GaleriaPaseoItemDTO {
	Integer idMensaje;
	String urlMedia;
	String imageUrl;
	String comentario;
	String content;
	Instant fechaEnvio;
	Instant timestamp;
	Integer idUsuario;
}
