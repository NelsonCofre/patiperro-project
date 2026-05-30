package com.patiperro.pagos.service;

import com.patiperro.pagos.model.Banco;
import com.patiperro.pagos.model.Billetera;
import com.patiperro.pagos.model.Cuenta;
import com.patiperro.pagos.model.TipoCuenta;
import com.patiperro.pagos.repository.BilleteraRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "patiperro.pagos.retiro.minimo=1000.00"
})
class RetiroPaseadorServiceRollbackTest {

    @Autowired
    private RetiroPaseadorService retiroPaseadorService;

    @Autowired
    private BilleteraRepository billeteraRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private TransaccionRepository transaccionRepository;

    @Test
    void solicitarRetiro_siFallaRegistroTransaccion_noDescuentaSaldo() {
        Long idUsuario = 123L;

        transactionTemplate.executeWithoutResult(status -> {
            persistirBilleteraConCuentaBancaria(idUsuario, new BigDecimal("5000.00"));
        });

        when(transaccionRepository.save(any()))
                .thenThrow(new RuntimeException("fallo forzado al registrar retiro"));

        assertThatThrownBy(() -> retiroPaseadorService.solicitarRetiro(idUsuario, new BigDecimal("1200.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fallo forzado");

        BigDecimal saldoTrasFallo = transactionTemplate.execute(status -> billeteraRepository.findByIdUsuario(idUsuario)
                .orElseThrow()
                .getSaldoActual());

        assertThat(saldoTrasFallo).isEqualByComparingTo("5000.00");
    }

    private void persistirBilleteraConCuentaBancaria(Long idUsuario, BigDecimal saldoActual) {
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
    }
}

