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
- 7: `PENDIENTE_PAGO`
- 8: `PAGADA`
- 9: `EXPIRADA`

> Importante: el backend asume el catálogo completo con ids fijos (`CANCELADA = 6`, `PENDIENTE_PAGO = 7`, `PAGADA = 8`, `EXPIRADA = 9`).

Query sugerida:

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

### 3.1 Puertos HTTP del backend

Mantener esta tabla alineada entre `application.properties`, `api-gateway` y los launchers del IDE:

- `api-gateway`: `8080`
- `tutores-service`: `8081`
- `paseadores-service`: `8082`
- `mascotas-service`: `8083`
- `agenda-service`: `8084`
- `notification-service`: `8086`
- `pagos-service`: `8087`
- `resena-service`: `8088`
- `reserva-service`: `8090`

Si cambias un puerto HTTP, actualiza al mismo tiempo:

- el `application.properties` del servicio
- las rutas del gateway
- cualquier `base-url` o `*_SERVICE_URI` que lo referencie
- los launchers del IDE si llevan flags o entorno asociado

### 3.2 Puertos de tooling y arranque local

No mezclar puertos HTTP del servicio con puertos de tooling como JMX, RMI o debug.

- El error `java.rmi.server.ExportException: Port already in use: 64188` corresponde a un puerto JMX/RMI del launcher de VS Code, no al `server.port` HTTP de `notification-service`.
- Para desarrollo local, `notification-service` arranca con `spring.jmx.enabled=false` y `spring.application.admin.enabled=false` para evitar colisiones intermitentes al lanzar varias sesiones desde el IDE.
- Si el equipo decide volver a habilitar JMX en local, cada servicio debe usar un puerto JMX unico y documentado, distinto de su `server.port`.

### 3.3 Diagnostico rapido cuando falle `notification-service` por puertos

Si vuelve a aparecer un error de tipo `ExportException` o `Address already in use`:

1. Verifica si el puerto mencionado pertenece al `server.port` HTTP o a un flag JVM del launcher.
2. Revisa si ya existe otra instancia Java del mismo servicio corriendo en segundo plano.
3. Si el error menciona un puerto alto como `64188`, sospecha primero de JMX/RMI y revisa `.vscode/launch.json`.
4. Si el error menciona `8086`, entonces si corresponde al puerto HTTP de `notification-service`.
5. Si arrancas por Maven o `java -jar` y el error desaparece, el problema esta en el launcher del IDE y no en el codigo del servicio.

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

