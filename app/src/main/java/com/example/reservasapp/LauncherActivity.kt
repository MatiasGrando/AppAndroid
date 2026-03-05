package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LauncherActivity : AppCompatActivity() {

    private val authRepository by lazy { AuthRepository(this) }
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = authRepository.usuarioAutenticado()
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        firestoreRepository.obtenerUsuario(user.uid) { usuarioResult ->
            val usuario = usuarioResult.getOrNull()
            if (usuario == null) {
                startActivity(Intent(this, RegistroUsuarioActivity::class.java))
                finish()
                return@obtenerUsuario
            }

            AppSession.currentUsuario = usuario
            firestoreRepository.esAdmin(user.uid) { adminResult ->
                AppSession.isAdmin = adminResult.getOrDefault(false)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
