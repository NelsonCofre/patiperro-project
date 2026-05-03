package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.PagoExterno;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoExternoRepository extends JpaRepository<PagoExterno, Long> {

    Optional<PagoExterno> findByProviderAndProviderPaymentId(String provider, String providerPaymentId);

    Optional<PagoExterno> findByProviderAndPreferenceId(String provider, String preferenceId);

    Optional<PagoExterno> findByProviderAndExternalReference(String provider, String externalReference);

    Optional<PagoExterno> findByTransaccion_IdTransaccion(Long idTransaccion);

    @Query("SELECT pe FROM PagoExterno pe JOIN pe.transaccion t WHERE pe.provider = :provider "
            + "AND t.estadoPago = :estado "
            + "AND (pe.refundProviderId IS NOT NULL OR pe.refundFecha IS NOT NULL) "
            + "AND pe.notificacionReembolsoCorreoEnviadaEn IS NULL "
            + "AND t.idReserva IS NOT NULL "
            + "ORDER BY pe.idPagoExterno ASC")
    List<PagoExterno> findCorreoReembolsoPendiente(
            @Param("provider") String provider,
            @Param("estado") EstadoPago estado,
            Pageable pageable);
}

