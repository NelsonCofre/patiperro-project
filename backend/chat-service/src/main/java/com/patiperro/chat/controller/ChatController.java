package com.patiperro.chat.controller;

import com.patiperro.chat.dto.ConversacionCreateRequest;
import com.patiperro.chat.dto.ConversacionResponseDTO;
import com.patiperro.chat.dto.ConversacionUpdateRequest;
import com.patiperro.chat.dto.EstadoCatalogoDTO;
import com.patiperro.chat.dto.MensajeCreateRequest;
import com.patiperro.chat.dto.MensajeResponseDTO;
import com.patiperro.chat.dto.MensajeUpdateRequest;
import com.patiperro.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@GetMapping("/catalogo/estados-chat")
	public List<EstadoCatalogoDTO> listarEstadosChat() {
		return chatService.listarEstadosChat();
	}

	@GetMapping("/catalogo/estados-mensaje")
	public List<EstadoCatalogoDTO> listarEstadosMensaje() {
		return chatService.listarEstadosMensaje();
	}

	@GetMapping("/conversaciones")
	public List<ConversacionResponseDTO> listarConversaciones(
			@RequestParam(name = "idReserva", required = false) Integer idReserva) {
		if (idReserva != null) {
			return chatService.listarConversacionesPorReserva(idReserva);
		}
		return chatService.listarConversaciones();
	}

	@GetMapping("/conversaciones/{id}")
	public ConversacionResponseDTO obtenerConversacion(@PathVariable Integer id) {
		return chatService.obtenerConversacion(id);
	}

	@PostMapping("/conversaciones")
	public ResponseEntity<ConversacionResponseDTO> crearConversacion(
			@Valid @RequestBody ConversacionCreateRequest request) {
		ConversacionResponseDTO creada = chatService.crearConversacion(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(creada);
	}

	@PutMapping("/conversaciones/{id}")
	public ConversacionResponseDTO actualizarConversacion(
			@PathVariable Integer id,
			@Valid @RequestBody ConversacionUpdateRequest request) {
		return chatService.actualizarConversacion(id, request);
	}

	@DeleteMapping("/conversaciones/{id}")
	public ResponseEntity<Void> eliminarConversacion(@PathVariable Integer id) {
		chatService.eliminarConversacion(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/conversaciones/{id}/mensajes")
	public List<MensajeResponseDTO> listarMensajes(@PathVariable Integer id) {
		return chatService.listarMensajes(id);
	}

	@GetMapping("/conversaciones/{idConversacion}/mensajes/{idMensaje}")
	public MensajeResponseDTO obtenerMensaje(
			@PathVariable Integer idConversacion,
			@PathVariable Integer idMensaje) {
		return chatService.obtenerMensaje(idConversacion, idMensaje);
	}

	@PostMapping("/conversaciones/{id}/mensajes")
	public ResponseEntity<MensajeResponseDTO> enviarMensaje(
			@PathVariable Integer id,
			@Valid @RequestBody MensajeCreateRequest request) {
		MensajeResponseDTO enviado = chatService.enviarMensaje(id, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(enviado);
	}

	@PutMapping("/conversaciones/{idConversacion}/mensajes/{idMensaje}")
	public MensajeResponseDTO actualizarMensaje(
			@PathVariable Integer idConversacion,
			@PathVariable Integer idMensaje,
			@Valid @RequestBody MensajeUpdateRequest request) {
		return chatService.actualizarMensaje(idConversacion, idMensaje, request);
	}

	@DeleteMapping("/conversaciones/{idConversacion}/mensajes/{idMensaje}")
	public ResponseEntity<Void> eliminarMensaje(
			@PathVariable Integer idConversacion,
			@PathVariable Integer idMensaje) {
		chatService.eliminarMensaje(idConversacion, idMensaje);
		return ResponseEntity.noContent().build();
	}
}
