package com.patiperro.pagos.dto.disputa;

import java.time.LocalDateTime;

public record DisputaReservaResponse(
        Integer idReserva, boolean disputaActiva, LocalDateTime abiertoEn, LocalDateTime cerradoEn) {}
