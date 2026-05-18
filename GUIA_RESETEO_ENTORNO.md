# Guia de reseteo de entorno (Patiperro)

Esta guia resume que configurar manualmente cuando se reinicia todo:

- microservicios
- frontend
- tuneles (ngrok y/o Cloudflare)
- variables de entorno

---

## 1) Levantar backend

Orden sugerido de arranque:

1. `api-gateway` (puerto `8080`)
2. `tutores-service` (`8081`)
3. `paseadores-service` (`8082`)
4. `mascotas-service` (`8083`)
5. `agenda-service` (`8084`)
6. `reserva-service` (`8085`)
7. `notification-service` (`8086`) si aplica
8. `pagos-service` (`8087`)
9. `resena-service` (`8088`) si aplica

Nota: si vas justo de tiempo, primero levanta los que afectan flujo tutor/paseador:
`api-gateway`, `agenda-service`, `reserva-service`, `pagos-service`, `paseadores-service`, `tutores-service`.

---

## 2) Levantar tuneles

Necesitas **dos terminales** (o pestañas): una para el backend público (ngrok) y otra para el frontend (Cloudflare Quick Tunnel). Orden habitual: primero backend local + `ngrok`, luego `npm run dev` + `cloudflared`.

### ngrok (exponer `api-gateway`, puerto 8080)

1. **Instalar** [ngrok](https://ngrok.com/download) y tenerlo en el `PATH` (en Windows suele bastar descomprimir y añadir la carpeta, o `winget install ngrok` si lo tienes disponible).
2. **Autenticar** (una vez por máquina; token en el panel de ngrok):

   ```powershell
   ngrok config add-authtoken <TU_TOKEN>
   ```

3. **Levantar el túnel** apuntando al gateway (mismo puerto que en la sección 1):

   ```powershell
   ngrok http 8080
   ```

4. En la consola de ngrok copia la URL **HTTPS** (por ejemplo `https://xxxx.ngrok-free.dev`). Es la base pública del **api-gateway**; **donde pegarla** está detallado en la sección 3 (resumen y `pagos-service`).

Notas:

- En plan gratuito la URL **cambia** al reiniciar ngrok.
- El frontend en dev ya envía `ngrok-skip-browser-warning` vía proxy de Vite cuando el target es ngrok; no hace falta tocar el navegador para eso.

### Cloudflare Tunnel — Quick Tunnel (exponer Vite, puerto 5173)

Sirve para abrir el **frontend** por HTTPS (móvil, retorno de checkout, etc.) con un dominio `*.trycloudflare.com`.

1. **Descargar** [cloudflared para Windows](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/) (`.exe`). Opcional pero recomendable: moverlo a una carpeta que esté en el `PATH` del sistema, o instalar con `winget install --id Cloudflare.cloudflared` para poder llamarlo solo como `cloudflared` desde cualquier directorio.

2. **Comprobar** (solo si `cloudflared` está en el `PATH`):

   ```powershell
   cloudflared --version
   ```

3. **Levanta primero Vite** en otra terminal (puerto por defecto `5173`):

   ```powershell
   cd frontend\patiperro-project
   npm run dev
   ```

4. **Quick tunnel** hacia ese puerto — el comando depende de **cómo** tengas el ejecutable:
   - Si está en el `PATH` (winget, instalador, o carpeta añadida al PATH):

     ```powershell
     cloudflared tunnel --url http://localhost:5173
     ```

   - Si descargaste solo el `.exe` y estás **en la misma carpeta** que el archivo (por ejemplo `...\Downloads\cloudflare-tunnel`), PowerShell **no** ejecuta programas de la carpeta actual por nombre solo: hay que usar la ruta relativa con `.\`:

     ```powershell
     cd <carpeta-donde-esta-cloudflared.exe>
     .\cloudflared.exe tunnel --url http://localhost:5173
     ```

     Sin el `.\`, verás errores del estilo _«El término 'cloudflared' no se reconoce…»_ o _«…'cloudflared.exe' no se reconoce…»_ aunque el archivo exista ahí; la sugerencia del propio PowerShell es usar `.\cloudflared.exe`.

5. En la salida busca la línea con `https://....trycloudflare.com` y cópiala. **Donde pegarla** (CORS del gateway, front público, checkout) está en la sección 3.

Notas:

- Quick Tunnel **no requiere** cuenta ni zona en Cloudflare; el subdominio es **aleatorio** y cambia al reiniciar `cloudflared`.
- Si usas otro puerto de Vite (`npm run dev -- --port 3000`), cambia la URL del paso 4 acorde.

### Resumen

| Túnel       | Comando típico                                                                                                                    | Expone          |
| ----------- | --------------------------------------------------------------------------------------------------------------------------------- | --------------- |
| ngrok       | `ngrok http 8080`                                                                                                                 | API Gateway     |
| cloudflared | `cloudflared tunnel --url http://127.0.0.1:5173` o `.\cloudflared.exe tunnel --url http://127.0.0.1:5173` (exe en carpeta actual) | Frontend (Vite) |

Guarda las URLs nuevas cada vez que reinicies los túneles.

Importante:

- en plan free de ngrok o con quick tunnel de Cloudflare, las URLs cambian al reiniciar.
- si cambian, debes actualizar variables (ver seccion 3).

---

## 3) Variables a actualizar manualmente

### Donde pegar la URL de **ngrok** (HTTPS → api-gateway :8080)

La URL de ngrok es la **base pública del gateway** (sin path). **No** va en `backend/api-gateway/.env.local` para CORS (eso es Cloudflare). Pégala aquí:

| Archivo                                                               | Clave / propiedad                                                                               | Valor (ejemplo)                                                                                                                 |
| --------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| `frontend/patiperro-project/.env.local`                               | `VITE_GATEWAY_PROXY_TARGET`                                                                     | `https://xxxx.ngrok-free.dev` (sin `/` final). Con `VITE_API_BASE_URL=proxy`, Vite proxifica `/api` hacia el gateway por ngrok. |
| `backend/pagos-service/src/main/resources/application-dev.properties` | `patiperro.pagos.checkout.notification-url` y `patiperro.mercadopago.checkout.notification-url` | `https://xxxx.ngrok-free.dev/api/pagos/webhooks/mercadopago` (mismo texto en las dos líneas; ejemplo copiable abajo).           |

**Webhook de Mercado Pago — `notification-url` (solo en `pagos-service`, no en el gateway ni en el front)**

Mercado Pago necesita una URL **pública HTTPS** que llegue a tu **api-gateway** y que este reenvíe a **pagos-service**. Esa URL completa se configura en el microservicio de pagos, en este archivo:

`backend/pagos-service/src/main/resources/application-dev.properties`

Ahí van **dos propiedades** (mismo valor en ambas): la base es tu ngrok y el path es el del gateway hacia el webhook de pagos:

```properties
# ngrok → api-gateway :8080 (MP IPN / webhook).
patiperro.pagos.checkout.notification-url=https://xxxx.ngrok-free.dev/api/pagos/webhooks/mercadopago
patiperro.mercadopago.checkout.notification-url=https://xxxx.ngrok-free.dev/api/pagos/webhooks/mercadopago
```

- **No** existen claves `notification-url` en `backend/api-gateway/.env.local` ni en `frontend/.../.env.local` para esto.
- Tras editar, reinicia **pagos-service**.

**Alternativa** sin tocar ese `.properties`: al arrancar **pagos-service**, define en el entorno del proceso la URL completa en las variables `CHECKOUT_NOTIFICATION_PUBLIC_URL` y `MERCADOPAGO_CHECKOUT_NOTIFICATION_URL` (mapeo en `backend/pagos-service/src/main/resources/application.properties`).

### Donde pegar la URL de **Cloudflare** (`*.trycloudflare.com` → Vite :5173)

| Archivo                                                               | Clave / propiedad                                      | Valor (ejemplo)                                                                                                         |
| --------------------------------------------------------------------- | ------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------- |
| `frontend/patiperro-project/.env.local`                               | `VITE_PUBLIC_APP_URL`                                  | `https://yyyy.trycloudflare.com` — origen público del front (enlaces, retornos, etc.).                                  |
| `backend/api-gateway/.env.local`                                      | `GATEWAY_EXTRA_CORS_ORIGINS`                           | `https://yyyy.trycloudflare.com` — el navegador abre el front por ese dominio y el API debe aceptar ese origen en CORS. |
| `backend/pagos-service/src/main/resources/application-dev.properties` | `patiperro.mercadopago.checkout.public-front-base-url` | Misma URL Cloudflare — Checkout Pro arma `back_urls` hacia el React por HTTPS (`MercadoPagoCheckoutService`).           |

Alternativa: variable de entorno `CHECKOUT_PUBLIC_FRONT_BASE_URL` al arrancar pagos (ver `application.properties`).

---

### Frontend (`frontend/patiperro-project/.env.local`)

Mantener recomendado:

- `VITE_API_BASE_URL=proxy`

Actualizar cuando cambien los tuneles (ver tablas de arriba):

- **ngrok:** `VITE_GATEWAY_PROXY_TARGET=https://<tu-ngrok>.ngrok-free.dev` (u omite si el gateway sigue en `http://127.0.0.1:8080`).
- **Cloudflare:** `VITE_PUBLIC_APP_URL=https://<tu-subdominio>.trycloudflare.com`

Si existe alguna otra `VITE_*` con URL absoluta de tunel, actualizarla tambien.

Luego reinicia Vite.

### api-gateway (`backend/api-gateway/.env.local`)

Puedes dejar el valor en un archivo **local** (como en el front), sin ejecutar comandos en PowerShell cada vez.

1. En la carpeta del módulo **`backend/api-gateway/`** crea el archivo **`.env.local`** (no va al repo: está en `.gitignore`).
2. Copia de plantilla si quieres:

   ```text
   backend/api-gateway/.env.local.example   →   backend/api-gateway/.env.local
   ```

3. Dentro de `.env.local` usa **formato `.properties`** (una variable por línea, **sin** comillas):

   ```properties
   GATEWAY_EXTRA_CORS_ORIGINS=https://<nuevo-subdominio-cloudflare>.trycloudflare.com
   ```

   Debe coincidir con el origen HTTPS desde el que sirves el frontend (p. ej. la misma base que `VITE_PUBLIC_APP_URL` en el front). Así el gateway añade ese origen a CORS (`extra-allowed-origins-csv` en `application.yml`).

4. **Directorio de trabajo:** Spring busca `.env.local` respecto al directorio desde el que arranca el JVM. Con el IDE suele ser `backend/api-gateway`; si arrancas a mano, haz antes `cd backend\api-gateway` y ejecuta Maven/Java desde ahí.

5. Tras editar `.env.local`, **reinicia** el `api-gateway`.

**Alternativas** (si no usas archivo o quieres sobrescribir un valor puntual): variable de entorno `GATEWAY_EXTRA_CORS_ORIGINS` en el proceso; en PowerShell: `$env:GATEWAY_EXTRA_CORS_ORIGINS = "https://..."` en la misma sesión donde arrancas el gateway; o variables de entorno del Run en el IDE. Si existen **tanto** archivo como variable de entorno con el mismo nombre, suele **prevalecer** la variable de entorno del proceso.

### pagos-service (dev, tunel Mercado Pago)

Si usas perfil dev con `application-dev.properties`, al rotar URLs actualiza las **tres** líneas del bloque _Túneles dev_: una Cloudflare (`public-front-base-url`) y dos ngrok (`notification-url` duplicadas). Reinicia **pagos-service** después de guardar.

---

## 4) Pruebas minimas (2-5 minutos)

1. Login tutor (`200`).
2. Login paseador (`200`).
3. Tutor crea reserva (`POST /api/reserva` sin `403`).
4. Tutor completa pago y retorno.
5. Tutor ve reserva pagada y comprobante.
6. Paseador ve solicitud pagada en "por responder" (con opcion aceptar/rechazar).
7. Paseador crea bloque de agenda sin `403`.

---

## 5) Diagnostico rapido de fallas

### Error 403 en API

- Reinicia `api-gateway` + microservicio de destino.
- Verifica que usas sesion/token nuevo (logout/login).

### Error CORS

- Revisar `GATEWAY_EXTRA_CORS_ORIGINS` con el dominio actual de Cloudflare.
- Verificar que frontend usa `VITE_API_BASE_URL=proxy`.

### Flujo de pago inconsistente

- Validar retorno de Mercado Pago.
- Revisar "Mis reservas" y refrescar.
- Confirmar sincronizacion webhook/sincronizar-pago.

### Agenda paseador no deja crear bloque

- Verificar login como paseador.
- Confirmar `patiperro_paseador_id` en session storage.
- Revisar `POST /api/agenda/bloques` en Network (status y response body).

---

## 6) Comandos utiles (PowerShell)

Verificar puertos escuchando:

```powershell
netstat -ano | findstr ":8080 :8081 :8082 :8083 :8084 :8085 :8086 :8087 :8088"
```

Si un proceso quedo colgado:

```powershell
taskkill /PID <pid> /F
```

---

## 7) Recomendacion para reducir mantenimiento manual

- Mantener frontend por proxy (`VITE_API_BASE_URL=proxy`).
- Al rotar **ngrok**: `VITE_GATEWAY_PROXY_TARGET` en el front + las dos `notification-url` en `pagos-service` (misma base + `/api/pagos/webhooks/mercadopago`).
- Al rotar **Cloudflare**: `VITE_PUBLIC_APP_URL` en el front + `GATEWAY_EXTRA_CORS_ORIGINS` en `backend/api-gateway/.env.local` + en `pagos-service` la clave `patiperro.mercadopago.checkout.public-front-base-url` (Cloudflare → Vite).

Si no usas túneles, el gateway local y `pagos` con `localhost` suelen bastar sin tocar lo anterior.
