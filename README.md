# Patiperro - Documentacion Operativa y Arquitectura Backend

Este documento resume dos puntos clave del proyecto:

1. Que datos base debes tener en BD antes de usar la aplicacion.
2. Que patron(es) de diseno se usan en el backend y como se reflejan en el codigo.

---

## 1) Datos base obligatorios (seed inicial)

Antes de probar flujos completos (registro, agenda, reservas, mapa, etc.), valida estos catalogos minimos.

### 1.1 `reserva_db`

Tabla: `estado_reserva`

- 1: `SOLICITADA`
- 2: `ACEPTADA`
- 3: `RECHAZADA`
- 4: `EN CURSO`
- 5: `FINALIZADA`
- 6: `CANCELADA`

> Importante: el backend asume `ID_CANCELADA = 6`. No usar `EXPIRADA` en ese id.

Query sugerida:

```sql
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado) VALUES
(1, 'SOLICITADA'),
(2, 'ACEPTADA'),
(3, 'RECHAZADA'),
(4, 'EN CURSO'),
(5, 'FINALIZADA'),
(6, 'CANCELADA')
ON CONFLICT (id_estado_reserva) DO UPDATE
SET nombre_estado = EXCLUDED.nombre_estado;
```

### 1.2 `agenda_db`

Tablas:

- `estado_bloque` (minimo `Disponible` y `Reservado`)
- `dia_semana` (`Lunes` a `Domingo`)

Sin estos catalogos, la creacion/listado de bloques de agenda puede fallar.

Queries sugeridas:

```sql
INSERT INTO estado_bloque (id_estado, nombre) VALUES
(1, 'Disponible'),
(2, 'Reservado')
ON CONFLICT (id_estado) DO UPDATE
SET nombre = EXCLUDED.nombre;
```

```sql
INSERT INTO dia_semana (id_dia, nombre) VALUES
(1, 'Lunes'),
(2, 'Martes'),
(3, 'Miercoles'),
(4, 'Jueves'),
(5, 'Viernes'),
(6, 'Sabado'),
(7, 'Domingo')
ON CONFLICT (id_dia) DO UPDATE
SET nombre = EXCLUDED.nombre;
```

### 1.3 `mascotas_db`

Tablas:

- `tamano`
- `especie`
- `raza` (relacionada a `especie`)

Sin estos registros, el flujo de registro/edicion de mascotas no funciona correctamente.

Queries sugeridas:

```sql
INSERT INTO tamano (id_tamano, nombre) VALUES
(1, 'Pequeno'),
(2, 'Mediano'),
(3, 'Grande')
ON CONFLICT (id_tamano) DO UPDATE
SET nombre = EXCLUDED.nombre;
```

```sql
INSERT INTO especie (id_especie, nombre) VALUES
(1, 'Perro'),
(2, 'Gato')
ON CONFLICT (id_especie) DO UPDATE
SET nombre = EXCLUDED.nombre;
```

```sql
INSERT INTO raza (id_raza, nombre, especie_id_especie) VALUES
(1, 'Mestizo', 1),
(2, 'Labrador', 1),
(3, 'Poodle', 1),
(4, 'Siames', 2),
(5, 'Persa', 2)
ON CONFLICT (id_raza) DO UPDATE
SET nombre = EXCLUDED.nombre,
    especie_id_especie = EXCLUDED.especie_id_especie;
```

### 1.4 `paseadores_db`

Tabla:

- `tamano`

Se usa para validar tarifas por tamano en la configuracion del paseador.

Query sugerida:

```sql
INSERT INTO tamano (id_tamano, nombre) VALUES
(1, 'Pequeno'),
(2, 'Mediano'),
(3, 'Grande')
ON CONFLICT (id_tamano) DO UPDATE
SET nombre = EXCLUDED.nombre;
```

### 1.5 `notification_db` (segun alcance)

Tabla recomendada:

- `plantilla_correo` (si quieres notificaciones por plantilla reales desde el inicio)

`log_envio` se puebla automaticamente con el uso.

Query sugerida (opcional):

```sql
INSERT INTO plantilla_correo (id_plantilla, tipo_evento, asunto, cuerpo_html, activo) VALUES
(1, 'RESERVA_ACEPTADA', 'Tu reserva fue aceptada', '<p>Hola {{nombreTutor}}, tu reserva fue aceptada.</p>', true),
(2, 'RESERVA_RECHAZADA', 'Tu reserva fue rechazada', '<p>Hola {{nombreTutor}}, tu reserva fue rechazada.</p>', true),
(3, 'SOLICITUD_PASEO', 'Nueva solicitud de paseo', '<p>Tienes una nueva solicitud de paseo.</p>', true)
ON CONFLICT (id_plantilla) DO UPDATE
SET tipo_evento = EXCLUDED.tipo_evento,
    asunto = EXCLUDED.asunto,
    cuerpo_html = EXCLUDED.cuerpo_html,
    activo = EXCLUDED.activo;
```

---

## 2) Patrones de diseno usados en backend

La arquitectura backend esta basada en microservicios Spring Boot y combina varios patrones.

## 2.1 Layered Architecture (Arquitectura por capas)

Patron principal del proyecto:

- **Controller**: expone endpoints HTTP (`@RestController`).
- **Service**: concentra reglas de negocio.
- **Repository**: acceso a datos con Spring Data JPA.
- **Model/Entity**: mapeo de tablas.

Ventaja: separa responsabilidades y facilita testing/mantenimiento.

## 2.2 Repository Pattern

Implementado con interfaces `JpaRepository`.

- Encapsula consultas a BD.
- Permite metodos declarativos (`findBy...`) y queries custom.

Ejemplo: repositorios en `reserva-service`, `agenda-service`, `mascotas-service`, etc.

## 2.3 DTO Pattern (Data Transfer Object)

Uso intensivo de DTOs para desacoplar contrato API de entidades internas.

- Request DTOs (`...RequestDTO`)
- Response DTOs (`...ResponseDTO`)
- DTOs de integracion entre microservicios (`dto.integracion`)

Ventaja: evita exponer entidades JPA directamente y estabiliza contratos frontend/backend.

## 2.4 Adapter / Gateway Pattern para integraciones internas

Cada microservicio consumidor encapsula llamadas HTTP a otros servicios en clientes dedicados.

Ejemplos:

- `AgendaIntegracionClient`
- `TutorIntegracionClient`
- `MascotaIntegracionClient`

Estos componentes actuan como adaptadores de infraestructura para no mezclar `RestClient` con logica de negocio.

## 2.5 Event-Driven / Observer (publicacion-suscripcion)

En `reserva-service` se publica evento de dominio al confirmar inicio de paseo (PIN valido), y luego se ejecutan side effects:

- cambio de estado
- notificaciones websocket
- activaciones asociadas

Esto separa el caso de uso principal de efectos secundarios.

## 2.6 Strategy-like rules via estado/decision

Flujos como `ACEPTAR`, `RECHAZAR`, `INICIAR_PASEO`, `FINALIZAR_PASEO` usan logica de decision por estado actual + estado destino en servicio.

No es una estrategia clasica por clase, pero si una estrategia de transicion centralizada por reglas de negocio.

---

## 3) Notas operativas importantes

- La geocodificacion (latitud/longitud) en registro de tutor/paseador es **best-effort**:
  - si Nominatim falla, la direccion puede guardarse sin coordenadas.
- Para mapa de tutor, el paseador debe tener `direccion.latitud` y `direccion.longitud` no nulas.
- En frontend, cuando hay cambios de UI (mockups, formato, etc.), normalmente basta reiniciar/refrescar Vite.
- Cuando hay cambios de contratos DTO o servicios Java, reiniciar el microservicio afectado.

---

## 4) Recomendacion de evolucion

Para reducir incidencias en geolocalizacion:

- aceptar `latitud/longitud` desde frontend en registro,
- mantener geocodificacion backend como fallback,
- y agregar reintento asincrono para direcciones pendientes.

