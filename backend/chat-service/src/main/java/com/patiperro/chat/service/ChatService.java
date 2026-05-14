package com.patiperro.chat.service;

import com.patiperro.chat.config.ChatEstadosSeedRunner;
import com.patiperro.chat.dto.ChatMessageInbound;
import com.patiperro.chat.dto.ChatMessageOutbound;
import com.patiperro.chat.dto.ChatTypingEvent;
import com.patiperro.chat.dto.ConversacionCreateRequest;
import com.patiperro.chat.dto.ConversacionResponseDTO;
import com.patiperro.chat.dto.ConversacionUpdateRequest;
import com.patiperro.chat.dto.EstadoCatalogoDTO;
import com.patiperro.chat.dto.MensajeCreateRequest;
import com.patiperro.chat.dto.MensajeResponseDTO;
import com.patiperro.chat.dto.MensajeUpdateRequest;
import com.patiperro.chat.model.Conversacion;
import com.patiperro.chat.model.EstadoChat;
import com.patiperro.chat.model.EstadoMensaje;
import com.patiperro.chat.model.Mensaje;
import com.patiperro.chat.repository.ConversacionRepository;
import com.patiperro.chat.repository.EstadoChatRepository;
import com.patiperro.chat.repository.EstadoMensajeRepository;
import com.patiperro.chat.repository.MensajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

	private final ConversacionRepository conversacionRepository;
	private final MensajeRepository mensajeRepository;
	private final EstadoChatRepository estadoChatRepository;
	private final EstadoMensajeRepository estadoMensajeRepository;

	@Transactional(readOnly = true)
	public List<EstadoCatalogoDTO> listarEstadosChat() {
		return estadoChatRepository.findAll().stream()
				.map(this::toEstadoCatalogo)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<EstadoCatalogoDTO> listarEstadosMensaje() {
		return estadoMensajeRepository.findAll().stream()
				.map(this::toEstadoCatalogo)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ConversacionResponseDTO> listarConversaciones() {
		return conversacionRepository.findAllWithEstadoChat().stream()
				.map(this::toConversacionDTO)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ConversacionResponseDTO> listarConversacionesPorReserva(Integer idReserva) {
		return conversacionRepository.findByIdReservaWithEstadoChat(idReserva).stream()
				.map(this::toConversacionDTO)
				.toList();
	}

	@Transactional(readOnly = true)
	public ConversacionResponseDTO obtenerConversacion(Integer id) {
		Conversacion conversacion = conversacionRepository.findByIdWithEstadoChat(id)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Conversación no encontrada: " + id));
		return toConversacionDTO(conversacion);
	}

	@Transactional
	public ConversacionResponseDTO crearConversacion(ConversacionCreateRequest request) {
		EstadoChat estadoChat = estadoChatRepository.findById(request.getIdEstadoChat())
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Estado de chat no encontrado: " + request.getIdEstadoChat()));

		Conversacion conversacion = new Conversacion();
		conversacion.setIdReserva(request.getIdReserva());
		conversacion.setEstadoChat(estadoChat);
		conversacion = conversacionRepository.save(conversacion);

		Conversacion reloaded = conversacionRepository.findByIdWithEstadoChat(conversacion.getId())
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.INTERNAL_SERVER_ERROR,
						"No se pudo recargar la conversación creada."));
		return toConversacionDTO(reloaded);
	}

	@Transactional
	public ConversacionResponseDTO actualizarConversacion(Integer id, ConversacionUpdateRequest request) {
		Conversacion conversacion = conversacionRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Conversación no encontrada: " + id));

		EstadoChat estadoChat = estadoChatRepository.findById(request.getIdEstadoChat())
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Estado de chat no encontrado: " + request.getIdEstadoChat()));

		conversacion.setIdReserva(request.getIdReserva());
		conversacion.setEstadoChat(estadoChat);
		conversacionRepository.save(conversacion);

		return conversacionRepository.findByIdWithEstadoChat(id)
				.map(this::toConversacionDTO)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.INTERNAL_SERVER_ERROR,
						"No se pudo recargar la conversación actualizada."));
	}

	@Transactional
	public void eliminarConversacion(Integer id) {
		if (!conversacionRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada: " + id);
		}
		conversacionRepository.deleteById(id);
	}

	@Transactional(readOnly = true)
	public List<MensajeResponseDTO> listarMensajes(Integer idConversacion) {
		if (!conversacionRepository.existsById(idConversacion)) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Conversación no encontrada: " + idConversacion);
		}

		return mensajeRepository.findByConversacionIdWithCatalogos(idConversacion).stream()
				.map(this::toMensajeDTO)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MensajeResponseDTO> listarMensajesPorReserva(Integer idReserva) {
		if (idReserva == null || idReserva <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idReserva inválido.");
		}

		return conversacionRepository.findFirstByIdReservaOrderByFechaCreacionAsc(idReserva)
				.map(conversacion -> mensajeRepository.findByConversacionIdWithCatalogos(conversacion.getId()).stream()
						.map(this::toMensajeDTO)
						.toList())
				.orElseGet(List::of);
	}

	@Transactional(readOnly = true)
	public MensajeResponseDTO obtenerMensaje(Integer idConversacion, Integer idMensaje) {
		Mensaje mensaje = mensajeRepository.findByIdAndConversacionIdWithCatalogos(idMensaje, idConversacion)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Mensaje no encontrado en la conversación: conversacion=" + idConversacion
								+ ", mensaje=" + idMensaje));
		return toMensajeDTO(mensaje);
	}

	@Transactional
	public MensajeResponseDTO enviarMensaje(Integer idConversacion, MensajeCreateRequest request) {
		Conversacion conversacion = conversacionRepository.findById(idConversacion)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Conversación no encontrada: " + idConversacion));

		EstadoMensaje estadoMensaje = resolverEstadoMensaje(request);

		Mensaje mensaje = new Mensaje();
		mensaje.setConversacion(conversacion);
		mensaje.setIdUsuario(request.getIdUsuario());
		mensaje.setContenido(request.getContenido());
		mensaje.setEstadoMensaje(estadoMensaje);
		mensaje = mensajeRepository.save(mensaje);

		return toMensajeDTO(mensaje);
	}

	@Transactional
	public ChatMessageOutbound enviarMensajeRealtime(ChatMessageInbound request) {
		validarChatRealtime(request);

		Conversacion conversacion = obtenerOCrearConversacionPorReserva(request.getIdReserva());
		EstadoMensaje estadoMensaje = resolverEstadoMensajeEnviado();

		Mensaje mensaje = new Mensaje();
		mensaje.setConversacion(conversacion);
		mensaje.setIdUsuario(request.getIdUsuario());
		mensaje.setContenido(request.getContent().trim());
		mensaje.setFechaEnvio(request.getTimestamp() != null ? request.getTimestamp() : Instant.now());
		mensaje.setEstadoMensaje(estadoMensaje);
		mensaje = mensajeRepository.save(mensaje);

		return toChatMessageOutbound(mensaje, request.getSender().trim());
	}

	@Transactional(readOnly = true)
	public ChatTypingEvent publicarTyping(ChatTypingEvent event) {
		if (event.getIdReserva() == null || event.getIdReserva() <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idReserva inválido.");
		}
		if (event.getSender() == null || event.getSender().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sender es obligatorio.");
		}
		if (event.getIsTyping() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isTyping es obligatorio.");
		}

		event.setSender(event.getSender().trim());
		if (event.getTimestamp() == null) {
			event.setTimestamp(Instant.now());
		}
		return event;
	}

	@Transactional
	public MensajeResponseDTO actualizarMensaje(Integer idConversacion, Integer idMensaje, MensajeUpdateRequest request) {
		Mensaje mensaje = mensajeRepository.findByIdAndConversacionId(idMensaje, idConversacion)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Mensaje no encontrado en la conversación: conversacion=" + idConversacion
								+ ", mensaje=" + idMensaje));

		EstadoMensaje estadoMensaje = estadoMensajeRepository.findById(request.getIdEstadoMensaje())
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Estado de mensaje no encontrado: " + request.getIdEstadoMensaje()));

		mensaje.setIdUsuario(request.getIdUsuario());
		mensaje.setContenido(request.getContenido());
		mensaje.setEstadoMensaje(estadoMensaje);
		mensajeRepository.save(mensaje);

		return mensajeRepository.findByIdAndConversacionIdWithCatalogos(idMensaje, idConversacion)
				.map(this::toMensajeDTO)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.INTERNAL_SERVER_ERROR,
						"No se pudo recargar el mensaje actualizado."));
	}

	@Transactional
	public void eliminarMensaje(Integer idConversacion, Integer idMensaje) {
		Mensaje mensaje = mensajeRepository.findByIdAndConversacionId(idMensaje, idConversacion)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Mensaje no encontrado en la conversación: conversacion=" + idConversacion
								+ ", mensaje=" + idMensaje));
		mensajeRepository.delete(mensaje);
	}

	private void validarChatRealtime(ChatMessageInbound request) {
		if (request.getIdReserva() == null || request.getIdReserva() <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idReserva inválido.");
		}
		if (request.getIdUsuario() == null || request.getIdUsuario() <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idUsuario inválido.");
		}
		if (request.getSender() == null || request.getSender().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sender es obligatorio.");
		}
		if (request.getContent() == null || request.getContent().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content no puede estar vacío.");
		}
	}

	private Conversacion obtenerOCrearConversacionPorReserva(Integer idReserva) {
		return conversacionRepository.findFirstByIdReservaOrderByFechaCreacionAsc(idReserva)
				.orElseGet(() -> {
					EstadoChat estadoChat = estadoChatRepository.findByNombre(ChatEstadosSeedRunner.ESTADO_CHAT_ABIERTA)
							.orElseThrow(() -> new ResponseStatusException(
									HttpStatus.INTERNAL_SERVER_ERROR,
									"Catálogo estado_chat vacío o sin fila ABIERTA."));

					Conversacion conversacion = new Conversacion();
					conversacion.setIdReserva(idReserva);
					conversacion.setEstadoChat(estadoChat);
					return conversacionRepository.save(conversacion);
				});
	}

	private EstadoMensaje resolverEstadoMensaje(MensajeCreateRequest request) {
		if (request.getIdEstadoMensaje() != null) {
			return estadoMensajeRepository.findById(request.getIdEstadoMensaje())
					.orElseThrow(() -> new ResponseStatusException(
							HttpStatus.NOT_FOUND,
							"Estado de mensaje no encontrado: " + request.getIdEstadoMensaje()));
		}

		return resolverEstadoMensajeEnviado();
	}

	private EstadoMensaje resolverEstadoMensajeEnviado() {
		return estadoMensajeRepository.findByNombre(ChatEstadosSeedRunner.ESTADO_MENSAJE_ENVIADO)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.INTERNAL_SERVER_ERROR,
						"Catálogo estado_mensaje vacío o sin fila ENVIADO."));
	}

	private EstadoCatalogoDTO toEstadoCatalogo(EstadoChat estadoChat) {
		return EstadoCatalogoDTO.builder()
				.id(estadoChat.getId())
				.nombre(estadoChat.getNombre())
				.build();
	}

	private EstadoCatalogoDTO toEstadoCatalogo(EstadoMensaje estadoMensaje) {
		return EstadoCatalogoDTO.builder()
				.id(estadoMensaje.getId())
				.nombre(estadoMensaje.getNombre())
				.build();
	}

	private ConversacionResponseDTO toConversacionDTO(Conversacion conversacion) {
		return ConversacionResponseDTO.builder()
				.id(conversacion.getId())
				.idReserva(conversacion.getIdReserva())
				.fechaCreacion(conversacion.getFechaCreacion())
				.idEstadoChat(conversacion.getEstadoChat().getId())
				.nombreEstadoChat(conversacion.getEstadoChat().getNombre())
				.build();
	}

	private MensajeResponseDTO toMensajeDTO(Mensaje mensaje) {
		return MensajeResponseDTO.builder()
				.id(mensaje.getId())
				.idConversacion(mensaje.getConversacion().getId())
				.idUsuario(mensaje.getIdUsuario())
				.contenido(mensaje.getContenido())
				.fechaEnvio(mensaje.getFechaEnvio())
				.idEstadoMensaje(mensaje.getEstadoMensaje().getId())
				.nombreEstadoMensaje(mensaje.getEstadoMensaje().getNombre())
				.build();
	}

	private ChatMessageOutbound toChatMessageOutbound(Mensaje mensaje, String sender) {
		return ChatMessageOutbound.builder()
				.idMensaje(mensaje.getId())
				.idConversacion(mensaje.getConversacion().getId())
				.idReserva(mensaje.getConversacion().getIdReserva())
				.idUsuario(mensaje.getIdUsuario())
				.sender(sender)
				.content(mensaje.getContenido())
				.timestamp(mensaje.getFechaEnvio())
				.estadoMensaje(mensaje.getEstadoMensaje().getNombre())
				.build();
	}
}
