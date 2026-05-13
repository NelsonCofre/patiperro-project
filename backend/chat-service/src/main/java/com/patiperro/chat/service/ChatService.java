package com.patiperro.chat.service;

import com.patiperro.chat.config.ChatEstadosSeedRunner;
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
		Conversacion c = conversacionRepository.findByIdWithEstadoChat(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada: " + id));
		return toConversacionDTO(c);
	}

	@Transactional
	public ConversacionResponseDTO crearConversacion(ConversacionCreateRequest request) {
		EstadoChat estadoChat = estadoChatRepository.findById(request.getIdEstadoChat())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Estado de chat no encontrado: " + request.getIdEstadoChat()));
		Conversacion c = new Conversacion();
		c.setIdReserva(request.getIdReserva());
		c.setEstadoChat(estadoChat);
		c = conversacionRepository.save(c);
		c = conversacionRepository.findByIdWithEstadoChat(c.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"No se pudo recargar la conversación creada."));
		return toConversacionDTO(c);
	}

	@Transactional
	public ConversacionResponseDTO actualizarConversacion(Integer id, ConversacionUpdateRequest request) {
		Conversacion c = conversacionRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada: " + id));
		EstadoChat estadoChat = estadoChatRepository.findById(request.getIdEstadoChat())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Estado de chat no encontrado: " + request.getIdEstadoChat()));
		c.setIdReserva(request.getIdReserva());
		c.setEstadoChat(estadoChat);
		conversacionRepository.save(c);
		return conversacionRepository.findByIdWithEstadoChat(id)
				.map(this::toConversacionDTO)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
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
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada: " + idConversacion);
		}
		return mensajeRepository.findByConversacionIdWithCatalogos(idConversacion).stream()
				.map(this::toMensajeDTO)
				.toList();
	}

	@Transactional(readOnly = true)
	public MensajeResponseDTO obtenerMensaje(Integer idConversacion, Integer idMensaje) {
		Mensaje m = mensajeRepository.findByIdAndConversacionIdWithCatalogos(idMensaje, idConversacion)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Mensaje no encontrado en la conversación: conversacion=" + idConversacion + ", mensaje=" + idMensaje));
		return toMensajeDTO(m);
	}

	@Transactional
	public MensajeResponseDTO enviarMensaje(Integer idConversacion, MensajeCreateRequest request) {
		Conversacion c = conversacionRepository.findById(idConversacion)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada: " + idConversacion));
		EstadoMensaje estadoMensaje = resolverEstadoMensaje(request);
		Mensaje m = new Mensaje();
		m.setConversacion(c);
		m.setIdUsuario(request.getIdUsuario());
		m.setContenido(request.getContenido());
		m.setEstadoMensaje(estadoMensaje);
		m = mensajeRepository.save(m);
		return toMensajeDTO(m);
	}

	@Transactional
	public MensajeResponseDTO actualizarMensaje(Integer idConversacion, Integer idMensaje, MensajeUpdateRequest request) {
		Mensaje m = mensajeRepository.findByIdAndConversacionId(idMensaje, idConversacion)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Mensaje no encontrado en la conversación: conversacion=" + idConversacion + ", mensaje=" + idMensaje));
		EstadoMensaje estadoMensaje = estadoMensajeRepository.findById(request.getIdEstadoMensaje())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Estado de mensaje no encontrado: " + request.getIdEstadoMensaje()));
		m.setIdUsuario(request.getIdUsuario());
		m.setContenido(request.getContenido());
		m.setEstadoMensaje(estadoMensaje);
		mensajeRepository.save(m);
		return mensajeRepository.findByIdAndConversacionIdWithCatalogos(idMensaje, idConversacion)
				.map(this::toMensajeDTO)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"No se pudo recargar el mensaje actualizado."));
	}

	@Transactional
	public void eliminarMensaje(Integer idConversacion, Integer idMensaje) {
		Mensaje m = mensajeRepository.findByIdAndConversacionId(idMensaje, idConversacion)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Mensaje no encontrado en la conversación: conversacion=" + idConversacion + ", mensaje=" + idMensaje));
		mensajeRepository.delete(m);
	}

	private EstadoMensaje resolverEstadoMensaje(MensajeCreateRequest request) {
		if (request.getIdEstadoMensaje() != null) {
			return estadoMensajeRepository.findById(request.getIdEstadoMensaje())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
							"Estado de mensaje no encontrado: " + request.getIdEstadoMensaje()));
		}
		return estadoMensajeRepository.findByNombre(ChatEstadosSeedRunner.ESTADO_MENSAJE_ENVIADO)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Catálogo estado_mensaje vacío o sin fila ENVIADO."));
	}

	private EstadoCatalogoDTO toEstadoCatalogo(EstadoChat e) {
		return EstadoCatalogoDTO.builder().id(e.getId()).nombre(e.getNombre()).build();
	}

	private EstadoCatalogoDTO toEstadoCatalogo(EstadoMensaje e) {
		return EstadoCatalogoDTO.builder().id(e.getId()).nombre(e.getNombre()).build();
	}

	private ConversacionResponseDTO toConversacionDTO(Conversacion c) {
		return ConversacionResponseDTO.builder()
				.id(c.getId())
				.idReserva(c.getIdReserva())
				.fechaCreacion(c.getFechaCreacion())
				.idEstadoChat(c.getEstadoChat().getId())
				.nombreEstadoChat(c.getEstadoChat().getNombre())
				.build();
	}

	private MensajeResponseDTO toMensajeDTO(Mensaje m) {
		return MensajeResponseDTO.builder()
				.id(m.getId())
				.idConversacion(m.getConversacion().getId())
				.idUsuario(m.getIdUsuario())
				.contenido(m.getContenido())
				.fechaEnvio(m.getFechaEnvio())
				.idEstadoMensaje(m.getEstadoMensaje().getId())
				.nombreEstadoMensaje(m.getEstadoMensaje().getNombre())
				.build();
	}
}
