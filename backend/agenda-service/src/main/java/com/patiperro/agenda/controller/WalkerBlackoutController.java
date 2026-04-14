package com.patiperro.agenda.controller;

import com.patiperro.agenda.dto.WalkerBlackoutRequestDTO;
import com.patiperro.agenda.service.WalkerBlackoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/walker")
@RequiredArgsConstructor
public class WalkerBlackoutController {

    private final WalkerBlackoutService walkerBlackoutService;

    @PostMapping("/blackout")
    public ResponseEntity<?> blackout(@Valid @RequestBody WalkerBlackoutRequestDTO body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(walkerBlackoutService.registrarBlackoutConChequeoReservas(body));
    }

    @DeleteMapping("/blackout/{id}")
    public ResponseEntity<Void> eliminarBlackout(@PathVariable Integer id) {
        walkerBlackoutService.eliminarBlackout(id);
        return ResponseEntity.noContent().build();
    }
}