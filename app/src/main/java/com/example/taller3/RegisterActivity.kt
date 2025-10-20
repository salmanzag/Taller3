package com.example.taller3

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.taller3.databinding.ActivityRegisterBinding
import com.example.taller3.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val REQUEST_GALLERY = 1
    private val REQUEST_CAMERA = 2

    private var imageUri: Uri? = null
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance("https://trabajo-en-clase-57464-default-rtdb.firebaseio.com").reference
    private val storage = FirebaseStorage.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        binding.buttonTomarFoto.setOnClickListener { takePhoto() }
        binding.buttonGaleria.setOnClickListener { openGallery() }
        binding.buttonCrearCuentaRegister.setOnClickListener { createAccount() }
    }
    
    private fun takePhoto() {
        openCamera()
    }

    private fun openGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), REQUEST_GALLERY)
        } else {
            openGalleryIntent()
        }
    }
    
    private fun openGalleryIntent() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        } else {
            openCameraIntent()
        }
    }
    
    private fun openCameraIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = try {
            createImageFile()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.let {
            val photoURI = FileProvider.getUriForFile(this, "${packageName}.fileprovider", it)
            imageUri = photoURI
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, REQUEST_CAMERA)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = cacheDir
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCameraIntent()
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_GALLERY -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGalleryIntent()
                } else {
                    Toast.makeText(this, "Permiso de galería denegado", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQUEST_GALLERY -> {
                imageUri = data?.data
                Glide.with(this).load(imageUri).centerCrop().into(binding.imgPerfil)
            }
            REQUEST_CAMERA -> {
                imageUri?.let { Glide.with(this).load(it).centerCrop().into(binding.imgPerfil) }
            }
        }
    }

    private fun createAccount() {
        val firstName = binding.etNombreUsuario.text.toString().trim()
        val lastName = binding.etApellidoUsuario.text.toString().trim()
        val email = binding.etCorreoUsuario.text.toString().trim()
        val idNumber = binding.etIDUsuario.text.toString().trim()
        val password = binding.etPasswordUsuario.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || idNumber.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Ingresa un email válido", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonCrearCuentaRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    binding.buttonCrearCuentaRegister.isEnabled = true
                    Toast.makeText(this, "Error: No se pudo obtener el UID del usuario", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                uploadImageAndSaveUser(uid, firstName, lastName, email, idNumber)
            }
            .addOnFailureListener { exception ->
                binding.buttonCrearCuentaRegister.isEnabled = true
                val errorMessage = when {
                    exception.message?.contains("password") == true -> "Contraseña incorrecta o muy débil"
                    exception.message?.contains("email") == true -> "Email ya registrado o inválido"
                    exception.message?.contains("network") == true -> "Error de conexión"
                    else -> "Datos inválidos: ${exception.message}"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun uploadImageAndSaveUser(uid: String, firstName: String, lastName: String, email: String, idNumber: String) {
        if (imageUri == null) {
            saveUser(uid, firstName, lastName, email, idNumber, "")
            return
        }

        val imageRef = storage.child("profile_images/$uid.jpg")
        
        imageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val photoUrl = uri.toString()
                    saveUser(uid, firstName, lastName, email, idNumber, photoUrl)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error subiendo foto, guardando sin imagen", Toast.LENGTH_SHORT).show()
                saveUser(uid, firstName, lastName, email, idNumber, "")
            }
    }

    private fun saveUser(uid: String, firstName: String, lastName: String, email: String, idNumber: String, photoUrl: String) {
        var lat = 0.0
        var lon = 0.0
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val lm = getSystemService(LOCATION_SERVICE) as LocationManager
                val providers = lm.getProviders(true)
                for (p in providers) {
                    val loc = lm.getLastKnownLocation(p) ?: continue
                    lat = loc.latitude
                    lon = loc.longitude
                    break
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        val user = User(uid, firstName, lastName, email, idNumber, photoUrl, lat, lon, "disconnected")
        database.child("users").child(uid).setValue(user)
            .addOnSuccessListener {
                binding.buttonCrearCuentaRegister.isEnabled = true
                Toast.makeText(this, "Cuenta creada correctamente", Toast.LENGTH_LONG).show()
                
                val intent = Intent(this, MapActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { error ->
                binding.buttonCrearCuentaRegister.isEnabled = true
                auth.currentUser?.delete()?.addOnCompleteListener {
                    auth.signOut()
                }
                Toast.makeText(this, "Error guardando usuario: ${error.message}", Toast.LENGTH_LONG).show()
            }
    }
}
