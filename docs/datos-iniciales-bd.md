# Datos iniciales de base de datos (seeds)

Catálogos mínimos que el backend espera encontrar en PostgreSQL. **No se cargan automáticamente**: hay que ejecutar cada `INSERT` **manualmente** en pgAdmin, DBeaver o `psql`.

---

## Cuándo ejecutar los seeds

| Paso previo | Descripción |
|-------------|-------------|
| 1 | Crear las 9 bases de datos (`CREATE DATABASE`) — ver [README](../README.md) o [guia-entorno.md](guia-entorno.md) |
| 2 | Levantar los microservicios para que **Hibernate cree las tablas** |
| 3 | **Recién entonces** ejecutar aquí los `INSERT`, uno por bloque, en la BD indicada |

Si corres un seed antes de que exista la tabla, verás un error del tipo `relation "estado_reserva" does not exist`.

**Herramientas:** pgAdmin, DBeaver, consola `psql` o cualquier cliente SQL conectado a la BD correcta.

Si ya existen filas, las queries usan `ON CONFLICT ... DO UPDATE` para no duplicar.

---

## `reservas_db` — tabla `estado_reserva`

Microservicio: `reserva-service`. El backend asume ids fijos (`CANCELADA = 6`, `PENDIENTE_PAGO = 7`, `PAGADA = 8`, `EXPIRADA = 9`).

```sql
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado) VALUES
(1, 'SOLICITADA'),
(2, 'ACEPTADA'),
(3, 'RECHAZADA'),
(4, 'EN CURSO'),
(5, 'FINALIZADA'),
(6, 'CANCELADA'),
(7, 'PENDIENTE_PAGO'),
(8, 'PAGADA'),
(9, 'EXPIRADA')
ON CONFLICT (id_estado_reserva) DO UPDATE
SET nombre_estado = EXCLUDED.nombre_estado;
```

---

## `agenda_db`

Microservicio: `agenda-service`. Tablas: `estado_bloque`, `dia_semana`.

```sql
INSERT INTO estado_bloque (id_estado, nombre) VALUES
(1, 'Disponible'),
(2, 'Reservado')
ON CONFLICT (id_estado) DO UPDATE SET nombre = EXCLUDED.nombre;
```

```sql
INSERT INTO dia_semana (id_dia, nombre) VALUES
(1, 'Lunes'), (2, 'Martes'), (3, 'Miercoles'), (4, 'Jueves'),
(5, 'Viernes'), (6, 'Sabado'), (7, 'Domingo')
ON CONFLICT (id_dia) DO UPDATE SET nombre = EXCLUDED.nombre;
```

---

## `mascotas_db`

Microservicio: `mascotas-service`. Tablas: `tamano`, `especie`, `raza`.

```sql
INSERT INTO tamano (id_tamano, nombre) VALUES
(1, 'Pequeno'), (2, 'Mediano'), (3, 'Grande')
ON CONFLICT (id_tamano) DO UPDATE SET nombre = EXCLUDED.nombre;
```

```sql
INSERT INTO especie (id_especie, nombre) VALUES
(1, 'Perro'), (2, 'Gato')
ON CONFLICT (id_especie) DO UPDATE SET nombre = EXCLUDED.nombre;
```

```sql
INSERT INTO raza (id_raza, nombre, especie_id_especie) VALUES
(1, 'Mestizo', 1), (2, 'Labrador', 1), (3, 'Poodle', 1),
(4, 'Siames', 2), (5, 'Persa', 2)
ON CONFLICT (id_raza) DO UPDATE
SET nombre = EXCLUDED.nombre, especie_id_especie = EXCLUDED.especie_id_especie;
```

---

## `paseadores_db` — tabla `tamano`

Microservicio: `paseadores-service`. Usado para tarifas por tamaño de mascota.

```sql
INSERT INTO tamano (id_tamano, nombre) VALUES
(1, 'Pequeno'), (2, 'Mediano'), (3, 'Grande')
ON CONFLICT (id_tamano) DO UPDATE SET nombre = EXCLUDED.nombre;
```

---

## `notification_db` — tabla `plantilla_correo` (opcional)

Microservicio: `notification-service`.

```sql
INSERT INTO plantilla_correo (id_plantilla, tipo_evento, asunto, cuerpo_html, activo) VALUES
(1, 'RESERVA_ACEPTADA', 'Tu reserva fue aceptada', '<p>Hola {{nombreTutor}}, tu reserva fue aceptada.</p>', true),
(2, 'RESERVA_RECHAZADA', 'Tu reserva fue rechazada', '<p>Hola {{nombreTutor}}, tu reserva fue rechazada.</p>', true),
(3, 'SOLICITUD_PASEO', 'Nueva solicitud de paseo', '<p>Tienes una nueva solicitud de paseo.</p>', true)
ON CONFLICT (id_plantilla) DO UPDATE
SET tipo_evento = EXCLUDED.tipo_evento, asunto = EXCLUDED.asunto,
    cuerpo_html = EXCLUDED.cuerpo_html, activo = EXCLUDED.activo;
```

---

## `pagos_db` — catálogos bancarios (opcional)

Microservicio: `pagos-service`. Datos ficticios para cuenta de abono del paseador.

```sql
INSERT INTO tipo_cuenta (id_tipo_cuenta, nombre) VALUES
(1, 'Cuenta corriente'), (2, 'Cuenta vista'),
(3, 'Cuenta de ahorro'), (4, 'Cuenta RUT')
ON CONFLICT (id_tipo_cuenta) DO UPDATE SET nombre = EXCLUDED.nombre;
```

```sql
INSERT INTO banco (id_banco, nombre) VALUES
(1, 'Banco de Chile'), (2, 'Banco Estado'), (3, 'Banco Santander Chile'),
(4, 'Banco BCI'), (5, 'Scotiabank Chile'), (6, 'Itaú Chile'),
(7, 'Banco Security'), (8, 'Coopeuch')
ON CONFLICT (id_banco) DO UPDATE SET nombre = EXCLUDED.nombre;
```
