package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller3.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        auth.currentUser?.let {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
        }

        binding.buttonCrearCuenta.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.buttonIngresar.setOnClickListener {
            val email = binding.etEMAIL.text.toString().trim()
            val password = binding.etPASSWORD.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingresa email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Ingresa un email válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MapActivity::class.java))
                    finish()
                }
                .addOnFailureListener { exception ->
                    val errorMessage = when {
                        exception.message?.contains("password") == true || 
                        exception.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> 
                            "Contraseña incorrecta"
                        exception.message?.contains("no user record") == true || 
                        exception.message?.contains("user-not-found") == true -> 
                            "Usuario no registrado"
                        exception.message?.contains("email") == true -> 
                            "Email inválido"
                        exception.message?.contains("network") == true -> 
                            "Error de conexión"
                        else -> 
                            "Datos inválidos: Verifica tu email y contraseña"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
        }
    }
}
