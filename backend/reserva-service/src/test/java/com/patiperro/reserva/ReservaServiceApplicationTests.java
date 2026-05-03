package com.patiperro.reserva;

import com.patiperro.reserva.config.JacksonTestSupportConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@Import(JacksonTestSupportConfig.class)
class ReservaServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
