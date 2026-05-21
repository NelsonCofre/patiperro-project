package com.patiperro.chat.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/** Ítem de la galería de fotos del paseo (mensajes tipo IMAGEN) para el tutor tras paseo finalizado. */
@Value
@Builder
public class GaleriaPaseoItemDTO {
	Integer idMensaje;
	Integer idUsuario;
	/** Ruta relativa de la imagen (alias JSON: {@code imageUrl}). */
	String urlMedia;
	String imageUrl;
	/** Comentario opcional del paseador (alias JSON: {@code content}). */
	String comentario;
	String content;
	Instant fechaEnvio;
	Instant timestamp;
}
