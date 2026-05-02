package com.patiperro.pagos.checkout;

import com.patiperro.pagos.checkout.dto.CheckoutProResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos/checkout")
@RequiredArgsConstructor
public class CheckoutProController {

    private final CheckoutProService checkoutProService;

    public record CheckoutProRequest(Long idReserva) {
    }

    @PostMapping("/checkout-pro")
    public ResponseEntity<CheckoutProResponseDto> crearPreferencia(@RequestBody(required = false) CheckoutProRequest body) {
        if (body == null || body.idReserva() == null) {
            return ResponseEntity.badRequest().build();
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long tutorId)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(checkoutProService.iniciar(body.idReserva(), tutorId));
    }
}
