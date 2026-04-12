package com.patiperro.tutores.geo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeocodingProperties.class)
public class GeoConfiguration {}
