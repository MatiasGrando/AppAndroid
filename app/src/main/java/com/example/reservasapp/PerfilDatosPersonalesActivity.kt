package com.example.reservasapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class PerfilDatosPersonalesActivity : AppCompatActivity() {

    private companion object {
        const val EMPRESA_PLACEHOLDER_INDEX = 0
    }

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var spinnerEmpresa: Spinner
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
        spinnerEmpresa = findViewById(R.id.spinnerEmpresa)
        etDni = findViewById(R.id.etDni)
        btnGuardar = findViewById(R.id.btnGuardarPerfil)

        val empresas = resources.getStringArray(R.array.profile_company_options)
        spinnerEmpresa.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            empresas
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        cargarPerfil()

        btnGuardar.setOnClickListener {
            val empresaSeleccionada = if (spinnerEmpresa.selectedItemPosition == EMPRESA_PLACEHOLDER_INDEX) {
                ""
            } else {
                spinnerEmpresa.selectedItem?.toString().orEmpty()
            }

            val perfil = PerfilUsuario(
                nombre = etNombre.text.toString(),
                apellido = etApellido.text.toString(),
                empresa = empresaSeleccionada,
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
                val empresas = resources.getStringArray(R.array.profile_company_options)
                val index = if (perfil.empresa.isBlank()) {
                    EMPRESA_PLACEHOLDER_INDEX
                } else {
                    empresas.indexOf(perfil.empresa).takeIf { it >= 0 } ?: EMPRESA_PLACEHOLDER_INDEX
                }
                spinnerEmpresa.setSelection(index)
                etDni.setText(perfil.dni)
            }
        }
    }
}
