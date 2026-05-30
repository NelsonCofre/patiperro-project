package com.patiperro.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "mensaje")
@Getter
@Setter
@NoArgsConstructor
public class Mensaje {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_mensaje", nullable = false)
	private Integer id;

	@Column(name = "id_usuario", nullable = false)
	private Integer idUsuario;

	@Column(nullable = false, length = 4000)
	private String contenido;

	@Column(name = "fecha_envio", nullable = false)
	private Instant fechaEnvio = Instant.now();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversacion_id_conversacion", referencedColumnName = "id_conversacion", nullable = false)
	private Conversacion conversacion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estado_mensaje_id_estado_mensaje", referencedColumnName = "id_estado_mensaje", nullable = false)
	private EstadoMensaje estadoMensaje;
}
