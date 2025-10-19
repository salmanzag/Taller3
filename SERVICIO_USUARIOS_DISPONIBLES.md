# 📡 Servicio de Usuarios Disponibles

## Funcionalidad Implementada

### ✅ 1. Servicio en Segundo Plano
**Archivo**: `UserStatusService.kt`

El servicio escucha cambios en tiempo real de todos los usuarios en Firebase:
- **Detecta** cuando un usuario cambia de estado
- **Muestra Toast** con el nombre del usuario y su nuevo estado
- **Envía broadcast** para actualizar la lista automáticamente

### 🔔 2. Notificaciones de Cambio de Estado

**Cuando un usuario SE CONECTA:**
```
✅ [Nombre Usuario] está disponible
```

**Cuando un usuario SE DESCONECTA:**
```
❌ [Nombre Usuario] se desconectó
```

### 📋 3. Lista de Usuarios Disponibles
**Archivo**: `UsersListActivity.kt`

**Comportamiento:**
- ✅ **Muestra SOLO** usuarios con estado `"connected"` o `"available"`
- ✅ **Oculta** usuarios con estado `"disconnected"`
- ✅ **Se actualiza automáticamente** cuando recibe el broadcast del servicio
- ✅ **Actualización en tiempo real** mediante Firebase ValueEventListener

### 🔄 Flujo de Funcionamiento

```
1. Usuario A cambia su estado a "connected" en MapActivity
   ↓
2. Firebase actualiza el campo "status" en la base de datos
   ↓
3. UserStatusService detecta el cambio
   ↓
4. UserStatusService muestra Toast: "✅ Usuario A está disponible"
   ↓
5. UserStatusService envía broadcast "USER_STATUS_CHANGED"
   ↓
6. UsersListActivity recibe el broadcast
   ↓
7. UsersListActivity recarga la lista desde Firebase
   ↓
8. Usuario A APARECE en la lista (porque su status = "connected")
```

```
1. Usuario B cambia su estado a "disconnected" en MapActivity
   ↓
2. Firebase actualiza el campo "status" en la base de datos
   ↓
3. UserStatusService detecta el cambio
   ↓
4. UserStatusService muestra Toast: "❌ Usuario B se desconectó"
   ↓
5. UserStatusService envía broadcast "USER_STATUS_CHANGED"
   ↓
6. UsersListActivity recibe el broadcast
   ↓
7. UsersListActivity recarga la lista desde Firebase
   ↓
8. Usuario B DESAPARECE de la lista (porque su status = "disconnected")
```

## 📂 Archivos Modificados

### 1. `UserStatusService.kt`
**Mejoras:**
- ✅ Logs detallados para debugging
- ✅ Manejo de nombres vacíos (muestra primeros 8 caracteres del UID)
- ✅ Toast con emojis para mejor experiencia
- ✅ Soporte para estados "connected" y "available"

### 2. `UsersListActivity.kt`
**Mejoras:**
- ✅ **Filtro de estado**: `if (user.status != "connected" && user.status != "available") continue`
- ✅ BroadcastReceiver para recibir notificaciones del servicio
- ✅ Recarga automática cuando detecta cambios
- ✅ Mensajes específicos: "X usuario(s) disponible(s)"

### 3. `MapActivity.kt`
**Función de cambio de estado:**
- Botón de estado que alterna entre "connected" y "disconnected"
- Actualiza Firebase inmediatamente
- Dispara toda la cadena de eventos

## 🧪 Pruebas

### Escenario 1: Usuario se conecta
1. Usuario A abre la app (estado inicial: "disconnected")
2. Usuario A toca el botón de estado → cambia a "connected"
3. **Usuario B ve:**
   - Toast: "✅ Usuario A está disponible"
   - Usuario A aparece en la lista de usuarios disponibles

### Escenario 2: Usuario se desconecta
1. Usuario A toca el botón de estado → cambia a "disconnected"
2. **Usuario B ve:**
   - Toast: "❌ Usuario A se desconectó"
   - Usuario A desaparece de la lista de usuarios disponibles

### Escenario 3: Ver lista vacía
1. Todos los usuarios están "disconnected"
2. Al abrir la lista: "⚠️ No hay usuarios disponibles en este momento"

## 🔍 Debugging

### Logs del Servicio
Filtra Logcat por `UserStatusService`:
```
✅ Servicio iniciado
👂 Escuchando cambios de estado de usuarios
🔄 Cambio detectado en usuarios
📊 [Nombre]: disconnected → connected
✅ [Nombre] está disponible
📡 Broadcast enviado: USER_STATUS_CHANGED
```

### Logs de la Lista
Filtra Logcat por `UsersListActivity`:
```
📊 Total usuarios en Firebase: 2
--- Usuario 1 ---
  Status: connected
  ✅ Usuario agregado a la lista
--- Usuario 2 ---
  Status: disconnected
  ⏭️ Saltando: Usuario no disponible (disconnected)
📝 Total usuarios en lista: 1
```

## ⚙️ Configuración en AndroidManifest.xml

El servicio ya está registrado:
```xml
<service
    android:name=".UserStatusService"
    android:enabled="true"
    android:exported="false" />
```

Se inicia automáticamente en `MapActivity.onCreate()`:
```kotlin
startService(Intent(this, UserStatusService::class.java))
```

## ✨ Resultado Final

- ✅ Servicio ejecutándose en segundo plano
- ✅ Toast automáticos al conectar/desconectar
- ✅ Lista que se actualiza en tiempo real
- ✅ Solo muestra usuarios disponibles
- ✅ Experiencia de usuario fluida y responsiva
