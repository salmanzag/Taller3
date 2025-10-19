# Servicio de Monitoreo de Estado de Usuarios

## Implementación Completa

Se ha implementado un sistema completo de monitoreo de estado de usuarios con las siguientes características:

### 1. **UserStatusService** (Nuevo archivo)
- Servicio de Android que se ejecuta en segundo plano
- Escucha cambios en tiempo real en Firebase Database
- Detecta cuando un usuario cambia su estado de "connected" a "disconnected" o viceversa
- Muestra notificaciones Toast cuando:
  - ✅ Un usuario se conecta: "Juan Pérez se ha conectado"
  - ✅ Un usuario se desconecta: "Juan Pérez se ha desconectado"
- Envía broadcasts para actualizar la lista de usuarios en tiempo real
- Limpia recursos al destruirse para evitar memory leaks

### 2. **UsersListActivity** (Modificado)
- **Filtrado automático**: Solo muestra usuarios con estado "connected" (disponibles)
- **BroadcastReceiver**: Escucha cambios de estado y recarga la lista automáticamente
- **Desaparición automática**: Cuando un usuario pasa a "disconnected", desaparece de la lista
- **Aparición automática**: Cuando un usuario pasa a "connected", aparece en la lista
- Limpia el receiver al destruirse para evitar memory leaks

### 3. **MapActivity** (Modificado)
- Inicia el servicio `UserStatusService` cuando el usuario entra al mapa
- Detiene el servicio cuando el usuario cierra sesión
- Gestión adecuada del ciclo de vida del servicio

### 4. **AndroidManifest.xml** (Modificado)
- Servicio `UserStatusService` declarado correctamente
- Configurado como no exportado por seguridad

## Flujo de Funcionamiento

```
1. Usuario inicia sesión → MapActivity inicia UserStatusService
2. UserStatusService escucha cambios en /users en Firebase
3. Usuario A cambia estado a "connected":
   - UserStatusService detecta el cambio
   - Muestra Toast: "Usuario A se ha conectado"
   - Envía broadcast "USER_STATUS_CHANGED"
   - UsersListActivity recibe broadcast y recarga lista
   - Usuario A aparece en la lista
4. Usuario B cambia estado a "disconnected":
   - UserStatusService detecta el cambio
   - Muestra Toast: "Usuario B se ha desconectado"
   - Envía broadcast "USER_STATUS_CHANGED"
   - UsersListActivity recibe broadcast y recarga lista
   - Usuario B desaparece de la lista
5. Usuario cierra sesión → MapActivity detiene UserStatusService
```

## Características Técnicas

✅ **Tiempo real**: Usa ValueEventListener de Firebase para actualizaciones instantáneas
✅ **Eficiencia**: No procesa cambios del usuario actual (solo otros usuarios)
✅ **Memoria limpia**: Remueve todos los listeners en onDestroy()
✅ **Broadcast**: Comunicación entre servicio y actividades
✅ **Compatibilidad**: Maneja diferencias de API level (TIRAMISU+)
✅ **Prevención de duplicados**: Usa Map para rastrear estados previos
✅ **Notificaciones amigables**: Toast con nombre completo del usuario

## Resultados Esperados

- **Lista dinámica**: Solo usuarios disponibles (connected) son visibles
- **Notificaciones**: Toast cada vez que alguien se conecta/desconecta
- **Sin usuarios desconectados**: La lista se actualiza automáticamente
- **Performance óptimo**: Sin memory leaks ni listeners huérfanos
