package com.patiperro.reserva.event;

import com.patiperro.reserva.service.PaseoInicioSideEffectsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaseoIniciadoEventListener {

    private final PaseoInicioSideEffectsService paseoInicioSideEffectsService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaseoIniciado(PaseoIniciadoDomainEvent event) {
        paseoInicioSideEffectsService.ejecutar(event);
    }
}
