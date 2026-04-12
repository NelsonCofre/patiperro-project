package com.patiperro.notification_service.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "plantilla_correo")
@Data
public class PlantillaCorreo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_plantilla; // [cite: 580, 586, 591]
    
    private String nombreEvento; // [cite: 581, 588, 592]
    private String asunto; // [cite: 582, 589, 593]
    
    @Column(length = 4000)
    private String cuerpo_html; // [cite: 583, 590, 594]
}