INSERT INTO raza (nombre) VALUES ('Poodle');
INSERT INTO raza (nombre) VALUES ('Pastor Alemán');
INSERT INTO raza (nombre) VALUES ('Golden Retriever');
INSERT INTO raza (nombre) VALUES ('Bulldog');
INSERT INTO raza (nombre) VALUES ('Quiltro (Mezcla)');
INSERT INTO raza (nombre) VALUES ('Labrador');
INSERT INTO raza (nombre) VALUES ('Chihuahua');


-- Insertamos una mascota de prueba asociada al Tutor ID 10 y Raza ID 1 (Poodle)
INSERT INTO mascota (nombre, fecha_nacimiento, peso, tamano, sexo, comportamiento, cuidados_especiales, id_tutor, id_raza) 
VALUES ('Bobby', '2022-05-20', 12.5, 'Mediano', 'Macho', 'Muy juguetón', 'Ninguno', 10, 1);