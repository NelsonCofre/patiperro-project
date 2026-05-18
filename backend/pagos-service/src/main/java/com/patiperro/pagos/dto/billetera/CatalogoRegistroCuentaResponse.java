package com.patiperro.pagos.dto.billetera;

import java.util.List;

public record CatalogoRegistroCuentaResponse(
        List<BancoCatalogoResponse> bancos,
        List<TipoCuentaCatalogoResponse> tiposCuenta) {
}
