-- ===== ESPECIE =====
INSERT INTO especie (id_especie, nombre) VALUES
  (1, 'Perro'),
  (2, 'Gato')
ON CONFLICT (id_especie) DO NOTHING;

-- ===== TAMAÑO (catálogo de mascota) =====
INSERT INTO tamano (id_tamano, nombre, descripcion) VALUES
  (1, 'Pequeño', 'Hasta 10 kg'),
  (2, 'Mediano', '10 a 25 kg'),
  (3, 'Grande', 'Más de 25 kg')
ON CONFLICT (id_tamano) DO NOTHING;

-- ===== RAZA (cada raza apunta a una especie) =====
INSERT INTO raza (id_raza, nombre, especie_id_especie) VALUES
  (1, 'Mestizo', 1),
  (2, 'Labrador Retriever', 1),
  (3, 'Golden Retriever', 1),
  (4, 'Bulldog Francés', 1),
  (5, 'Ovejero Alemán', 1),
  (6, 'Mestizo', 2),
  (7, 'Persa', 2),
  (8, 'Siamés', 2),
  (9, 'Maine Coon', 2),
  (10, 'British Shorthair', 2)
ON CONFLICT (id_raza) DO NOTHING;

