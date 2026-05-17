# Resumen del Backend de Patiperro

## Vista general

El backend de Patiperro está organizado como una arquitectura de microservicios. Considerando el `api-gateway` como parte de la capa backend expuesta al frontend, hoy existen 9 servicios principales:

1. `api-gateway`
2. `tutores-service`
3. `paseadores-service`
4. `mascotas-service`
5. `agenda-service`
6. `reserva-service`
7. `pagos-service`
8. `notification-service`
9. `resena-service`

La entrada pública del sistema pasa normalmente por el gateway, mientras que la comunicación entre microservicios se hace principalmente por HTTP síncrono usando `RestClient` y, en algunos casos, `FeignClient`. Para rutas internas se usa con frecuencia la cabecera `X-Patiperro-Interno-Secret`, y para rutas de usuario se reutiliza JWT compartido entre servicios.

## Rol de cada microservicio

### 1. `api-gateway`

Es el punto de entrada público del backend.

Funciones principales:
- Recibe las peticiones del frontend y las enruta al microservicio correspondiente.
- Centraliza la exposición de rutas públicas como agenda, mascotas, paseadores, reservas, pagos, notificaciones y reseñas.
- Aplica reglas de seguridad para no exponer rutas internas `*/interno/**`.

Salidas principales:
- No genera lógica de negocio propia.
- Su salida principal es redirigir correctamente la petición hacia el servicio de destino.

### 2. `tutores-service`

Administra a los tutores, que en este proyecto representan a los dueños de las mascotas.

Funciones principales:
- Registro de tutores.
- Inicio y cierre de sesión.
- Consulta de perfil del tutor.
- Gestión de fotos y foto de perfil.
- Exposición de endpoint interno para obtener correo del tutor.

Salidas principales:
- JWT y cookie `access_token`.
- Datos del tutor autenticado o consultado.
- URLs o recursos asociados a fotos de perfil y galería.

### 3. `paseadores-service`

Administra el dominio de los paseadores.

Funciones principales:
- Registro, login y logout de paseadores.
- Gestión de perfil y foto de perfil.
- Configuración personal del paseador.
- Búsqueda pública de paseadores cercanos.
- Consulta pública de resumen y disponibilidad.
- Endpoint interno para recuperar correo del paseador.

Salidas principales:
- Perfil del paseador.
- Configuración del paseador.
- Listas de paseadores cercanos.
- Resumen público y disponibilidad.

Notas:
- Para búsquedas con disponibilidad real, consulta a `agenda-service`.

### 4. `mascotas-service`

Gestiona el catálogo y ciclo de vida de las mascotas.

Funciones principales:
- Crear, listar, editar y eliminar mascotas.
- Gestionar fotos de mascotas.
- Administrar catálogos de especies, razas y tamaños.
- Exponer información interna resumida para otros servicios.

Salidas principales:
- Listados y detalle de mascotas.
- Fotos y portada de mascota.
- Catálogos de especies, razas y tamaños.
- Respuestas internas con detalle o portada para integración.

### 5. `agenda-service`

Gestiona la agenda del paseador y su disponibilidad horaria.

Funciones principales:
- CRUD de bloques de agenda.
- CRUD de bloqueos por día.
- Catálogo de estados de bloque y días de semana.
- Consulta de franjas disponibles para reserva.
- Marcar bloques como reservados o disponibles.
- Validar si un paseador tiene disponibilidad en una franja determinada.

Salidas principales:
- Bloques de agenda.
- Bloqueos personales.
- IDs de paseadores disponibles por rango de fecha y hora.
- Oferta horaria de un paseador para mostrar al tutor.

Notas:
- Antes de ciertos cambios, consulta a `reserva-service` para verificar si existen reservas comprometidas sobre esos bloques.

### 6. `reserva-service`

Es el servicio más central del backend y orquesta gran parte del flujo de negocio.

Funciones principales:
- Crear, listar, actualizar y eliminar reservas.
- Gestionar cambios de estado de la reserva.
- Exponer vistas para tutor y paseador.
- Iniciar el flujo de checkout.
- Manejar el código PIN de encuentro.
- Validar el inicio del paseo.
- Emitir eventos posteriores al inicio del paseo.
- Integrarse con múltiples servicios para enriquecer y validar la información.

Salidas principales:
- Reservas simples.
- Reservas detalladas para tutor.
- Solicitudes pendientes para el paseador.
- Timeline de la reserva.
- Estado del encuentro.
- Código activo de la reserva.
- Resultado de validación de PIN.
- DTOs internos para pagos, comprobantes y billetera.

Notas:
- Es el principal orquestador del backend.
- Se comunica con agenda, tutores, paseadores, mascotas, pagos y notificaciones.
- Además contempla integraciones opcionales con tracking y chat.

### 7. `pagos-service`

Gestiona el flujo financiero del sistema.

Funciones principales:
- Crear preferencias de pago para Mercado Pago.
- Procesar webhooks de Mercado Pago.
- Registrar pagos aprobados o no aprobados.
- Gestionar reembolsos.
- Administrar la billetera del paseador.
- Gestionar retiros.
- Calcular comisión de la plataforma.
- Generar comprobantes y resúmenes de pago.

Salidas principales:
- `preferenceId` y URL de checkout para el frontend.
- Estado de procesamiento de pagos y webhooks.
- Resumen de billetera del paseador.
- Buckets de saldo: `retenido`, `verificacion`, `disponible`.
- Respuesta de solicitud de retiro.
- Comprobante en JSON y HTML.

Notas:
- Consulta a `reserva-service` para validar si una reserva se puede pagar y para enriquecer información financiera.
- También puede notificar a `notification-service` para envío de correos asociados a pagos o comprobantes.

### 8. `notification-service`

Gestiona la mensajería y notificaciones del sistema.

Funciones principales:
- Administración de plantillas de correo.
- Registro de logs de envío.
- Disparo de eventos de notificación.
- Recepción de eventos internos de pagos.
- Recepción de eventos internos de inicio de paseo.
- Integración con Brevo para correo transaccional.

Salidas principales:
- Plantillas registradas.
- Historial de logs de envío.
- Confirmación de procesamiento de notificaciones.
- Efecto externo de negocio: envío o intento de envío de correo.

Notas:
- Sus respuestas HTTP muchas veces representan resultado operativo, por ejemplo `204`, `502` o `503`, más que un DTO funcional complejo.

### 9. `resena-service`

Gestiona las reseñas que los tutores dejan a los paseadores.

Funciones principales:
- Crear reseñas.
- Listar reseñas.
- Listar reseñas por paseador.
- Calcular el promedio de estrellas de un paseador.
- Eliminar reseñas.

Salidas principales:
- Reseñas guardadas.
- Reseñas enriquecidas con datos del tutor.
- Promedio de reputación del paseador.

Notas:
- Consulta a `reserva-service` para asegurarse de que la reserva esté finalizada antes de permitir una reseña.
- Consulta a `tutores-service` para enriquecer el nombre del tutor en la respuesta.

## Cómo se comunican entre sí

La comunicación es principalmente síncrona por HTTP entre microservicios.

Patrones principales observados:
- El frontend entra por `api-gateway`.
- `paseadores-service` consulta a `agenda-service` para disponibilidad real.
- `reserva-service` consulta a:
  - `agenda-service`
  - `tutores-service`
  - `paseadores-service`
  - `mascotas-service`
  - `pagos-service`
  - `notification-service`
- `pagos-service` consulta a `reserva-service` para validar y enriquecer operaciones de pago.
- `pagos-service` informa a `reserva-service` cuando un pago fue aprobado o rechazado.
- `pagos-service` también llama a `notification-service` para avisos financieros.
- `resena-service` consulta a `reserva-service` y `tutores-service`.
- `agenda-service` consulta a `reserva-service` para detectar conflictos sobre bloques ya comprometidos.

## Flujo funcional más importante de la aplicación

Un flujo típico del negocio se entiende así:

1. El tutor entra por el frontend y se autentica.
2. Busca paseadores cercanos en `paseadores-service`.
3. Si filtra por disponibilidad, `paseadores-service` consulta a `agenda-service`.
4. El tutor gestiona sus mascotas en `mascotas-service`.
5. El tutor crea una reserva en `reserva-service`.
6. `reserva-service` valida datos y consulta otros servicios para enriquecer la reserva.
7. Cuando corresponde pagar, `reserva-service` inicia checkout a través de `pagos-service`.
8. `pagos-service` se comunica con Mercado Pago.
9. Después del resultado del pago, `pagos-service` informa a `reserva-service`.
10. `reserva-service` actualiza el estado de la reserva y puede gatillar notificaciones.
11. Cuando el paseo empieza, `reserva-service` valida el código de encuentro y emite eventos en tiempo real.
12. Después de finalizar el paseo, el tutor puede dejar una reseña a través de `resena-service`.

## Principales salidas que entrega la aplicación

Desde el punto de vista funcional, el backend entrega principalmente:

- Autenticación y sesión:
  - JWT
  - cookie `access_token`

- Gestión de usuarios:
  - perfil de tutor
  - perfil de paseador
  - fotos y configuración

- Gestión de mascotas:
  - mascotas del tutor
  - especies, razas, tamaños
  - fotos y portada

- Agenda y disponibilidad:
  - bloques horarios
  - bloqueos
  - oferta del paseador
  - paseadores disponibles

- Reservas:
  - reserva creada
  - listados por tutor y por paseador
  - detalle de reserva
  - timeline
  - estado del encuentro
  - PIN de validación

- Pagos:
  - preferencia de pago
  - URL de checkout
  - billetera del paseador
  - retiros
  - comprobantes
  - estados de pago o reembolso

- Notificaciones:
  - logs de envíos
  - plantillas
  - resultado de disparo de eventos

- Reseñas:
  - comentario
  - calificación
  - promedio reputacional del paseador

## Observaciones técnicas relevantes

- La arquitectura está pensada como un backend por dominios.
- `reserva-service` es el servicio más conectado y el principal coordinador del negocio.
- `pagos-service` es otro punto crítico por su integración con Mercado Pago y por su relación con billetera, reembolsos y comprobantes.
- `notification-service` funciona como servicio transversal para correos y avisos del sistema.
- Existe además comunicación en tiempo real desde `reserva-service` por STOMP al iniciar un paseo.

## Conclusión

Patiperro está modelado como una plataforma de conexión entre tutores y paseadores con foco en:

- descubrimiento de paseadores
- gestión de mascotas
- disponibilidad horaria
- reservas
- pagos
- notificaciones
- reputación posterior al servicio

La estructura actual muestra una separación de responsabilidades bastante clara por dominio, pero con una fuerte dependencia de `reserva-service` como centro del flujo principal de la aplicación.
