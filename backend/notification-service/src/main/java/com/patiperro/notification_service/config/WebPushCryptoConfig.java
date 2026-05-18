package com.patiperro.notification_service.config;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

/**
 * Registro idempotente del proveedor JCE para {@code nl.martijndwars:web-push} (firmas VAPID).
 */
@Configuration
public class WebPushCryptoConfig {

    private static final Logger log = LoggerFactory.getLogger(WebPushCryptoConfig.class);

    @PostConstruct
    void registerBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.debug("Proveedor BouncyCastle registrado para Web Push");
        }
    }
}
