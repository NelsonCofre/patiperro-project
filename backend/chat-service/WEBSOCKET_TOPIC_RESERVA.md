# Explicacion tecnica de /topic/reserva.{idReserva}

## Definicion

`/topic/reserva.{idReserva}` es un topico STOMP de difusion asociado a una reserva especifica.

Su objetivo es permitir que los participantes del chat de un paseo reciban mensajes en tiempo real sin mezclar conversaciones de otras reservas.

Ejemplos:

- `/topic/reserva.25`
- `/topic/reserva.40`

## Desglose de la direccion

### `/topic`

Es el prefijo del broker para destinos de salida. Indica que el backend publicara eventos para todos los clientes suscritos a ese canal.

### `reserva`

Es el contexto funcional del chat. En este caso, la conversacion se modela alrededor de una reserva de paseo.

### `{idReserva}`

Es el identificador dinamico de la reserva. No se usa literalmente; se reemplaza por el valor real.

Por ejemplo, si la reserva tiene id `25`, el canal resultante es:

```txt
/topic/reserva.25
```

## Flujo de mensajeria

El flujo esperado para un mensaje de chat es:

1. El frontend envia el mensaje al backend por un destino de aplicacion, por ejemplo `"/app/chat.send"`.
2. El backend recibe el payload del mensaje.
3. El backend valida el contenido y resuelve la reserva asociada.
4. El backend persiste el mensaje en base de datos.
5. El backend publica el mensaje persistido en `"/topic/reserva.{idReserva}"`.
6. Todos los clientes suscritos a ese topico reciben el mensaje de inmediato.

## Diferencia entre entrada y salida

### Entrada al backend

El cliente no envia mensajes directamente al topico. Los envia a un destino de aplicacion:

```txt
/app/chat.send
```

### Salida del broker

El backend, despues de procesar y persistir el mensaje, lo reenvia al topico de la reserva:

```txt
/topic/reserva.25
```

En resumen:

```txt
Frontend -> /app/chat.send -> Backend -> /topic/reserva.{idReserva} -> Frontends suscritos
```

## Ejemplo de suscripcion en frontend

```ts
client.subscribe("/topic/reserva.25", (frame) => {
  const message = JSON.parse(frame.body);
  console.log("Mensaje recibido:", message);
});
```

## Ejemplo de payload enviado por frontend

```json
{
  "sender": "Camila",
  "content": "Voy llegando al punto de encuentro",
  "timestamp": "2026-05-14T18:30:00Z",
  "idReserva": 25
}
```

## Ejemplo de publicacion en backend

```java
messagingTemplate.convertAndSend(
    "/topic/reserva." + chatMessage.getIdReserva(),
    chatMessage
);
```

## Ventajas del diseno

### Aislamiento por reserva

Cada reserva tiene su propio canal. Esto evita que mensajes de un paseo aparezcan en conversaciones ajenas.

### Simplicidad para el frontend

El frontend solo necesita conocer el `idReserva` para suscribirse al canal correcto.

### Alineacion con el dominio

Como el chat existe para coordinar un paseo concreto, usar la reserva como clave del topico mantiene una relacion directa con el negocio.

## Uso esperado en frontend

El frontend debe:

- suscribirse al topico de la reserva activa
- escuchar mensajes nuevos sin recargar la pagina
- actualizar la interfaz de chat en tiempo real

Ejemplo:

```txt
Si el usuario abre el chat de la reserva 40, debe suscribirse a /topic/reserva.40
```

## Uso esperado en backend

El backend debe:

- recibir el mensaje por `@MessageMapping`
- validarlo
- persistirlo
- publicarlo usando `SimpMessagingTemplate`

Esto convierte al backend en el punto de orquestacion entre persistencia y distribucion en tiempo real.

## Resumen final

`/topic/reserva.{idReserva}` es el canal STOMP que representa la conversacion de una reserva especifica. El cliente envia mensajes al backend por `"/app/chat.send"` y el backend los redistribuye por el topico correspondiente despues de procesarlos.

Este diseno permite mensajeria en tiempo real, separacion clara entre conversaciones y una integracion simple entre frontend, broker y persistencia.
