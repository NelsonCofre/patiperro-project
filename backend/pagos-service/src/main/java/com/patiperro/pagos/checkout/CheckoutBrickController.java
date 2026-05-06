package com.patiperro.pagos.checkout;

import com.patiperro.pagos.checkout.dto.BrickPagoRequest;
import com.patiperro.pagos.checkout.dto.PagoBrickResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checkout API con Payment Brick (token). Sin preferencia {@code /checkout/preferences}.
 */
@RestController
@RequestMapping("/api/pagos/checkout")
@RequiredArgsConstructor
public class CheckoutBrickController {

    private final CheckoutBrickService checkoutBrickService;

    @PostMapping("/pago-brick")
    public ResponseEntity<PagoBrickResponseDto> pagoBrick(@RequestBody(required = false) BrickPagoRequest body) {
        if (body == null) {
            return ResponseEntity.badRequest().build();
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Long tutorId = extraerTutorId(auth != null ? auth.getPrincipal() : null);
        if (tutorId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(checkoutBrickService.procesar(body, tutorId));
    }

    private static Long extraerTutorId(Object principal) {
        if (principal instanceof Long id) {
            return id;
        }
        if (principal instanceof Number n) {
            return n.longValue();
        }
        if (principal instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
