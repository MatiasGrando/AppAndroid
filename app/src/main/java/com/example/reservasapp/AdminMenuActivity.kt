package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
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
        val tvGuarnicion = findViewById<TextView>(R.id.tvGuarnicion)
        val layoutGuarnicionChecks = findViewById<LinearLayout>(R.id.layoutGuarnicionChecks)
        val cbGuarnicionSi = findViewById<CheckBox>(R.id.cbGuarnicionSi)
        val cbGuarnicionNo = findViewById<CheckBox>(R.id.cbGuarnicionNo)
        val btnCrearPlato = findViewById<Button>(R.id.btnCrearPlato)
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenuAdmin)
        val listSecciones = findViewById<ListView>(R.id.listSeccionesMenu)

        val secciones = MenuRepository.seccionesPermitidas()

        selectorSeccion.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, secciones))
        selectorSeccion.threshold = 0
        selectorSeccion.setOnClickListener { selectorSeccion.showDropDown() }
        selectorSeccion.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                selectorSeccion.showDropDown()
            }
        }
        selectorSeccion.setText(secciones.firstOrNull().orEmpty(), false)

        cbGuarnicionSi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbGuarnicionNo.isChecked = false
        }
        cbGuarnicionNo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbGuarnicionSi.isChecked = false
        }

        fun actualizarVisibilidadGuarnicion() {
            val esPrincipal = selectorSeccion.text.toString().trim() == "Plato principal"
            val visibilidad = if (esPrincipal) View.VISIBLE else View.GONE
            tvGuarnicion.visibility = visibilidad
            layoutGuarnicionChecks.visibility = visibilidad
            if (!esPrincipal) {
                cbGuarnicionSi.isChecked = false
                cbGuarnicionNo.isChecked = false
            }
        }

        selectorSeccion.setOnItemClickListener { _, _, _, _ -> actualizarVisibilidadGuarnicion() }
        actualizarVisibilidadGuarnicion()

        fun refrescarListadoSecciones() {
            MenuRepository.cargarSecciones { ok, loadedSections ->
                runOnUiThread {
                    val items = loadedSections.map { section ->
                        val platos = if (section.opciones.isEmpty()) {
                            "(sin platos)"
                        } else {
                            section.opciones.joinToString(" | ") { plato ->
                                val detalleGuarnicion = if (section.nombre == "Plato principal") {
                                    " (Guarnición: ${if (plato.guarnicion) getString(R.string.si) else getString(R.string.no)})"
                                } else {
                                    ""
                                }
                                "${plato.nombre} - ${plato.detalle}$detalleGuarnicion"
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

            if (seccion.isBlank() || nombre.isBlank()) {
                Toast.makeText(this, R.string.error_plato_invalido, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val guarnicion = if (seccion == "Plato principal") {
                when {
                    cbGuarnicionSi.isChecked -> true
                    cbGuarnicionNo.isChecked -> false
                    else -> {
                        Toast.makeText(this, R.string.error_guarnicion_requerida, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
            } else {
                false
            }

            MenuRepository.agregarPlato(
                seccion = seccion,
                nombre = nombre,
                detalle = detalle,
                imageUrl = imageUrl,
                guarnicion = guarnicion
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
                    cbGuarnicionSi.isChecked = false
                    cbGuarnicionNo.isChecked = false
                    actualizarVisibilidadGuarnicion()
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
