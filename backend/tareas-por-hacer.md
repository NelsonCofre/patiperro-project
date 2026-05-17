Prompt para cursor

- Este documento guarda las 7 historias de usuario con cada tarea por hacer, entonces yo necesito que analises cada historiaa que no se haya repetido nada, y ademas revises los criterios de aceptacion y las tareas por hacer, quiero que revises el proyecto si es que ya hay alguna de estas tareas implementadas, necesito que trabajemos historia por historia, ppara no saturar con contexto
- Regla de revision por historia: antes de integrar con backend, validar y reportar si el frontend de esa historia ya esta implementado (total, parcial o pendiente). El frontend no es parte de mi trabajo, por lo que primero necesito ese estado para decidir la integracion.

Historia de usuario

1- Como Plataforma, quiero descontar automáticamente la comisión antes de acreditar el saldo al paseador

- Criterios
  Criterios de Aceptación (AC)
  Definición de Tasa de Comisión

El sistema debe contar con un parámetro configurable (por ejemplo, un 15% o 20%) que defina el porcentaje que la plataforma retendrá de cada transacción exitosa.

Cálculo Automático en el flujo de Pago Al recibir la confirmación de pago exitoso desde Mercado Pago, el sistema debe calcular inmediatamente el monto neto destinado al paseador utilizando la fórmula:

$$Monto_{Neto} = Monto_{Bruto} - (Monto_{Bruto} \times \text{Tasa}_{Comision})$$

Registro de Transacción Desglosada La base de datos debe registrar tres valores por cada servicio: el monto bruto pagado por el tutor, el monto descontado por la plataforma y el monto neto final que se acreditará al paseador.

Acreditación de Saldo Neto

El valor que se visualice en el "Saldo Retenido" del paseador debe ser siempre el Monto Neto, asegurando que el colaborador vea desde el inicio la cifra real que recibirá tras cumplir el ciclo de verificación.

Transparencia para el Paseador

En el detalle de la billetera digital, el paseador debe poder consultar el desglose de cada pago para entender cuánto se descontó por concepto de comisión de la plataforma.

Integridad Financiera

La suma del saldo acreditado al paseador y la comisión retenida por la plataforma debe coincidir exactamente con el total procesado por la pasarela de pagos, garantizando que no existan fugas de dinero en el sistema.

Tareas por hacer:

- Actualización de la Entidad de Pago (Backend) Modificar la entidad Pago en el microservicio correspondiente para incluir los campos monto_bruto, comision_valor y monto_neto en la base de datos PostgreSQL
- Implementación de Parámetro de Configuración Definir la tasa de comisión en el archivo application.properties (ej: app.comision.porcentaje=0.15) para permitir ajustes rápidos sin necesidad de modificar el código fuente.
- Desarrollo de la Lógica de Cálculo en Service Crear en el PagoService el método calcularLiquidacion que realice la operación aritmética antes de persistir la información en la base de datos de pagos.
- Integración con el Webhook de Mercado Pago Configurar el controlador de pagos para que, al recibir el estado approved desde la API externa, gatille automáticamente el proceso de cálculo de comisión y acreditación de saldo neto.

2- Como Paseador, quiero marcar un paseo como finalizado para iniciar el proceso de cobro

Criterios de Aceptación (AC)
Registro de Término de Servicio

El Paseador debe poder marcar el paseo como finalizado solo si el estado actual es EN_CURSO.

El sistema debe capturar el timestamp exacto del cierre para determinar el día de origen ($N$).

Cálculo de Fecha de Disponibilidad

El sistema debe calcular automáticamente la fecha de liberación como el inicio del día subsiguiente ($N+2$ a las $00:00$ hrs).

Ejemplo: Un paseo terminado a cualquier hora del martes debe quedar programado para liberación el jueves a las $00:00:01$.

Visualización de Saldo "En Tránsito"

El Frontend debe mostrar al paseador en su billetera digital el monto bajo la etiqueta "Saldo en Verificación", indicando explícitamente la fecha y hora en que pasará a estar disponible para retiro.

Procesamiento Automático por Lotes

Al llegar a las $00:00$ hrs de cada día, el sistema debe procesar de forma masiva todos los pagos cuya fecha de liberación coincida con el día actual, moviéndolos al "Saldo Disponible".

Protección ante Disputas (Tutor)

Durante el periodo de retención (día $N$ y $N+1$), el Tutor mantiene la facultad de reportar incidentes.

Si existe un reporte activo, la liberación programada para el día $N+2$ debe suspenderse automáticamente hasta que el soporte técnico resuelva el caso.

Notificación de Fondos Liberados

El sistema debe notificar al paseador cada mañana el monto total que ha pasado a estar disponible de manera consolidada tras el cierre del ciclo de retención.

Tareas (alineadas al diseño implementado en `pagos-service`):

- [x] **Modelo de fecha de liberación:** la fecha efectiva para la regla N+2 **no** se persiste en `transaccion` (esa tabla mezcla cobros, retiros e intentos). La verdad de negocio está en **`billetera_reserva_tracking`** (`fecha_fin_servicio`, fase `EN_VERIFICACION`, `liberado_en`) y en **`BilleteraLiberacionTransaccionalService`** (misma fórmula calendario: día N del fin de servicio + 2 días). Duplicar `fecha_liberacion_estimada` en `transaccion` quedó **descartado** para evitar dos fuentes de verdad.

- [x] **Lógica de fecha N+2 y liberación por lotes:** scheduler `BilleteraLiberacionScheduler` + servicio transaccional de liberación; transición retenido → verificación al **finalizar** paseo vía `reserva-service` → interno `pasar-verificacion`.

- [x] **Proyección de ingresos por fecha de liberación:** el **`GET /api/pagos/paseador/billetera`** incluye el campo JSON **`proyeccionLiberacionesPorDia`**: lista de grupos `{ fechaDisponibleDesde, totalNeto, liberacionPausadaPorDisputa, reservas[] }` para ítems en verificación, misma regla que el scheduler. La UI del paseador puede mostrarlo sin un endpoint aparte.

3- Como Paseador, quiero ver mi "Saldo Retenido" de paseos finalizados

Criterios de aceptacion

Sección de Billetera Detallada El sistema debe disponer de una vista de "Mi Billetera" donde se desglose de forma jerárquica el estado de los fondos. El "Saldo Retenido" debe figurar como la categoría primaria para servicios no concluidos.

Diferenciación de Estados de Saldo Para evitar confusiones con la lógica N+2, el sistema debe separar claramente:

Saldo Retenido: Dinero de paseos en estado PAGADO o EN_CURSO.

Saldo en Verificación: Dinero de paseos FINALIZADOS que están cumpliendo su periodo de espera (Día N y N+1).

Saldo Disponible: Dinero liberado listo para retiro (Día N+2).

Desglose por Reserva Al seleccionar el Saldo Retenido, el paseador debe poder ver una lista de las reservas específicas que componen ese monto, incluyendo el nombre de la mascota y la fecha programada del servicio.

Actualización Síncrona tras el Pago Tan pronto como el Tutor realice el pago exitoso vía Mercado Pago y la reserva cambie a estado PAGADA, el monto correspondiente (menos la comisión) debe verse reflejado instantáneamente en el Saldo Retenido del paseador.

Mensajería Informativa de Seguridad La interfaz debe incluir un texto aclaratorio que indique: "Este saldo corresponde a servicios pagados que aún no han finalizado. Una vez que marques el paseo como terminado, este monto iniciará su proceso de liberación".

Gestión de Reembolsos por Rechazo Si una reserva que aportaba al Saldo Retenido es rechazada por el paseador, el monto debe desaparecer de su vista inmediatamente, reflejando el proceso de devolución automática al tutor realizado por la plataforma.

Tareas:

- Desarrollo de Query de Agregación en el Microservicio de Pagos Implementar en la base de datos pagos_db una consulta SQL que sume los montos de todas las transacciones vinculadas al id_paseador
- Exposición de Endpoint de Resumen de Billetera Desarrollar un endpoint de tipo GET en el controlador de pagos que devuelva un objeto JSON estructurado con los tres estados de saldo (Retenido, Verificación, Disponible) para el usuario autenticado
- Lógica de Transición de Saldo al Finalizar Paseo Programar el disparador en el backend para que, al cambiar el estado de la reserva a FINALIZADO, el monto se descuente automáticamente del "Saldo Retenido" y se sume al "Saldo en Verificación"

4- Como Paseador, quiero ver mi "Saldo Disponible" de paseos finalizados.

Criterios de Aceptación (AC)
Identificación del Saldo Líquido El sistema debe mostrar de forma prominente en la billetera el "Saldo Disponible". Este monto debe representar exclusivamente el dinero que ya ha cumplido el ciclo de retención (Día N+2) y que no tiene ninguna disputa activa.

Actualización Automática al Inicio del Día El Saldo Disponible debe actualizarse de forma automática e inmediata a las 00:00:01 del día programado para su liberación. El usuario no debe realizar ninguna acción manual para que el dinero pase de "En Verificación" a "Disponible".

Historial de Fondos Liberados El sistema debe permitir ver un historial o detalle de qué paseos específicos conforman el saldo disponible actual, permitiendo al paseador realizar una conciliación rápida entre sus servicios realizados y el dinero recibido.

Botón de Acción "Retirar Fondos" Solo cuando el Saldo Disponible sea superior a cero ($ > 0$), el sistema debe habilitar el botón de "Retirar". Si el saldo es cero, el botón debe permanecer deshabilitado o mostrar un estado inactivo.

Sincronización con el Estado de Cuenta Cualquier movimiento que convierta Saldo Disponible en una solicitud de retiro (Transferencia Bancaria) debe descontarse en tiempo real del saldo mostrado, evitando la posibilidad de realizar retiros duplicados.

Transparencia de Comisiones El saldo mostrado como disponible debe ser siempre el valor neto (monto pagado por el tutor menos la comisión de la plataforma), asegurando que el paseador vea exactamente lo que recibirá en su cuenta bancaria.

Tareas:

- [x] **Consulta de saldo disponible:** `BilleteraService.resumenParaPaseador` y buckets en `BilleteraPaseadorController`.
- [x] **Endpoint de retiro:** `POST /api/pagos/paseador/billetera/retiros` con validación de monto ≤ saldo y lógica en `RetiroPaseadorService`.
- **[Decisión de alcance]** **Sin segundo factor adicional:** no se pide contraseña ni OTP en el retiro; la operación se protege con **JWT vigente + rol PASEADOR** (`@PreAuthorize` en el controlador). La tarea original de “doble factor” del enunciado **no** se implementa en ese sentido; se considera cubierta la parte de “token JWT vigente” del propio criterio de la historia 5 (seguridad de la transacción).

5- Como Paseador, quiero solicitar el retiro de mis ganancias a mi cuenta bancaria (Mas que nada simular un retiro)

Esta funcionalidad representa el cierre del flujo de valor para el paseador en Patiperro, permitiendo que el saldo acumulado y liberado tras el ciclo de verificación se convierta en dinero real en su cuenta bancaria personal. Al solicitar el retiro, el sistema debe garantizar la integridad financiera y la trazabilidad de la operación.

A continuación, presento los criterios de aceptación y el desglose de tareas técnicas:

Historia de Usuario: Solicitud de Retiro de Ganancias
Como Paseador,

Quiero solicitar el retiro de mis ganancias a mi cuenta bancaria,

Para disponer efectivamente del dinero obtenido por mis servicios realizados en la plataforma.

Criterios de Aceptación (AC)
Requisito de Saldo Disponible El sistema debe permitir la solicitud de retiro únicamente si el "Saldo Disponible" (fondos que ya cumplieron el ciclo N+2) es mayor a cero. No se podrá solicitar el retiro de saldos que aún se encuentren en estado "Retenido" o "En Verificación".

Configuración de Datos Bancarios Para procesar el retiro, el paseador debe haber registrado previamente su información bancaria (Banco, Tipo de Cuenta y Número de Cuenta) en su perfil. El sistema debe validar que estos campos no estén vacíos al momento de la solicitud.

Validación del Monto a Retirar El usuario debe ingresar el monto que desea transferir. El sistema debe validar que este monto sea mayor al mínimo de retiro establecido por la plataforma y menor o igual al Saldo Disponible actual.

Confirmación y Deducción de Saldo Al confirmar la solicitud, el monto solicitado debe descontarse inmediatamente del "Saldo Disponible" y registrarse como "Retiro en Proceso". Esto evita que el usuario intente retirar el mismo dinero varias veces antes de que se procese la transferencia.

Registro y Comprobante de Solicitud El sistema debe generar un número de operación único para la solicitud y mostrar un mensaje de éxito: "Su solicitud de retiro ha sido recibida y será procesada en las próximas 48 horas hábiles".

Seguridad de la Transacción Cualquier solicitud de retiro debe ser validada mediante el token de autenticación JWT vigente, asegurando que solo el titular de la cuenta pueda movilizar sus fondos.

Tareas:

- Creación del Modelo de Datos para Retiros Definir e implementar la tabla retiro_fondos en el microservicio pagos_db, incluyendo campos para el id_paseador, monto, id_cuenta_bancaria, estado_operacion (PENDIENTE/COMPLETADO/RECHAZADO) y fecha_solicitud
- Implementación de Lógica de Validación de Saldo (Backend) Desarrollar en el BilleteraService una función que verifique la disponibilidad de fondos en tiempo real antes de insertar la solicitud de retiro en la base de datos.
- Exposición del Endpoint de Solicitud Crear en el controlador de pagos un endpoint POST /api/billetera/retiros que reciba el objeto de solicitud y gatille el proceso de descuento de saldo y registro de la operación.

6- Como Tutor, quiero pagar mi reserva de forma segura mediante una pasarela de pagos (Esto es lo que hemos trabajado ultimamente y ya debe de estar casi listo)

Criterios de aceptacion

Inicio del Flujo de Pago

Una vez que el Tutor solicita una reserva, el sistema debe habilitar una opción de "Pagar Reserva" en el detalle de la misma.

El estado de la reserva debe transicionar a PENDIENTE_PAGO hasta que la pasarela confirme la transacción.

Integración con Mercado Pago (Redirect/Modal)

Al accionar el pago, el sistema debe conectar con la API de Mercado Pago y redirigir al usuario al Checkout Pro o abrir el modal de pago seguro.

El Tutor debe poder utilizar métodos de pago locales (tarjetas de crédito, débito o transferencia vía Webpay).

Mensajería de Saldo Retenido

Durante el proceso de pago y en la confirmación posterior, el Frontend debe mostrar un mensaje explícito: "Su dinero será retenido por Patiperro y solo se liberará al paseador una vez que el viaje finalice satisfactoriamente".

Esta información debe ser visible en el historial de la reserva para otorgar tranquilidad al Tutor.

Confirmación y Cambio de Estado

Tras un pago exitoso, el sistema debe recibir la notificación (IPN/Webhook) y cambiar automáticamente el estado de la reserva a PAGADA.

El Paseador debe recibir una notificación inmediata de que el servicio ya cuenta con el respaldo financiero.

Lógica de Reembolso Automático por Rechazo

Si el Paseador rechaza la solicitud de reserva o si el tiempo de espera para la aceptación expira, el sistema debe gatillar una orden de devolución inmediata a través de la API de Mercado Pago.

El Tutor debe recibir una notificación confirmando que el dinero ha sido devuelto a su medio de pago original.

Manejo de Errores de Transacción

En caso de pago fallido o cancelado por el usuario, el sistema debe retornar al Tutor a la aplicación con un mensaje de error claro y permitirle reintentar el proceso sin perder la reserva solicitada.

Tareas:

- Integrar pasarela de pagos Mercado Pago (Checkout Pro/Modal)
- Configuración de Credenciales y SDK (Backend)
- Creación del Servicio de Preferencias (Backend) Desarrollar la lógica para generar una "Preferencia de Pago" en Mercado Pago, incluyendo el detalle del servicio, el monto (monto_total) y las URLs de retorno (success, failure, pending)..
- Instalar e importar la biblioteca oficial de Mercado Pago en React mediante npm install @mercadopago/sdk-react.
- Registro de Transacciones y Auditoría (DB) Configurar tablas en el microservicio de pagos para registrar cada intento de transacción, incluyendo el ID de Mercado Pago y el estado de la operación para fines de soporte técnico.
- Crear microservicio de pagos y billetera

7- Como Tutor, quiero recibir un comprobante o resumen del pago realizado

Criterios de Aceptación (AC)

Generación Post-Pago Exitoso El sistema debe generar automáticamente el resumen de pago una vez que la pasarela de Mercado Pago confirme que la transacción fue exitosa y la reserva cambie al estado PAGADA.

Contenido Detallado del Resumen El comprobante debe incluir, de forma obligatoria:

Número de orden o ID de transacción único.

Fecha y hora de la operación.

Nombre del paseador y de la mascota.

Detalle del servicio (duración y fecha del paseo).

Desglose del monto total pagado en pesos chilenos (CLP).

Indicador de Estado de Fondos Para alinearse con la lógica de confianza de la plataforma, el resumen debe incluir una nota informativa: "Estado: Pago confirmado. Fondos retenidos en garantía por Patiperro hasta la finalización del servicio".

Notificación vía Correo Electrónico Una copia del resumen de pago debe ser enviada automáticamente al correo electrónico registrado del Tutor en un formato legible (HTML o PDF), sirviendo como respaldo externo a la aplicación.

Acceso Histórico en la App El Tutor debe poder consultar y descargar el comprobante en cualquier momento desde su historial de paseos, incluso después de que el servicio haya finalizado.

Validez Informativa Dado que la integración tributaria con el SII se encuentra fuera del alcance actual del proyecto, el documento debe indicar claramente que es un "Resumen de Transacción" y no constituye una boleta o factura legal.

Tareas:

- Definición del Modelo de Datos de Comprobante Crear una entidad Comprobante en el microservicio de pagos que se relacione con el id_reserva y almacene los datos clave de la transacción de Mercado Pago (Payment ID, Merchant Order, etc.)
- esarrollo del Servicio de Generación de Documentos (Backend) Implementar una lógica en Spring Boot que recolecte información de los microservicios de reservas_db, mascotas_db y tutores_db para armar el objeto consolidado del resumen
