package com.patiperro.reserva;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest(properties = {
        // Por encima de variables de entorno vacías (p. ej. SPRING_DATASOURCE_URL="").
        "spring.datasource.url=jdbc:postgresql://localhost:5432/reserva_db",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.username=postgres",
        "spring.datasource.password=1234",
        "jwt.secret=test-jwt-secret-key-for-context-load-only-min-chars-2026xxxx",
        "patiperro.reserva.interno.secret=test-reserva-interno-secret"
})
class ReservaServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
