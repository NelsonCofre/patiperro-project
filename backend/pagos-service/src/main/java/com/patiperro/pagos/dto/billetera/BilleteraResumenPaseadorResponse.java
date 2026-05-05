package com.patiperro.pagos.dto.billetera;

import java.time.LocalDateTime;

public record BilleteraResumenPaseadorResponse(
        BilleteraBucketResponse retenido,
        BilleteraBucketResponse verificacion,
        BilleteraBucketResponse disponible,
        LocalDateTime updatedAt
) {
}
