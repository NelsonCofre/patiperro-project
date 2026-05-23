Historias de usuario con sus criterios de aceptacion y tareas por hacer

1. Como paseador quiero subir documentos para validar mi cuenta y generar confianza con el tutor

Criterios de Aceptación (AC)
AC 1: Carga de archivos múltiples

Dado que el paseador está en su perfil en la sección de "Verificación".

Cuando selecciona los archivos (Cédula de Identidad frontal y reverso).

Entonces el sistema debe permitir subir archivos en formato imagen (JPG, PNG) o PDF, con un peso máximo de 5MB por archivo.

AC 2: Previsualización y validación visual

Dado que el usuario ha seleccionado un archivo de imagen.

Cuando el archivo se carga en el navegador.

Entonces el sistema debe mostrar una miniatura (thumbnail) del documento antes de confirmar el envío.

AC 3: Estado de verificación

Dado que los documentos han sido subidos exitosamente.

Entonces el perfil del paseador debe mostrar un estado de "Verificación en Proceso" y bloquear nuevas subidas hasta que el administrador responda.

AC 4: Seguridad y Privacidad

Dado que los documentos contienen información sensible.

Entonces los archivos no deben ser accesibles públicamente mediante una URL directa; deben requerir un token de acceso o estar en un bucket privado.

Tareas por hacer:

- Validación de archivos: Implementar lógica en el cliente para validar el tipo de MIME (image/jpeg, application/pdf) y el tamaño del archivo antes de la subida.


2. Como Paseador, quiero enviar fotos del perro durante el paseo para dar tranquilidad al dueño.

Criterios de Aceptación
Escenario: Envío exitoso de fotografía en el chat

Dado que el Paseador tiene un paseo "En Curso" y se encuentra en la vista de chat con el Tutor.

Cuando selecciona el ícono de cámara o galería, elige una foto del perro y presiona "Enviar".

Entonces el sistema debe cargar el archivo al servidor de almacenamiento, generar una URL segura y enviar el mensaje multimedia por el socket para que el Tutor la vea inmediatamente en la conversación.

Escenario: Previsualización de imagen antes del envío

Dado que el Paseador ha seleccionado una foto desde su dispositivo.

Cuando la imagen se carga en el componente de chat pero aún no se ha presionado "Enviar".

Entonces el frontend debe mostrar una miniatura (thumbnail) de la foto con una opción de "X" para descartarla o un campo para añadir un comentario opcional antes de subirla definitivamente.

Escenario: Validación de peso y formato de imagen

Dado que el Paseador intenta enviar un archivo multimedia.

Cuando el archivo seleccionado supera el límite de peso (ej: 10MB) o no es un formato de imagen válido (ej: un archivo .zip o .pdf).

Entonces el sistema debe bloquear la carga y mostrar una alerta: "Solo puedes enviar imágenes (JPG/PNG) de hasta 10MB para asegurar la rapidez del chat".

Escenario: Visualización y expansión para el Tutor

Dado que el Tutor recibe una foto de su mascota a través del chat.

Cuando hace clic o pulsa sobre la miniatura de la imagen recibida.

Entonces el sistema debe abrir la foto en pantalla completa (Lightbox) permitiendo hacer zoom para ver los detalles y ofrecer una opción de "Descargar" en el dispositivo.

Escenario: Galería de fotos del paseo

Dado que el Paseador ha enviado varias fotos durante el trayecto.

Cuando el servicio finaliza y el Tutor revisa el detalle del paseo.

Entonces el sistema debe agrupar todas las imágenes enviadas en una sección de "Galería del Paseo" dentro del historial, para que el usuario pueda consultarlas sin tener que navegar por todo el historial del chat.

Tareas por hacer:

- Evento de Notificación: Una vez subida, el backend debe emitir un mensaje vía WebSocket indicando que hay un nuevo mensaje de tipo IMAGE con la URL del recurso.

3. Como Tutor, quiero ver si un paseador está verificado antes de contratarlo

Criterios de Aceptación (AC)
AC 1: Indicador visual en la lista

Dado que el tutor visualiza la lista de resultados de búsqueda.

Entonces cada tarjeta de paseador que haya completado su proceso de validación debe mostrar un sello o icono de verificado (ej: check azul o dorado).

AC 2: Detalle en el Perfil

Dado que el tutor entra al perfil detallado de un paseador.

Entonces debe aparecer una etiqueta o mensaje explícito que diga "Identidad Verificada" cerca del nombre o la foto del paseador.

AC 3: Filtrado por Verificados (Opcional pero recomendado)

Dado que el tutor usa los filtros avanzados.

Entonces debe poder seleccionar una opción para mostrar "Solo paseadores verificados".

AC 4: Sincronización de estados

Dado que un administrador aprueba los documentos de un paseador.

Entonces el sello de verificación debe aparecer de forma automática e inmediata en todas las vistas de la plataforma.

Tareas por hacer:

- Lógica de Negocio: Crear el servicio que cambie este estado a true cuando el flujo de aprobación de documentos finalice con éxito.

