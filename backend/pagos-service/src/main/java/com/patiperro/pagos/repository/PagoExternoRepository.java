package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.PagoExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoExternoRepository extends JpaRepository<PagoExterno, Long> {

    Optional<PagoExterno> findByProviderAndProviderPaymentId(String provider, String providerPaymentId);

    Optional<PagoExterno> findByProviderAndPreferenceId(String provider, String preferenceId);

    Optional<PagoExterno> findByProviderAndExternalReference(String provider, String externalReference);
}

