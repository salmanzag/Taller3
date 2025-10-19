package com.example.taller3

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserStatusService : Service() {

    private val database = FirebaseDatabase.getInstance("https://trabajo-en-clase-57464-default-rtdb.firebaseio.com").reference
    private val auth = FirebaseAuth.getInstance()
    private var usersListener: ValueEventListener? = null
    private val userStatusMap = mutableMapOf<String, String>()

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("UserStatusService", " Servicio iniciado")
        startListeningToUsers()
    }

    private fun startListeningToUsers() {
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            android.util.Log.w("UserStatusService", " No hay usuario autenticado")
            return
        }

        android.util.Log.d("UserStatusService", " Escuchando cambios de estado de usuarios")

        usersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("UserStatusService", " Cambio detectado en usuarios")
                
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    
                    if (userId == currentUserId) continue

                    val firstName = userSnapshot.child("firstName").getValue(String::class.java) ?: ""
                    val lastName = userSnapshot.child("lastName").getValue(String::class.java) ?: ""
                    val fullName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                        "$firstName $lastName".trim()
                    } else {
                        "Usuario ${userId.take(8)}"
                    }
                    
                    val newStatus = userSnapshot.child("status").getValue(String::class.java) ?: "disconnected"
                    val previousStatus = userStatusMap[userId]
                    
                    if (previousStatus != null && previousStatus != newStatus) {
                        android.util.Log.d("UserStatusService", " $fullName: $previousStatus → $newStatus")
                        
                        when (newStatus) {
                            "connected", "available" -> {
                                val message = " $fullName está disponible"
                                android.util.Log.i("UserStatusService", message)
                                showToast(message)
                                sendStatusChangeBroadcast()
                            }
                            "disconnected" -> {
                                val message = " $fullName se desconectó"
                                android.util.Log.i("UserStatusService", message)
                                showToast(message)
                                sendStatusChangeBroadcast()
                            }
                        }
                    }
                    
                    userStatusMap[userId] = newStatus
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("UserStatusService", "Error: ${error.message}")
            }
        }

        database.child("users").addValueEventListener(usersListener!!)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun sendStatusChangeBroadcast() {
        val intent = Intent("com.example.taller3.USER_STATUS_CHANGED")
        sendBroadcast(intent)
        android.util.Log.d("UserStatusService", " Broadcast enviado: USER_STATUS_CHANGED")
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("UserStatusService", "Servicio detenido")
        usersListener?.let {
            database.child("users").removeEventListener(it)
        }
        userStatusMap.clear()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
