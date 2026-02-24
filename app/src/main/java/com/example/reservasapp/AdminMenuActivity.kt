package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        val etPrincipales = findViewById<EditText>(R.id.etPlatosPrincipales)
        val etPostres = findViewById<EditText>(R.id.etPostres)
        val etNuevaSeccion = findViewById<EditText>(R.id.etNuevaSeccion)
        val etOpcionesNuevaSeccion = findViewById<EditText>(R.id.etOpcionesNuevaSeccion)
        val btnGuardarBase = findViewById<Button>(R.id.btnGuardarMenusBase)
        val btnAgregarSeccion = findViewById<Button>(R.id.btnAgregarSeccion)
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenuAdmin)
        val listSecciones = findViewById<ListView>(R.id.listSeccionesMenu)

        val principalesActuales = MenuRepository.obtenerOpcionesPorSeccion("Plato principal")
        val postresActuales = MenuRepository.obtenerOpcionesPorSeccion("Postres")
        etPrincipales.setText(principalesActuales.joinToString(", "))
        etPostres.setText(postresActuales.joinToString(", "))

        fun refrescarListadoSecciones() {
            val items = MenuRepository.obtenerSecciones().map {
                "${it.nombre}: ${it.opciones.joinToString(", ")}"
            }
            listSecciones.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        }

        refrescarListadoSecciones()

        btnGuardarBase.setOnClickListener {
            val principales = etPrincipales.text.toString().split(",").map { it.trim() }.filter { it.isNotBlank() }
            val postres = etPostres.text.toString().split(",").map { it.trim() }.filter { it.isNotBlank() }

            if (principales.isEmpty() || postres.isEmpty()) {
                Toast.makeText(this, R.string.error_opciones_vacias, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MenuRepository.actualizarOpciones("Plato principal", principales)
            MenuRepository.actualizarOpciones("Postres", postres)
            refrescarListadoSecciones()
            Toast.makeText(this, R.string.mensaje_menus_actualizados, Toast.LENGTH_SHORT).show()
        }

        btnAgregarSeccion.setOnClickListener {
            val nombre = etNuevaSeccion.text.toString().trim()
            val opciones = etOpcionesNuevaSeccion.text.toString().split(",").map { it.trim() }.filter { it.isNotBlank() }

            if (nombre.isBlank() || opciones.isEmpty()) {
                Toast.makeText(this, R.string.error_seccion_invalida, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MenuRepository.agregarSeccion(nombre, opciones)
            etNuevaSeccion.setText("")
            etOpcionesNuevaSeccion.setText("")
            refrescarListadoSecciones()
            Toast.makeText(this, R.string.mensaje_seccion_agregada, Toast.LENGTH_SHORT).show()
        }

        btnVolverMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
