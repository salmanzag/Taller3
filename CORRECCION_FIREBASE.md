# 🔧 Corrección de Datos en Firebase

## Problema Detectado
Los usuarios en Firebase solo tienen estos campos:
- ✅ latitude
- ✅ longitude  
- ✅ status
- ❌ FALTAN: firstName, lastName, email, idNumber, photoUrl, uid

## Solución: Agregar Campos Manualmente

### Usuario 1: `5x4Mv3gfv3W99jhGV6Uih1QjZnf1`

1. En Firebase Console, expande el nodo: `users/5x4Mv3gfv3W99jhGV6Uih1QjZnf1`
2. Haz clic en el **+** al lado del UID para agregar campos
3. Agrega cada uno de estos campos:

```
uid: "5x4Mv3gfv3W99jhGV6Uih1QjZnf1"
firstName: "[Nombre del compañero]"
lastName: "[Apellido del compañero]"
email: "[email del compañero]"
idNumber: "[cédula del compañero]"
photoUrl: ""
```

### Usuario 2: `vr3RQvGArycIw02Oba991e0XmUw2`

1. Expande: `users/vr3RQvGArycIw02Oba991e0XmUw2`
2. Haz clic en el **+** para agregar campos
3. Agrega:

```
uid: "vr3RQvGArycIw02Oba991e0XmUw2"
firstName: "Romero"
lastName: "[Tu apellido]"
email: "romeropipe@hotmail.com"
idNumber: "[Tu cédula]"
photoUrl: ""
```

## Pasos en Firebase Console

1. Ve a: https://console.firebase.google.com/project/trabajo-en-clase-57464/database
2. Busca el nodo `users`
3. Expande el UID del usuario
4. Haz clic en el botón **+** (más) al lado del UID
5. Escribe el nombre del campo (ejemplo: `firstName`)
6. Escribe el valor
7. Click en **Agregar**
8. Repite para cada campo

## Alternativa: Volver a Registrarse

**Opción más rápida y segura:**

1. En la app, cierra sesión
2. En Firebase Console, **ELIMINA** los nodos de usuarios incompletos
3. Vuelve a registrarte desde cero
4. Todos los campos se guardarán correctamente

---

## ⚠️ Nota Importante

Después de corregir los datos:
- Cierra y vuelve a abrir la app
- Los nombres aparecerán correctamente en la lista
- El rastreo de ubicación funcionará

## Siguiente Paso

Después de corregir los datos de Firebase, pégame los **logs del error del mapa** filtrando por `UserTracking` cuando le das clic a "Ver ubicación".
