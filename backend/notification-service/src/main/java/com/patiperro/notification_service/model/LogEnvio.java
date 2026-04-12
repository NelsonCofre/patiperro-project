package com.patiperro.notification_service.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_envio")
@Data
public class LogEnvio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_log; // [cite: 584, 598]
    
    private Integer idUsuario; // [cite: 584, 600]
    private LocalDateTime fecha; // [cite: 584, 602]
    private String estado; // [cite: 584, 604]

    @ManyToOne
    @JoinColumn(name = "plantilla_correo_id_plantilla")
    private PlantillaCorreo plantilla; // [cite: 606, 609]
}