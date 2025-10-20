package com.example.taller3

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.taller3.databinding.ActivityUsersListBinding
import com.example.taller3.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UsersListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersListBinding
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance("https://trabajo-en-clase-57464-default-rtdb.firebaseio.com").reference
    private val usersList = mutableListOf<User>()
    private lateinit var adapter: UserListAdapter
    private var usersListener: ValueEventListener? = null
    
    private val statusChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "USER_STATUS_CHANGED") {
                loadUsers()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        adapter = UserListAdapter(usersList)
        binding.listViewUsers.adapter = adapter

        val filter = IntentFilter("USER_STATUS_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusChangeReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusChangeReceiver, filter)
        }

        loadUsers()
    }

    private fun loadUsers() {
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            Toast.makeText(this, "Error: No estás autenticado", Toast.LENGTH_LONG).show()
            return
        }

        if (usersListener != null) {
            database.child("users").removeEventListener(usersListener!!)
        }

        usersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersList.clear()
                
                if (!snapshot.exists()) {
                    Toast.makeText(this@UsersListActivity, "No hay usuarios en la base de datos", Toast.LENGTH_LONG).show()
                    return
                }
                
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key
                    
                    try {
                        var user = userSnapshot.getValue(User::class.java)
                        if (user == null) continue
                        
                        if (user.uid.isEmpty() && userId != null) {
                            user = user.copy(uid = userId)
                        }
                        
                        if (user.uid == currentUserId) continue
                        
                        if (user.status != "connected" && user.status != "available") continue
                        
                        usersList.add(user)
                        
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                adapter.notifyDataSetChanged()

                if (usersList.isEmpty()) {
                    Toast.makeText(this@UsersListActivity, "No hay usuarios disponibles en este momento", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@UsersListActivity, "${usersList.size} usuario(s) disponible(s)", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UsersListActivity, "Error de Firebase: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
        
        database.child("users").addValueEventListener(usersListener!!)
    }

    inner class UserListAdapter(private val users: List<User>) : ArrayAdapter<User>(
        this@UsersListActivity,
        R.layout.item_user,
        users
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_user, parent, false)
            
            val user = users[position]
            
            val imgPhoto = view.findViewById<ImageView>(R.id.imgUserPhoto)
            val tvName = view.findViewById<TextView>(R.id.tvUserName)
            val tvStatus = view.findViewById<TextView>(R.id.tvUserStatus)
            val btnVer = view.findViewById<Button>(R.id.btnVerUbicacion)
            
            val displayName = if (user.firstName.isNotEmpty() || user.lastName.isNotEmpty()) {
                "${user.firstName} ${user.lastName}".trim()
            } else {
                "Usuario ${user.uid.take(8)}"
            }
            
            tvName.text = displayName
            tvStatus.text = if (user.status == "connected") "Conectado" else "Desconectado"
            
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(this@UsersListActivity)
                    .load(user.photoUrl)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .circleCrop()
                    .into(imgPhoto)
            } else {
                Glide.with(this@UsersListActivity).clear(imgPhoto)
                imgPhoto.setImageResource(R.mipmap.ic_launcher)
            }
            
            btnVer.setOnClickListener(null)
            btnVer.setOnClickListener {
                val intent = Intent(this@UsersListActivity, UserTrackingActivity::class.java)
                intent.putExtra("userId", user.uid)
                intent.putExtra("userName", "${user.firstName} ${user.lastName}")
                startActivity(intent)
            }
            
            return view
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        usersListener?.let {
            database.child("users").removeEventListener(it)
        }
        usersListener = null
        
        try {
            unregisterReceiver(statusChangeReceiver)
        } catch (e: IllegalArgumentException) {
        }
    }
}
