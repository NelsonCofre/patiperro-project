package com.patiperro.pagos.dto.billetera;

import java.math.BigDecimal;
import java.util.List;

public record BilleteraBucketResponse(
        String key,
        String title,
        String helper,
        BigDecimal amount,
        BigDecimal grossAmount,
        BigDecimal commissionAmount,
        List<BilleteraReservaItemResponse> reservas
) {
}
