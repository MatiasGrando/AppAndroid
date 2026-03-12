package com.example.reservasapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminMenuActivity : AppCompatActivity() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedConfigDateMillis: Long = Calendar.getInstance().clearTime().timeInMillis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        val etNombrePlato = findViewById<android.widget.EditText>(R.id.etNombrePlato)
        val etDetallePlato = findViewById<android.widget.EditText>(R.id.etDetallePlato)
        val etImagenUrl = findViewById<android.widget.EditText>(R.id.etImagenUrl)
        val selectorSeccion = findViewById<AutoCompleteTextView>(R.id.actvSeccionPlato)
        val tvGuarnicion = findViewById<TextView>(R.id.tvGuarnicion)
        val layoutGuarnicionChecks = findViewById<LinearLayout>(R.id.layoutGuarnicionChecks)
        val cbGuarnicionSi = findViewById<CheckBox>(R.id.cbGuarnicionSi)
        val cbGuarnicionNo = findViewById<CheckBox>(R.id.cbGuarnicionNo)
        val btnCrearPlato = findViewById<Button>(R.id.btnCrearPlato)
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenuAdmin)
        val listSecciones = findViewById<ListView>(R.id.listSeccionesMenu)
        val tvFechaSeleccionada = findViewById<TextView>(R.id.tvFechaConfigSeleccionada)
        val btnSeleccionarFecha = findViewById<Button>(R.id.btnSeleccionarFechaConfig)
        val btnGuardarFecha = findViewById<Button>(R.id.btnGuardarPlatosPorFecha)
        val listPlatosFecha = findViewById<ListView>(R.id.listPlatosPorFecha)

        val secciones = MenuRepository.seccionesPermitidas()
        selectorSeccion.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, secciones))
        selectorSeccion.threshold = 0
        selectorSeccion.setOnClickListener { selectorSeccion.showDropDown() }
        selectorSeccion.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) selectorSeccion.showDropDown()
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

        var platosConfigurables: List<DishConfigOption> = emptyList()

        fun actualizarTextoFecha() {
            tvFechaSeleccionada.text = getString(
                R.string.admin_menu_fecha_config,
                dateFormatter.format(Date(selectedConfigDateMillis))
            )
        }

        fun cargarOpcionesPorFecha() {
            MenuRepository.cargarSecciones { okMenu, loadedSections ->
                if (!okMenu) {
                    runOnUiThread {
                        Toast.makeText(this, R.string.error_cargar_menu, Toast.LENGTH_SHORT).show()
                    }
                    return@cargarSecciones
                }

                MenuRepository.obtenerClavesPlatosHabilitados(selectedConfigDateMillis) { okConfig, dishKeys ->
                    runOnUiThread {
                        if (!okConfig) {
                            Toast.makeText(this, R.string.error_cargar_menu_fecha, Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }

                        platosConfigurables = loadedSections.flatMap { section ->
                            section.opciones.map { dish ->
                                DishConfigOption(
                                    label = "${section.nombre} • ${dish.nombre}",
                                    dishKey = MenuRepository.clavePlato(section.nombre, dish.nombre)
                                )
                            }
                        }

                        val labels = platosConfigurables.map { it.label }
                        listPlatosFecha.choiceMode = ListView.CHOICE_MODE_MULTIPLE
                        listPlatosFecha.adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_list_item_multiple_choice,
                            labels
                        )

                        val useFallback = dishKeys == null
                        platosConfigurables.forEachIndexed { index, option ->
                            val checked = if (useFallback) true else dishKeys.contains(option.dishKey)
                            listPlatosFecha.setItemChecked(index, checked)
                        }
                    }
                }
            }
        }

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

        actualizarTextoFecha()
        refrescarListadoSecciones()
        cargarOpcionesPorFecha()

        btnSeleccionarFecha.setOnClickListener {
            val calendar = Calendar.getInstance().clearTime().apply { timeInMillis = selectedConfigDateMillis }
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selected = Calendar.getInstance().clearTime().apply {
                        set(year, month, dayOfMonth)
                    }
                    selectedConfigDateMillis = selected.timeInMillis
                    actualizarTextoFecha()
                    cargarOpcionesPorFecha()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnGuardarFecha.setOnClickListener {
            if (platosConfigurables.isEmpty()) {
                Toast.makeText(this, R.string.error_sin_platos_para_configurar, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedKeys = mutableSetOf<String>()
            for (i in platosConfigurables.indices) {
                if (listPlatosFecha.isItemChecked(i)) {
                    selectedKeys.add(platosConfigurables[i].dishKey)
                }
            }

            MenuRepository.guardarPlatosHabilitadosParaFecha(selectedConfigDateMillis, selectedKeys) { ok ->
                runOnUiThread {
                    if (!ok) {
                        Toast.makeText(this, R.string.error_guardar_menu_fecha, Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                    Toast.makeText(this, R.string.mensaje_menu_fecha_guardado, Toast.LENGTH_SHORT).show()
                }
            }
        }

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
                    cargarOpcionesPorFecha()
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

    private fun Calendar.clearTime(): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private data class DishConfigOption(
    val label: String,
    val dishKey: String
)
