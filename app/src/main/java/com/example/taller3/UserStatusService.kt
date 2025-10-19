package com.example.taller3

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserStatusService : Service() {

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private var usersListener: ValueEventListener? = null
    private val userStatusMap = mutableMapOf<String, String>()

    override fun onCreate() {
        super.onCreate()
        startListeningToUsers()
    }

    private fun startListeningToUsers() {
        val currentUserId = auth.currentUser?.uid ?: return

        usersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    
                    if (userId == currentUserId) continue

                    val name = userSnapshot.child("name").getValue(String::class.java) ?: "Usuario"
                    val lastName = userSnapshot.child("lastName").getValue(String::class.java) ?: ""
                    val fullName = "$name $lastName".trim()
                    val newStatus = userSnapshot.child("status").getValue(String::class.java) ?: "disconnected"

                    val previousStatus = userStatusMap[userId]
                    
                    if (previousStatus != null && previousStatus != newStatus) {
                        when (newStatus) {
                            "connected" -> {
                                showToast("$fullName se ha conectado")
                                sendBroadcast(Intent("USER_STATUS_CHANGED"))
                            }
                            "disconnected" -> {
                                showToast("$fullName se ha desconectado")
                                sendBroadcast(Intent("USER_STATUS_CHANGED"))
                            }
                        }
                    }
                    
                    userStatusMap[userId] = newStatus
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        database.child("users").addValueEventListener(usersListener!!)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        usersListener?.let {
            database.child("users").removeEventListener(it)
        }
        userStatusMap.clear()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
