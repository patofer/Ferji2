# 📘 Manual de Usuario — Ferji Inspecciones

**Versión:** 1.0  
**Fecha:** Marzo 2026  
**Plataforma:** Android  

---

## 📋 Tabla de Contenidos

1. Introducción
2. Requisitos del Sistema
3. Inicio de Sesión
4. Pantalla de Bienvenida
5. Menú Principal
6. Crear Nueva Inspección
7. Agregar Habitaciones
8. Finalizar Inspección
9. Inspecciones Pendientes (Retomar)
10. Enviar Inspección
11. Maestro de Partidas (Admin)
12. Mantenedor de Precios (Admin)
13. Configuración de Correos (Admin)
14. Preguntas Frecuentes

---

## 1. Introducción

**Ferji Inspecciones** es una aplicación móvil diseñada para inspectores de seguros, peritos de obra e ingenieros civiles. Permite realizar inspecciones de siniestros directamente en terreno, registrando datos del inmueble, fotografías de daños y generando informes profesionales en PDF que se envían automáticamente por correo electrónico.

### ¿Qué puedo hacer con esta aplicación?

- ✅ Crear inspecciones de siniestros con todos los datos del caso
- ✅ Registrar múltiples habitaciones con sus daños y fotografías
- ✅ Generar automáticamente un informe PDF profesional
- ✅ Generar un presupuesto de reparación en Excel
- ✅ Enviar los documentos por email de forma automática
- ✅ Retomar inspecciones que quedaron incompletas
- ✅ Reenviar inspecciones ya completadas
- ✅ Gestionar categorías de daños y precios (administradores)

---

## 2. Requisitos del Sistema

| Requisito | Detalle |
|---|---|
| Sistema operativo | Android 8.0 (Oreo) o superior |
| Conexión a internet | Necesaria para login, sincronización y envío de emails |
| Cámara | Necesaria para tomar fotografías de daños |
| Almacenamiento | Permiso necesario en Android 9 o inferior para guardar PDFs |

---

## 3. Inicio de Sesión

Al abrir la aplicación por primera vez, verá la pantalla inicial con el logo de Ferji y un botón para ingresar.

### Pasos:

1. Toque el botón **"Ingresar"** en la pantalla inicial.
2. Se abrirá la pantalla de **Login** con tres campos:

| Campo | Descripción | Ejemplo |
|---|---|---|
| **RUT** | Su RUT chileno con formato válido | 12.345.678-9 |
| **Nombre Completo** | Su nombre y apellido | Juan Pérez González |
| **Correo Electrónico** | Su email de contacto | juan.perez@empresa.cl |

3. Complete los tres campos y toque **"Iniciar Sesión"**.

### Funcionalidad inteligente:
- Si su **RUT ya fue registrado** anteriormente, los campos de nombre y correo se completarán automáticamente.
- Si desea cambiar su correo electrónico, simplemente modifíquelo y presione "Iniciar Sesión" — se actualizará en la base de datos.

### Validaciones:
- El RUT debe ser un RUT chileno válido (se verifica el dígito verificador).
- El correo electrónico debe tener un formato válido (ejemplo@dominio.com).

> ⚠️ **Nota:** La primera vez que inicie sesión, la aplicación sincronizará las categorías de daños desde el servidor. Esto puede tomar unos segundos.

---

## 4. Pantalla de Bienvenida

Después de iniciar sesión, verá la pantalla de bienvenida que muestra:

- Su **nombre de usuario**
- Su **rol** (Inspector o Administrador)
- Botón **"Ingresar al Sistema"** → lleva al Menú Principal
- Botón **"Cerrar Sesión"** → vuelve a la pantalla inicial

---

## 5. Menú Principal

El menú principal es el centro de la aplicación. Desde aquí accede a todas las funcionalidades.

### Sección: Inspecciones (visible para todos)

| Opción | Descripción |
|---|---|
| 🟢 **Nueva Inspección** | Crear una nueva inspección de siniestro desde cero |
| 🔵 **Inspecciones Pendientes** | Ver y retomar inspecciones que no se han finalizado |
| 🟠 **Enviar Inspección** | Reenviar el PDF de una inspección ya completada |

### Sección: Administración (solo administradores)

| Opción | Descripción |
|---|---|
| 🟣 **Maestro de Partidas** | Gestionar las categorías y tipos de daño |
| ⚫ **Mantenedor de Precios** | Asignar precios unitarios a las partidas |
| 🟢 **Configuración de Correos** | Configurar destinatarios y reglas de envío de emails |

> 💡 **Tip:** También puede acceder a la Configuración desde el ícono de ⚙️ en la barra superior del menú.

---

## 6. Crear Nueva Inspección

### Pasos:

1. En el Menú Principal, toque **"Nueva Inspección"**.
2. Complete el formulario con los siguientes datos:

| Campo | Descripción | Validación |
|---|---|---|
| **RUT Asegurado** | RUT del cliente/asegurado | RUT chileno válido |
| **N° Siniestro** | Número del siniestro asignado por la aseguradora | Obligatorio |
| **Dirección** | Dirección completa del inmueble a inspeccionar | Obligatorio |
| **RUT Inspector** | Su RUT de inspector (se completa automáticamente) | RUT chileno válido |
| **Email Contacto** | Su correo electrónico (se completa automáticamente) | Email válido |

3. Una vez completados todos los campos, toque el botón **"Guardar y Continuar"**.
4. Se guardará la inspección y pasará a la pantalla de Agregar Habitaciones.

> ℹ️ Los campos **RUT Inspector** y **Email Contacto** se completan automáticamente con los datos de su sesión.

### Botón Atrás:
- Use la flecha ← en la barra superior para volver al menú sin guardar.

---

## 7. Agregar Habitaciones

Esta es la pantalla donde registra los daños de cada habitación o espacio del inmueble. Puede agregar tantas habitaciones como necesite.

### Sección 1: Datos de la Habitación

| Campo | Descripción | Ejemplo |
|---|---|---|
| **Nombre** | Nombre identificador de la habitación | Dormitorio Principal, Cocina, Baño 2 |

### Sección 2: Medidas (opcional)

| Campo | Descripción | Formato |
|---|---|---|
| **Alto** | Altura de la habitación | En centímetros (ej: 250) |
| **Largo** | Largo de la habitación | En centímetros (ej: 400) |
| **Ancho** | Ancho de la habitación | En centímetros (ej: 350) |

### Sección 3: Tipo de Daños

Seleccione uno o más tipos de daño de la lista desplegable. Estos corresponden a las categorías variables configuradas en el Maestro de Partidas:

- Toque el menú desplegable para ver las categorías disponibles
- Seleccione las categorías que apliquen (puede seleccionar varias)
- Si el daño no corresponde a ninguna categoría, active la opción **"Otro"** y escriba una descripción

### Sección 4: Comentarios

Escriba observaciones adicionales sobre la habitación o los daños encontrados.

### Sección 5: Fotografías

1. Toque el botón **"Tomar Foto"** 📷
2. La cámara del dispositivo se abrirá
3. Tome la fotografía del daño
4. La foto aparecerá en la galería inferior
5. Puede tomar múltiples fotos por habitación
6. Para eliminar una foto, toque el ícono ✕ sobre la imagen

> ⚠️ **Permiso de cámara:** La primera vez se le pedirá permiso para acceder a la cámara. Debe aceptarlo para poder tomar fotos.

### Acciones al finalizar la habitación:

| Botón | Acción |
|---|---|
| **"Guardar y Siguiente"** | Guarda la habitación actual y limpia el formulario para agregar otra |
| **"Terminar Inspección"** | Guarda la habitación actual (si tiene datos) y finaliza toda la inspección |
| ← **Atrás** | Vuelve sin guardar la habitación actual |

---

## 8. Finalizar Inspección

Cuando presiona **"Terminar Inspección"** desde la pantalla de habitaciones, el sistema ejecuta automáticamente los siguientes pasos:

1. **Guarda la última habitación** (si tiene datos ingresados)
2. **Cambia el estado** de la inspección de PENDIENTE a COMPLETADA
3. **Genera el informe PDF** con todos los datos de la inspección y habitaciones
4. **Genera el presupuesto Excel** con las partidas y costos de reparación
5. **Envía los emails** según la configuración establecida:
   - Al **Administrador**: siempre recibe PDF + Excel
   - Al **Inspector**: recibe PDF y/o Excel según las reglas configuradas

> 📧 El proceso de envío es completamente automático. Verá una pantalla de progreso con el estado: "Generando PDF...", "Enviando email...", etc.

### Al completarse:
La aplicación regresará automáticamente al Menú Principal.

---

## 9. Inspecciones Pendientes (Retomar)

Si una inspección quedó sin finalizar (cerró la app, presionó atrás, etc.), puede retomarla.

### Pasos:

1. En el Menú Principal, toque **"Inspecciones Pendientes"**.
2. Verá una lista con todas sus inspecciones, mostrando:
   - Número de inspección
   - RUT del asegurado
   - Número de siniestro
   - Dirección
   - Estado (🟠 Pendiente / 🟢 Completada)
   - Cantidad de habitaciones registradas
   - Fecha de creación
3. Las inspecciones Pendientes muestran un botón **"Retomar"** en color naranja.
4. Toque **"Retomar"** para continuar agregando habitaciones.
5. Al presionar **"Terminar Inspección"**, se ejecutará el proceso completo de finalización (PDF + email).

### Panel de estadísticas:
En la parte superior de la lista verá tarjetas con:
- **Total** de inspecciones
- **Pendientes** (en naranja)
- **Completadas** (en verde)

> 💡 Las inspecciones completadas aparecen en la lista pero no son clicables (ya fueron finalizadas).

---

## 10. Enviar Inspección

Esta pantalla permite reenviar el PDF de una inspección que ya fue completada. Útil cuando el email no llegó o desea enviarlo a otra persona.

### Pasos:

1. En el Menú Principal, toque **"Enviar Inspección"**.
2. Solo aparecerán las inspecciones con estado COMPLETADA.
3. Use la **barra de búsqueda** para filtrar por siniestro, RUT, dirección o inspector.
4. Toque el botón **"Enviar Inspección"** en la tarjeta correspondiente.
5. El sistema regenerará el PDF y lo enviará por email.

> ℹ️ En esta pantalla solo se envía el PDF de inspección (sin el presupuesto Excel), ya que está orientada al inspector.

---

## 11. Maestro de Partidas (Admin)

> 🔒 Solo disponible para usuarios con rol de Administrador.

El Maestro de Partidas permite gestionar las categorías de daños que los inspectores ven al registrar habitaciones.

### Estructura:
- **Partidas Principales** → Categorías generales (ej: "Fisura Pared", "Humedad Cielo")
- **Partidas Hijas** → Subcategorías o elementos específicos dentro de cada partida principal

### Naturaleza de las partidas:

| Tipo | Descripción |
|---|---|
| **VARIABLE** | Aparecen en el combobox de selección al registrar daños en habitaciones |
| **FIJA** | Se agregan automáticamente al presupuesto sin que el inspector las seleccione |

### Acciones disponibles:
- Agregar nuevas partidas principales
- Agregar partidas hijas a cada partida principal
- Los cambios se sincronizan con Firebase

---

## 12. Mantenedor de Precios (Admin)

> 🔒 Solo disponible para usuarios con rol de Administrador.

Permite asignar precios unitarios a las partidas para el cálculo del presupuesto de reparación.

### Campos por partida:

| Campo | Descripción |
|---|---|
| **Descripción** | Nombre de la partida |
| **Unidad** | Unidad de medida (m², ml, U, gl, etc.) |
| **Precio Unitario** | Costo por unidad en la moneda configurada |

---

## 13. Configuración de Correos (Admin)

> 🔒 Solo disponible para usuarios con rol de Administrador.

Desde esta pantalla se configuran los destinatarios y las reglas de envío de emails para todas las inspecciones.

### Campos configurables:

| Campo | Descripción |
|---|---|
| **Email Administrador** | Correo principal que siempre recibe todos los documentos (PDF + Excel). Obligatorio. |
| **Email en Copia (CC)** | Correo adicional opcional que se incluye en copia en todos los envíos. |

### Reglas de envío (toggles):

| Regla | Descripción |
|---|---|
| **Enviar Inspección al Inspector** | Si está activado, el inspector recibe una copia del PDF de inspección. |
| **Enviar Presupuesto al Inspector** | Si está activado, el inspector recibe una copia del presupuesto Excel. |

### Panel de Resumen:
Al final de la pantalla se muestra un resumen visual de la configuración actual para verificar que todo esté correcto.

### Importante:
- Los cambios se guardan en la nube (Firebase Firestore) y aplican inmediatamente a todas las inspecciones futuras.
- El botón **"Guardar Configuración"** almacena todos los cambios de una vez.

### Ejemplo de flujo de emails:

| Configuración | Email Admin | Email Inspector |
|---|---|---|
| Inspección ✅ / Presupuesto ❌ | PDF + Excel | Solo PDF |
| Inspección ✅ / Presupuesto ✅ | PDF + Excel | PDF + Excel |
| Inspección ❌ / Presupuesto ❌ | PDF + Excel | No recibe nada |

---

## 14. Preguntas Frecuentes

### ¿Qué pasa si cierro la app sin terminar una inspección?
La inspección queda guardada con estado PENDIENTE. Puede retomarla desde "Inspecciones Pendientes" en el menú principal.

### ¿Puedo agregar más habitaciones a una inspección ya empezada?
Sí. Vaya a "Inspecciones Pendientes", busque la inspección y toque "Retomar". Se abrirá la pantalla de habitaciones para continuar agregando.

### ¿Puedo reenviar una inspección ya completada?
Sí. Use la opción "Enviar Inspección" en el menú principal. Solo aparecerán las inspecciones completadas.

### ¿Por qué no veo las opciones de Administración?
Las opciones de Maestro de Partidas, Mantenedor de Precios y Configuración de Correos solo son visibles para usuarios con rol de Administrador. Contacte a su administrador si necesita acceso.

### ¿Cómo cambio mi correo electrónico?
En la pantalla de Login, ingrese su RUT (se autocompletarán los datos) y modifique el correo. Al presionar "Iniciar Sesión", se actualizará.

### ¿Necesito conexión a internet?
- **Para iniciar sesión:** Sí (primera vez para autenticación con Firebase)
- **Para crear inspecciones y habitaciones:** No (se guardan localmente)
- **Para finalizar y enviar emails:** Sí (necesita conexión para el envío SMTP)
- **Para sincronizar partidas:** Sí (se descargan de Firebase)

### ¿Cómo borro todos los datos de la app?
Vaya a Ajustes del teléfono → Aplicaciones → Ferji Inspecciones → Almacenamiento → Borrar datos. Esto eliminará toda la información local (inspecciones, sesión, etc.).

---

**© 2026 Ferji Inspecciones — Todos los derechos reservados.**

