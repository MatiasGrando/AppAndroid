package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        val etNombrePlato = findViewById<EditText>(R.id.etNombrePlato)
        val etDetallePlato = findViewById<EditText>(R.id.etDetallePlato)
        val etImagenUrl = findViewById<EditText>(R.id.etImagenUrl)
        val selectorSeccion = findViewById<AutoCompleteTextView>(R.id.actvSeccionPlato)
        val btnCrearPlato = findViewById<Button>(R.id.btnCrearPlato)
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenuAdmin)
        val listSecciones = findViewById<ListView>(R.id.listSeccionesMenu)

        val secciones = MenuRepository.seccionesPermitidas()
        selectorSeccion.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, secciones))
        selectorSeccion.setText(secciones.firstOrNull().orEmpty(), false)

        fun refrescarListadoSecciones() {
            MenuRepository.cargarSecciones { ok, loadedSections ->
                runOnUiThread {
                    val items = loadedSections.map { section ->
                        val platos = if (section.opciones.isEmpty()) {
                            "(sin platos)"
                        } else {
                            section.opciones.joinToString(" | ") { plato ->
                                "${plato.nombre} - ${plato.detalle}"
                            }
                        }
                        "${section.nombre}: $platos"
                    }
                    listSecciones.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)

                    if (!ok) {
                        Toast.makeText(this, R.string.error_cargar_menu, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        refrescarListadoSecciones()

        btnCrearPlato.setOnClickListener {
            val seccion = selectorSeccion.text.toString().trim()
            val nombre = etNombrePlato.text.toString().trim()
            val detalle = etDetallePlato.text.toString().trim()
            val imageUrl = etImagenUrl.text.toString().trim()

            if (seccion.isBlank() || nombre.isBlank() || detalle.isBlank() || imageUrl.isBlank()) {
                Toast.makeText(this, R.string.error_plato_invalido, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MenuRepository.agregarPlato(
                seccion = seccion,
                nombre = nombre,
                detalle = detalle,
                imageUrl = imageUrl
            ) { ok ->
                runOnUiThread {
                    if (!ok) {
                        Toast.makeText(this, R.string.error_guardar_menu, Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }

                    etNombrePlato.setText("")
                    etDetallePlato.setText("")
                    etImagenUrl.setText("")
                    selectorSeccion.setText(secciones.firstOrNull().orEmpty(), false)
                    refrescarListadoSecciones()
                    Toast.makeText(this, R.string.mensaje_plato_agregado, Toast.LENGTH_SHORT).show()
                }
            }
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
