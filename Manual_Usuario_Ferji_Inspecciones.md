# 📘 Manual de Usuario — Ferji Inspecciones

**Versión:** 1.0  
**Fecha:** Marzo 2026  
**Plataforma:** Android 8.0+  
**Desarrollado por:** Ferji Ingeniería y Construcción  

---

## 📋 Tabla de Contenidos

1. [Introducción](#1-introducción)
2. [Requisitos del Sistema](#2-requisitos-del-sistema)
3. [Pantalla Inicial](#3-pantalla-inicial)
4. [Inicio de Sesión (Login)](#4-inicio-de-sesión-login)
5. [Pantalla de Bienvenida](#5-pantalla-de-bienvenida)
6. [Menú Principal](#6-menú-principal)
7. [Crear Nueva Inspección](#7-crear-nueva-inspección)
8. [Agregar Habitaciones](#8-agregar-habitaciones)
9. [Finalizar Inspección](#9-finalizar-inspección)
10. [Inspecciones Pendientes (Retomar)](#10-inspecciones-pendientes-retomar)
11. [Enviar Inspección](#11-enviar-inspección)
12. [Maestro de Partidas (Admin)](#12-maestro-de-partidas-admin)
13. [Mantenedor de Precios (Admin)](#13-mantenedor-de-precios-admin)
14. [Configuración de Correos (Admin)](#14-configuración-de-correos-admin)
15. [Preguntas Frecuentes](#15-preguntas-frecuentes)

---

## 1. Introducción

**Ferji Inspecciones** es una aplicación móvil de campo diseñada para inspectores de seguros, peritos de obra e ingenieros civiles. Permite realizar inspecciones de siniestros directamente en terreno, documentando daños con fotografías, generando informes profesionales en PDF y presupuestos de reparación en Excel, y enviándolos automáticamente por correo electrónico.

### ¿Qué puedo hacer con esta aplicación?

- ✅ Crear inspecciones de siniestros con todos los datos del caso
- ✅ Registrar múltiples habitaciones con sus daños y fotografías
- ✅ Generar automáticamente un informe PDF profesional
- ✅ Generar un presupuesto de reparación en formato Excel
- ✅ Enviar los documentos por email de forma automática (sin abrir otra app)
- ✅ Retomar inspecciones que quedaron incompletas
- ✅ Reenviar inspecciones ya completadas
- ✅ Gestionar categorías de daños y precios (solo administradores)
- ✅ Configurar destinatarios de correo y reglas de envío (solo administradores)

### Roles de Usuario

| Rol | Descripción |
|---|---|
| **Inspector** | Puede crear inspecciones, agregar habitaciones, tomar fotos y finalizar |
| **Administrador** | Todo lo del inspector + gestión de partidas, precios y configuración de correos |

---

## 2. Requisitos del Sistema

| Requisito | Detalle |
|---|---|
| Sistema operativo | Android 8.0 (Oreo) o superior |
| Conexión a internet | Necesaria para login, sincronización y envío de emails |
| Cámara | Necesaria para tomar fotografías de los daños |
| Almacenamiento | Permiso requerido en Android 9 o inferior para guardar PDFs |

---

## 3. Pantalla Inicial

Al abrir la aplicación por primera vez (o si no hay sesión activa), verá la pantalla inicial con el logo de Ferji y un botón para ingresar.

> 📸 **[INSERTAR IMAGEN: Captura de la Pantalla Inicial con el logo de Ferji y el botón "Ingresar"]**

### Elementos de la pantalla:

- **Logo Ferji** — Identidad visual de la empresa en la parte superior
- **Nombre de la aplicación** — "FERJI · Inspecciones"
- **Botón "Ingresar"** — Toque para ir a la pantalla de Login

### Acción:
Toque el botón **"Ingresar"** para acceder a la pantalla de inicio de sesión.

---

## 4. Inicio de Sesión (Login)

En esta pantalla ingresará sus credenciales para acceder al sistema.

> 📸 **[INSERTAR IMAGEN: Captura de la Pantalla de Login con los 3 campos vacíos]**

### Campos del formulario:

| Campo | Descripción | Ejemplo |
|---|---|---|
| **RUT** | Su RUT chileno con formato válido | `12.345.678-9` |
| **Nombre Completo** | Su nombre y apellido | `Juan Pérez González` |
| **Correo Electrónico** | Su email de contacto profesional | `juan.perez@empresa.cl` |

### Pasos:

1. Ingrese su **RUT** en el primer campo.
2. Complete su **Nombre Completo**.
3. Ingrese su **Correo Electrónico**.
4. Toque el botón **"Iniciar Sesión"**.

> 📸 **[INSERTAR IMAGEN: Captura de la Pantalla de Login con campos completados]**

### Funcionalidad inteligente — Autocompletado por RUT:

Si su **RUT ya fue registrado** anteriormente en la aplicación:
- Los campos de **Nombre** y **Correo** se completarán automáticamente al ingresar su RUT.
- Si desea actualizar su correo electrónico, simplemente modifíquelo y presione "Iniciar Sesión" — se actualizará en la base de datos.

> 📸 **[INSERTAR IMAGEN: Captura mostrando los campos autocompletados después de ingresar un RUT existente]**

### Validaciones del formulario:

| Validación | Mensaje de error |
|---|---|
| RUT con formato inválido | "RUT chileno inválido" |
| Email con formato incorrecto | "Formato de email inválido" |
| Campos vacíos | "Todos los campos son obligatorios" |

> ⚠️ **Nota:** La primera vez que inicie sesión, la aplicación sincronizará las categorías de daños desde el servidor. Esto puede tomar unos segundos dependiendo de su conexión a internet.

---

## 5. Pantalla de Bienvenida

Después de iniciar sesión exitosamente, verá la pantalla de bienvenida personalizada.

> 📸 **[INSERTAR IMAGEN: Captura de la Pantalla de Bienvenida mostrando nombre del usuario]**

### Elementos de la pantalla:

| Elemento | Descripción |
|---|---|
| **Logo Ferji** | Logo circular en la parte superior |
| **"¡Bienvenido!"** | Mensaje de saludo |
| **Nombre del usuario** | Su nombre aparece destacado en color |
| **Badge "ADMINISTRADOR"** | Solo visible si su cuenta tiene rol de administrador |
| **Botón "Ingresar"** | Accede al Menú Principal |
| **"Cambiar de usuario"** | Cierra la sesión actual y vuelve a la pantalla inicial |

> 📸 **[INSERTAR IMAGEN: Captura de Bienvenida con badge de ADMINISTRADOR visible (si aplica)]**

### Acciones:

- Toque **"Ingresar"** para ir al Menú Principal.
- Toque **"Cambiar de usuario"** si necesita iniciar sesión con otra cuenta.

---

## 6. Menú Principal

El menú principal es el centro de operaciones de la aplicación. Desde aquí accede a todas las funcionalidades disponibles según su rol.

> 📸 **[INSERTAR IMAGEN: Captura del Menú Principal completo — vista de Inspector]**

### Sección: Inspecciones (visible para todos los usuarios)

| Tarjeta | Ícono | Descripción |
|---|---|---|
| **Nueva Inspección** | ➕ | Crear una nueva inspección de siniestro desde cero |
| **Inspecciones Pendientes** | 📋 | Ver y retomar inspecciones que no se han finalizado |
| **Enviar Inspección** | ✉️ | Reenviar el PDF de una inspección ya completada |

### Sección: Administración (solo visible para administradores)

| Tarjeta | Ícono | Descripción |
|---|---|---|
| **Maestro de Partidas** | 🏷️ | Gestionar las categorías y tipos de daño |
| **Mantenedor de Precios** | 💰 | Asignar precios unitarios a las partidas |
| **Configuración de Correos** | ⚙️ | Configurar destinatarios y reglas de envío |

> 📸 **[INSERTAR IMAGEN: Captura del Menú Principal — vista de Administrador mostrando la sección adicional]**

> 💡 **Tip:** También puede acceder a la Configuración de Correos desde el ícono de ⚙️ en la esquina superior derecha de la barra del menú.

---

## 7. Crear Nueva Inspección

Esta pantalla permite registrar los datos generales de un nuevo siniestro a inspeccionar.

> 📸 **[INSERTAR IMAGEN: Captura de la pantalla Nueva Inspección con los campos vacíos]**

### Pasos:

1. En el Menú Principal, toque la tarjeta **"Nueva Inspección"**.
2. Complete el formulario con los datos del siniestro.
3. Toque el botón **"Guardar y Continuar"**.

### Campos del formulario:

#### Sección — Datos del Asegurado:

| Campo | Descripción | Editable | Validación |
|---|---|---|---|
| **RUT** | RUT del cliente o asegurado | ✅ Sí | RUT chileno válido |

#### Sección — Datos del Siniestro:

| Campo | Descripción | Editable | Validación |
|---|---|---|---|
| **Siniestro** | Número del siniestro asignado por la aseguradora | ✅ Sí | Obligatorio |
| **Dirección** | Dirección completa del inmueble a inspeccionar | ✅ Sí | Obligatorio |

#### Sección — Inspector:

| Campo | Descripción | Editable | Validación |
|---|---|---|---|
| **RUT Inspector** | Su RUT de inspector | ❌ No (automático) | Se carga desde su sesión |
| **Mail Inspector** | Su correo electrónico | ❌ No (automático) | Se carga desde su sesión |

> 📸 **[INSERTAR IMAGEN: Captura de Nueva Inspección con todos los campos completados, mostrando que RUT Inspector y Mail son de solo lectura]**

> ℹ️ Los campos **RUT Inspector** y **Mail Inspector** se completan automáticamente con los datos de su sesión y **no se pueden modificar**. Si necesita cambiar su correo, hágalo desde la pantalla de Login.

### Navegación:

- **Flecha ← (Atrás)** — Vuelve al menú sin guardar.
- **"Guardar y Continuar"** — Guarda la inspección y abre la pantalla de Habitaciones.

---

## 8. Agregar Habitaciones

Esta es la pantalla principal de trabajo de campo. Aquí registra los daños de cada habitación o espacio del inmueble inspeccionado. Puede agregar tantas habitaciones como necesite.

> 📸 **[INSERTAR IMAGEN: Captura de la pantalla Agregar Habitación — vista general con formulario vacío]**

---

### 8.1 Nombre de la Habitación

| Campo | Descripción | Ejemplo |
|---|---|---|
| **Nombre** | Identificador del espacio inspeccionado | `Dormitorio Principal`, `Cocina`, `Baño 2`, `Living` |

> 📸 **[INSERTAR IMAGEN: Captura del campo Nombre con un ejemplo escrito]**

---

### 8.2 Medidas (opcional)

| Campo | Descripción | Formato |
|---|---|---|
| **Alto** | Altura de la habitación | En centímetros (ej: `250` = 2.50 m) |
| **Largo** | Largo de la habitación | En centímetros (ej: `400` = 4.00 m) |
| **Ancho** | Ancho de la habitación | En centímetros (ej: `350` = 3.50 m) |

> 📸 **[INSERTAR IMAGEN: Captura de los campos de medidas completados]**

---

### 8.3 Tipo de Daños

Seleccione uno o más tipos de daño del menú desplegable. Las categorías disponibles corresponden a las **partidas variables** configuradas por el administrador en el Maestro de Partidas.

**Pasos:**
1. Toque el menú desplegable **"Seleccionar categoría de daño"**.
2. Aparecerá la lista de categorías disponibles.
3. Toque una categoría para seleccionarla (puede seleccionar varias).
4. Las categorías seleccionadas aparecerán como chips debajo del desplegable.

> 📸 **[INSERTAR IMAGEN: Captura del desplegable de categorías de daño abierto]**

> 📸 **[INSERTAR IMAGEN: Captura mostrando varias categorías seleccionadas como chips]**

**Opción "Otro":**
Si el daño no corresponde a ninguna categoría existente:
1. Active el switch **"Otro"**.
2. Aparecerá un campo de texto libre.
3. Escriba la descripción del daño.

> 📸 **[INSERTAR IMAGEN: Captura mostrando el switch "Otro" activado con texto ingresado]**

---

### 8.4 Comentarios

Campo de texto libre para observaciones adicionales sobre la habitación, el estado de los daños, o cualquier detalle relevante para el informe.

> 📸 **[INSERTAR IMAGEN: Captura del campo de comentarios con texto de ejemplo]**

---

### 8.5 Fotografías

Las fotos son la evidencia visual del daño. Puede tomar múltiples fotografías por habitación.

**Pasos para tomar una foto:**
1. Toque el botón **"Tomar Foto"** 📷.
2. La cámara del dispositivo se abrirá.
3. Encuadre el daño y tome la fotografía.
4. La imagen aparecerá en la galería de la habitación.
5. Repita para tomar más fotos.

> 📸 **[INSERTAR IMAGEN: Captura del botón "Tomar Foto"]**

> 📸 **[INSERTAR IMAGEN: Captura de la galería de fotos con 2-3 imágenes tomadas]**

**Para eliminar una foto:**
- Toque el ícono **✕** en la esquina superior de la imagen que desea eliminar.

> ⚠️ **Permiso de cámara:** La primera vez que use esta función, el sistema le pedirá permiso para acceder a la cámara. Debe aceptar para poder tomar fotos.

---

### 8.6 Botones de Acción

Al final de la pantalla encontrará tres opciones:

| Botón | Acción | Cuándo usarlo |
|---|---|---|
| **"Guardar y Siguiente"** | Guarda la habitación actual y limpia el formulario para agregar otra | Cuando hay más habitaciones por inspeccionar |
| **"Terminar Inspección"** | Guarda la habitación actual (si tiene datos) y finaliza la inspección completa | Cuando ya registró todas las habitaciones |
| **← Atrás** | Vuelve sin guardar la habitación actual | Si desea cancelar |

> 📸 **[INSERTAR IMAGEN: Captura de los botones "Guardar y Siguiente" y "Terminar Inspección"]**

---

## 9. Finalizar Inspección

Cuando presiona **"Terminar Inspección"** desde la pantalla de habitaciones, la aplicación ejecuta automáticamente una secuencia de pasos. Verá una pantalla de progreso durante el proceso.

> 📸 **[INSERTAR IMAGEN: Captura de la pantalla de progreso/loading mientras se finaliza]**

### Secuencia automática:

| Paso | Descripción |
|---|---|
| 1️⃣ | Guarda la última habitación (si tiene datos ingresados) |
| 2️⃣ | Cambia el estado de la inspección de **PENDIENTE** → **COMPLETADA** |
| 3️⃣ | Genera el **informe PDF** con todos los datos, habitaciones y fotos |
| 4️⃣ | Genera el **presupuesto Excel** con partidas y costos de reparación |
| 5️⃣ | Envía los **emails automáticamente** según la configuración establecida |

### Reglas de envío de emails:

| Destinatario | Recibe |
|---|---|
| **Administrador** | Siempre: PDF + Excel |
| **Inspector** | Según configuración: PDF y/o Excel (o nada) |
| **Email en Copia (CC)** | Si está configurado: se incluye en copia |

### Al completarse:
La aplicación regresará automáticamente al **Menú Principal** y mostrará un mensaje de confirmación.

> 📸 **[INSERTAR IMAGEN: Captura del mensaje de confirmación "Informe y presupuesto enviados correctamente"]**

---

## 10. Inspecciones Pendientes (Retomar)

Si una inspección quedó sin finalizar por cualquier motivo (cerró la app, se quedó sin batería, presionó atrás, etc.), puede retomarla exactamente donde la dejó.

> 📸 **[INSERTAR IMAGEN: Captura de la pantalla Inspecciones Pendientes con la lista y estadísticas]**

### Pasos:

1. En el Menú Principal, toque **"Inspecciones Pendientes"**.
2. Verá la lista de todas sus inspecciones con un panel de estadísticas en la parte superior.

### Panel de estadísticas:

| Tarjeta | Descripción |
|---|---|
| **Total** | Cantidad total de inspecciones registradas |
| **Pendientes** (naranja) | Inspecciones sin finalizar |
| **Completadas** (verde) | Inspecciones ya finalizadas |

### Información de cada tarjeta:

| Dato | Descripción |
|---|---|
| Número de inspección | Identificador único (#1, #2, etc.) |
| RUT | RUT del asegurado |
| Siniestro | Número de siniestro |
| Dirección | Dirección del inmueble |
| Inspector | RUT del inspector asignado |
| Email | Correo del inspector |
| Habitaciones | Cantidad de habitaciones registradas hasta el momento |
| Estado | 🟠 PENDIENTE o 🟢 COMPLETADA |
| Fecha | Fecha y hora de creación |

> 📸 **[INSERTAR IMAGEN: Captura de detalle de una tarjeta de inspección PENDIENTE con el botón "Retomar"]**

### Retomar una inspección:

1. Busque la inspección con estado **PENDIENTE**.
2. Toque el botón **"Retomar"** (en naranja) o toque directamente sobre la tarjeta.
3. Se abrirá la pantalla de **Agregar Habitaciones** con el ID de esa inspección.
4. Agregue nuevas habitaciones o presione **"Terminar Inspección"** para finalizar.

> 📸 **[INSERTAR IMAGEN: Captura de una inspección COMPLETADA (sin botón Retomar, en verde)]**

> 💡 Las inspecciones **completadas** aparecen en la lista como referencia, pero **no son clicables** (ya fueron finalizadas y enviadas).

---

## 11. Enviar Inspección

Esta pantalla permite **reenviar** el PDF de una inspección que ya fue completada. Es útil cuando:
- El email original no llegó al destinatario.
- Necesita enviarlo a otra persona.
- Hubo un error de conexión durante el envío original.

> 📸 **[INSERTAR IMAGEN: Captura de la pantalla Enviar Inspección con la lista y barra de búsqueda]**

### Importante:
- Solo aparecen inspecciones con estado **COMPLETADA**.
- En esta pantalla se envía **únicamente el PDF** de inspección (sin el presupuesto Excel).

### Pasos:

1. En el Menú Principal, toque **"Enviar Inspección"**.
2. Use la **barra de búsqueda** en la parte superior para filtrar por:
   - Número de siniestro
   - RUT del asegurado
   - Dirección del inmueble
   - RUT del inspector
3. Localice la inspección deseada.
4. Toque el botón **"Enviar Inspección"** en la tarjeta correspondiente.

> 📸 **[INSERTAR IMAGEN: Captura mostrando el botón "Enviar Inspección" y el estado de envío (cargando)]**

5. Verá un indicador de progreso mientras se genera y envía el PDF.
6. Al finalizar, aparecerá un mensaje de confirmación.

> 📸 **[INSERTAR IMAGEN: Captura del mensaje de éxito "Inspección enviada correctamente"]**

---

## 12. Maestro de Partidas (Admin)

> 🔒 **Esta sección solo es visible para usuarios con rol de Administrador.**

El Maestro de Partidas permite gestionar las **categorías de daños** que los inspectores ven al registrar habitaciones.

> 📸 **[INSERTAR IMAGEN: Captura de la pantalla Maestro de Partidas con la lista de partidas principales]**

### Estructura jerárquica:

```
📂 Partida Principal (ej: "Fisura Pared")
   ├── 📄 Partida Hija 1 (ej: "Reparación fisura < 2mm")
   ├── 📄 Partida Hija 2 (ej: "Reparación fisura > 2mm")
   └── 📄 Partida Hija 3 (ej: "Demolición y reconstrucción")
```

### Naturaleza de las partidas:

| Tipo | Descripción | Ejemplo |
|---|---|---|
| **VARIABLE** | Aparecen en el desplegable de selección de daños al registrar habitaciones | "Fisura Pared", "Humedad Cielo" |
| **FIJA** | Se agregan automáticamente a todos los presupuestos sin selección del inspector | "Gastos Generales", "Utilidad" |

> 📸 **[INSERTAR IMAGEN: Captura mostrando las partidas hijas dentro de una partida principal]**

### Acciones disponibles:

| Acción | Cómo |
|---|---|
| **Agregar Partida Principal** | Botón "+" en la parte inferior |
| **Ver Partidas Hijas** | Toque sobre una partida principal |
| **Agregar Partida Hija** | Dentro del detalle, botón "+" |

> ℹ️ Los cambios se sincronizan automáticamente con Firebase y estarán disponibles para todos los inspectores en su próximo login.

---

## 13. Mantenedor de Precios (Admin)

> 🔒 **Esta sección solo es visible para usuarios con rol de Administrador.**

Permite asignar y modificar **precios unitarios** a las partidas hijas para el cálculo automático del presupuesto de reparación.

> 📸 **[INSERTAR IMAGEN: Captura de la pantalla Mantenedor de Precios con la lista de partidas y precios]**

### Campos por cada partida:

| Campo | Descripción | Ejemplo |
|---|---|---|
| **Descripción** | Nombre de la partida | "Reparación fisura menor" |
| **Unidad** | Unidad de medida | m², ml, U, gl |
| **Precio Unitario** | Costo por unidad | $15.000 |

> 📸 **[INSERTAR IMAGEN: Captura del formulario de edición de precio de una partida]**

### Uso:
Los precios configurados aquí se utilizan automáticamente al generar el **presupuesto Excel** cuando se finaliza una inspección.

---

## 14. Configuración de Correos (Admin)

> 🔒 **Esta sección solo es visible para usuarios con rol de Administrador.**

Desde esta pantalla se configuran los **destinatarios** y las **reglas de envío** de correos electrónicos para todas las inspecciones de la aplicación.

> 📸 **[INSERTAR IMAGEN: Captura completa de la pantalla Configuración de Correos]**

---

### 14.1 Correos de Notificación

| Campo | Descripción | Obligatorio |
|---|---|---|
| **Email Administrador** | Correo principal que **siempre** recibe PDF + Excel | ✅ Sí |
| **Email en Copia (CC)** | Correo adicional incluido en copia en todos los envíos | ❌ No |

> 📸 **[INSERTAR IMAGEN: Captura de la sección Correos de Notificación con campos completados]**

---

### 14.2 Reglas de Envío

| Toggle | Descripción | Efecto cuando está ACTIVADO |
|---|---|---|
| **Enviar Inspección al Inspector** | Control de envío del PDF al inspector | El inspector recibe el PDF de inspección |
| **Enviar Presupuesto al Inspector** | Control de envío del Excel al inspector | El inspector recibe el presupuesto Excel |

> 📸 **[INSERTAR IMAGEN: Captura de la sección Reglas de Envío con los toggles]**

---

### 14.3 Resumen del Flujo de Correos

Al final de la pantalla se muestra un **resumen visual** para verificar la configuración actual.

> 📸 **[INSERTAR IMAGEN: Captura del panel de Resumen del flujo de correos]**

### Tabla de referencia — Flujos de correo según configuración:

| Configuración | Email Admin | Email Inspector |
|---|---|---|
| Inspección ✅ / Presupuesto ❌ | PDF + Excel | Solo PDF |
| Inspección ✅ / Presupuesto ✅ | PDF + Excel | PDF + Excel |
| Inspección ❌ / Presupuesto ❌ | PDF + Excel | No recibe nada |
| Inspección ❌ / Presupuesto ✅ | PDF + Excel | Solo Excel |

### Guardar cambios:
Toque el botón **"Guardar Configuración"** para almacenar los cambios. Los datos se guardan en la nube (Firebase) y aplican inmediatamente a todas las inspecciones futuras.

> 📸 **[INSERTAR IMAGEN: Captura del botón "Guardar Configuración"]**

---

## 15. Preguntas Frecuentes

### ¿Qué pasa si cierro la app sin terminar una inspección?
La inspección queda guardada automáticamente con estado **PENDIENTE** en su dispositivo. Puede retomarla en cualquier momento desde **"Inspecciones Pendientes"** en el menú principal.

### ¿Puedo agregar más habitaciones a una inspección que ya empecé?
Sí. Vaya a **"Inspecciones Pendientes"**, busque la inspección y toque **"Retomar"**. Se abrirá la pantalla de habitaciones para continuar agregando más espacios.

### ¿Puedo reenviar una inspección ya completada?
Sí. Use la opción **"Enviar Inspección"** en el menú principal. Solo se mostrará el listado de inspecciones completadas listas para reenviar.

### ¿Por qué no veo las opciones de Administración en el menú?
Las opciones de **Maestro de Partidas**, **Mantenedor de Precios** y **Configuración de Correos** solo son visibles para usuarios con **rol de Administrador**. Contacte a su administrador si necesita acceso.

### ¿Cómo cambio mi correo electrónico?
Cierre su sesión (desde la pantalla de Bienvenida → "Cambiar de usuario"), luego inicie sesión nuevamente con su RUT. Los datos se autocompletarán, modifique el correo y presione "Iniciar Sesión". El cambio se guardará.

### ¿Necesito conexión a internet para usar la app?

| Función | ¿Requiere internet? |
|---|---|
| Iniciar sesión (primera vez) | ✅ Sí |
| Crear inspecciones y habitaciones | ❌ No (se guardan localmente) |
| Tomar y adjuntar fotografías | ❌ No |
| Finalizar y enviar emails | ✅ Sí |
| Sincronizar partidas maestras | ✅ Sí |
| Guardar configuración de correos | ✅ Sí |

### ¿Dónde se guarda el PDF generado?
El PDF se guarda automáticamente en la carpeta de **Descargas** de su dispositivo. Además, se envía como adjunto en el email.

### ¿Cómo borro todos los datos de la app para empezar de cero?
1. Vaya a **Ajustes** de su teléfono.
2. Seleccione **Aplicaciones**.
3. Busque **Ferji Inspecciones**.
4. Toque **Almacenamiento**.
5. Toque **Borrar datos**.

Esto eliminará toda la información local: inspecciones, sesión, configuraciones, y caché de fotos. Al abrir la app deberá iniciar sesión nuevamente.

### ¿Qué hago si el email no se envía?
Verifique:
1. Que tiene **conexión a internet** activa (WiFi o datos móviles).
2. Que la **Configuración de Correos** tiene un email de administrador válido.
3. Puede reintentar desde **"Enviar Inspección"** en el menú principal.

---

## Glosario

| Término | Definición |
|---|---|
| **Siniestro** | Evento o accidente que genera un reclamo ante la aseguradora |
| **Partida** | Categoría o tipo de trabajo de reparación (ej: pintura, albañilería) |
| **Partida Principal** | Categoría general de daño (ej: "Fisura Pared") |
| **Partida Hija** | Subcategoría o elemento específico de reparación |
| **Partida Variable** | Categoría que el inspector selecciona manualmente al registrar daños |
| **Partida Fija** | Categoría que se incluye automáticamente en todos los presupuestos |
| **RUT** | Rol Único Tributario — número de identificación fiscal chileno |
| **PDF** | Formato de documento portable — el informe de inspección |
| **Excel** | Formato de planilla — el presupuesto de reparación |

---

**© 2026 Ferji Ingeniería y Construcción — Todos los derechos reservados.**

*Ferji Inspecciones v1.0 — Manual de Usuario*

