package com.example.reservasapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class PerfilDatosPersonalesActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etEmpresa: EditText
    private lateinit var etDni: EditText
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_datos_personales)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBarPerfil)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etEmpresa = findViewById(R.id.etEmpresa)
        etDni = findViewById(R.id.etDni)
        btnGuardar = findViewById(R.id.btnGuardarPerfil)

        cargarPerfil()

        btnGuardar.setOnClickListener {
            val perfil = PerfilUsuario(
                nombre = etNombre.text.toString(),
                apellido = etApellido.text.toString(),
                empresa = etEmpresa.text.toString(),
                dni = etDni.text.toString()
            )

            PerfilRepository.guardarPerfil(perfil) { ok ->
                runOnUiThread {
                    if (ok) {
                        Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, R.string.profile_save_error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun cargarPerfil() {
        PerfilRepository.cargarPerfil { perfil ->
            runOnUiThread {
                if (perfil == null) {
                    Toast.makeText(this, R.string.profile_load_error, Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                etNombre.setText(perfil.nombre)
                etApellido.setText(perfil.apellido)
                etEmpresa.setText(perfil.empresa)
                etDni.setText(perfil.dni)
            }
        }
    }
}
