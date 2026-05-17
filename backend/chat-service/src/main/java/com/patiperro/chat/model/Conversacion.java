package com.patiperro.chat.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversacion")
@Getter
@Setter
@NoArgsConstructor
public class Conversacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_conversacion", nullable = false)
	private Integer id;

	@Column(name = "id_reserva", nullable = false)
	private Integer idReserva;

	@Column(name = "fecha_creacion", nullable = false)
	private Instant fechaCreacion = Instant.now();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estado_chat_id_estado", referencedColumnName = "id_estado", nullable = false)
	private EstadoChat estadoChat;

	@OneToMany(mappedBy = "conversacion", cascade = CascadeType.REMOVE)
	private List<Mensaje> mensajes = new ArrayList<>();
}
