package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.billetera.RetiroPaseadorResponse;
import com.patiperro.pagos.model.Banco;
import com.patiperro.pagos.model.Billetera;
import com.patiperro.pagos.model.Cuenta;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.TipoCuenta;
import com.patiperro.pagos.model.TipoTransaccion;
import com.patiperro.pagos.repository.TransaccionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "patiperro.pagos.retiro.minimo=1000.00"
})
class RetiroPaseadorServiceTest {

    @Autowired
    private RetiroPaseadorService retiroPaseadorService;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void solicitarRetiro_ok_descuentaSaldoYRegistraTransaccionPendiente() {
        Billetera billetera = persistirBilleteraConCuentaBancaria(77L, new BigDecimal("5000.00"));

        RetiroPaseadorResponse resp = retiroPaseadorService.solicitarRetiro(77L, new BigDecimal("1200.00"));

        assertThat(resp).isNotNull();
        assertThat(resp.idTransaccion()).isNotNull();
        assertThat(resp.montoRetirado()).isEqualByComparingTo("1200.00");
        assertThat(resp.saldoDisponibleTrasRetiro()).isEqualByComparingTo("3800.00");
        assertThat(resp.mensaje()).isNotBlank();

        Billetera refrescada = entityManager.find(Billetera.class, billetera.getIdBilletera());
        assertThat(refrescada.getSaldoActual()).isEqualByComparingTo("3800.00");

        var tx = transaccionRepository.findById(resp.idTransaccion()).orElseThrow();
        assertThat(tx.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(tx.getTipoTransaccion()).isEqualTo(TipoTransaccion.RETIRO_PASEADOR);
    }

    @Test
    @Transactional
    void solicitarRetiro_rechazaSinCuentaBancaria() {
        persistirBilleteraSinCuenta(88L, new BigDecimal("5000.00"));

        assertThatThrownBy(() -> retiroPaseadorService.solicitarRetiro(88L, new BigDecimal("1200.00")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @Transactional
    void solicitarRetiro_rechazaPorMinimo() {
        persistirBilleteraConCuentaBancaria(99L, new BigDecimal("5000.00"));

        assertThatThrownBy(() -> retiroPaseadorService.solicitarRetiro(99L, new BigDecimal("999.99")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private Billetera persistirBilleteraConCuentaBancaria(Long idUsuario, BigDecimal saldoActual) {
        Billetera billetera = Billetera.builder()
                .idUsuario(idUsuario)
                .saldoActual(saldoActual)
                .saldoRetenido(BigDecimal.ZERO)
                .saldoVerificacion(BigDecimal.ZERO)
                .build();
        entityManager.persist(billetera);

        Banco banco = Banco.builder().nombre("Banco Test").build();
        entityManager.persist(banco);

        TipoCuenta tipoCuenta = TipoCuenta.builder().nombre("Cuenta Corriente").build();
        entityManager.persist(tipoCuenta);

        Cuenta cuenta = Cuenta.builder()
                .billetera(billetera)
                .banco(banco)
                .tipoCuenta(tipoCuenta)
                .numeroCuenta("12345678")
                .build();
        entityManager.persist(cuenta);

        entityManager.flush();
        return billetera;
    }

    private void persistirBilleteraSinCuenta(Long idUsuario, BigDecimal saldoActual) {
        Billetera billetera = Billetera.builder()
                .idUsuario(idUsuario)
                .saldoActual(saldoActual)
                .saldoRetenido(BigDecimal.ZERO)
                .saldoVerificacion(BigDecimal.ZERO)
                .build();
        entityManager.persist(billetera);
        entityManager.flush();
    }
}

