package com.patiperro.chat.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EstadoCatalogoDTO {
	Integer id;
	String nombre;
}
