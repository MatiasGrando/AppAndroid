package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegistroUsuarioActivity : AppCompatActivity() {

    private val authRepository by lazy { AuthRepository(this) }
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_usuario)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etDni = findViewById<EditText>(R.id.etDni)

        findViewById<Button>(R.id.btnGuardarRegistro).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val dni = etDni.text.toString().trim()

            if (!RegistroValidator.esNombreValido(nombre) || !RegistroValidator.esNombreValido(apellido)) {
                Toast.makeText(this, R.string.error_nombre_apellido, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!RegistroValidator.esDniValido(dni)) {
                Toast.makeText(this, R.string.error_dni, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val firebaseUser = authRepository.usuarioAutenticado()
            if (firebaseUser == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return@setOnClickListener
            }

            val usuario = Usuario(
                userId = firebaseUser.uid,
                nombre = nombre,
                apellido = apellido,
                dni = dni,
                email = firebaseUser.email.orEmpty(),
                fechaRegistro = System.currentTimeMillis()
            )

            firestoreRepository.guardarUsuario(usuario) { result ->
                if (result.isFailure) {
                    Toast.makeText(this, R.string.error_guardar_usuario, Toast.LENGTH_SHORT).show()
                    return@guardarUsuario
                }

                AppSession.currentUsuario = usuario
                firestoreRepository.esAdmin(firebaseUser.uid) { adminResult ->
                    AppSession.isAdmin = adminResult.getOrDefault(false)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}
