package com.patiperro.agenda.controller;

import com.patiperro.agenda.dto.WalkerBlackoutRequestDTO;
import com.patiperro.agenda.service.WalkerBlackoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/walker")
@RequiredArgsConstructor
public class WalkerBlackoutController {

    private final WalkerBlackoutService walkerBlackoutService;

    @PostMapping("/blackout")
    public ResponseEntity<?> blackout(@Valid @RequestBody WalkerBlackoutRequestDTO body) {
        return ResponseEntity.ok(walkerBlackoutService.registrarBlackoutConChequeoReservas(body));
    }
}