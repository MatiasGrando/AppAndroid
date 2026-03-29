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
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast

class AdminMenuActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAdminAccess()) {
            return
        }

        setContentView(R.layout.activity_admin_menu)

        val etNombrePlato = findViewById<EditText>(R.id.etNombrePlato)
        val etDetallePlato = findViewById<EditText>(R.id.etDetallePlato)
        val etImagenUrl = findViewById<EditText>(R.id.etImagenUrl)
        val selectorSeccion = findViewById<AutoCompleteTextView>(R.id.actvSeccionPlato)
        val tvModoFormulario = findViewById<TextView>(R.id.tvModoFormularioPlato)
        val tvGuarnicion = findViewById<TextView>(R.id.tvGuarnicion)
        val tvGuarnicionHint = findViewById<TextView>(R.id.tvGuarnicionHint)
        val layoutGuarnicionChecks = findViewById<LinearLayout>(R.id.layoutGuarnicionChecks)
        val cbGuarnicionSi = findViewById<CheckBox>(R.id.cbGuarnicionSi)
        val cbGuarnicionNo = findViewById<CheckBox>(R.id.cbGuarnicionNo)
        val btnGuardarPlato = findViewById<Button>(R.id.btnCrearPlato)
        val btnCancelarEdicion = findViewById<Button>(R.id.btnCancelarEdicionPlato)
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenuAdmin)
        val listSecciones = findViewById<ListView>(R.id.listSeccionesMenu)
        val tvEmptyMenuList = findViewById<TextView>(R.id.tvEmptyMenuList)

        val secciones = MenuRepository.seccionesPermitidas()
        var platosListado: List<AdminDishListItem> = emptyList()
        var editingDishId: String? = null

        listSecciones.emptyView = tvEmptyMenuList

        selectorSeccion.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, secciones))
        selectorSeccion.threshold = 0
        selectorSeccion.setOnClickListener { selectorSeccion.showDropDown() }
        selectorSeccion.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) selectorSeccion.showDropDown()
        }

        cbGuarnicionSi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbGuarnicionNo.isChecked = false
        }
        cbGuarnicionNo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbGuarnicionSi.isChecked = false
        }

        fun seleccionarGuarnicion(value: Boolean?) {
            cbGuarnicionSi.isChecked = value == true
            cbGuarnicionNo.isChecked = value == false
        }

        fun actualizarVisibilidadGuarnicion() {
            val esPrincipal = MenuIdentity.normalizeSectionId(
                rawSectionId = null,
                rawSectionName = selectorSeccion.text.toString().trim()
            ) == MenuIdentity.SECTION_MAIN
            val visibility = if (esPrincipal) View.VISIBLE else View.GONE
            tvGuarnicion.visibility = visibility
            tvGuarnicionHint.visibility = visibility
            layoutGuarnicionChecks.visibility = visibility
            if (!esPrincipal) {
                seleccionarGuarnicion(null)
            }
        }

        fun salirModoEdicion() {
            editingDishId = null
            tvModoFormulario.text = getString(R.string.admin_menu_crear_plato)
            btnGuardarPlato.text = getString(R.string.crear_plato)
            btnCancelarEdicion.visibility = View.GONE
            etNombrePlato.setText("")
            etDetallePlato.setText("")
            etImagenUrl.setText("")
            selectorSeccion.setText(secciones.firstOrNull().orEmpty(), false)
            seleccionarGuarnicion(null)
            actualizarVisibilidadGuarnicion()
        }

        fun cargarPlatoEnFormulario(item: AdminDishListItem) {
            editingDishId = item.dish.id
            tvModoFormulario.text = getString(R.string.admin_menu_editar_plato)
            btnGuardarPlato.text = getString(R.string.guardar_cambios_plato)
            btnCancelarEdicion.visibility = View.VISIBLE
            selectorSeccion.setText(item.sectionName, false)
            etNombrePlato.setText(item.dish.nombre)
            etDetallePlato.setText(item.dish.detalle)
            etImagenUrl.setText(item.dish.imageUrl)
            actualizarVisibilidadGuarnicion()
            seleccionarGuarnicion(item.dish.guarnicion.takeIf { item.sectionId == MenuIdentity.SECTION_MAIN })
        }

        fun refrescarListadoSecciones() {
            MenuRepository.cargarSecciones { ok, loadedSections ->
                runOnUiThread {
                    platosListado = loadedSections.flatMap { section ->
                        section.opciones.map { dish ->
                            AdminDishListItem(
                                sectionId = section.id,
                                sectionName = section.nombre,
                                dish = dish
                            )
                        }
                    }

                    val items = platosListado.map { item ->
                        val detalle = item.dish.detalle.takeIf { it.isNotBlank() }
                            ?: getString(R.string.admin_menu_list_detail_empty)
                        val subtituloDetalle = if (item.sectionId == MenuIdentity.SECTION_MAIN) {
                            if (item.dish.guarnicion) {
                                listOf(detalle, getString(R.string.admin_menu_guarnicion_si)).joinToString(" - ")
                            } else {
                                listOf(detalle, getString(R.string.admin_menu_guarnicion_no)).joinToString(" - ")
                            }
                        } else {
                            detalle
                        }
                        mapOf(
                            "title" to item.dish.nombre,
                            "subtitle" to getString(
                                R.string.admin_menu_list_subtitle,
                                item.sectionName,
                                subtituloDetalle
                            )
                        )
                    }
                    listSecciones.adapter = SimpleAdapter(
                        this,
                        items,
                        R.layout.item_admin_menu_dish,
                        arrayOf("title", "subtitle"),
                        intArrayOf(R.id.tvDishTitle, R.id.tvDishSubtitle)
                    )

                    if (!ok) {
                        Toast.makeText(this, R.string.error_cargar_menu, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        selectorSeccion.setOnItemClickListener { _, _, _, _ -> actualizarVisibilidadGuarnicion() }
        listSecciones.setOnItemClickListener { _, _, position, _ ->
            platosListado.getOrNull(position)?.let(::cargarPlatoEnFormulario)
        }
        btnCancelarEdicion.setOnClickListener { salirModoEdicion() }

        salirModoEdicion()
        refrescarListadoSecciones()

        btnGuardarPlato.setOnClickListener {
            val seccion = selectorSeccion.text.toString().trim()
            val nombre = etNombrePlato.text.toString().trim()
            val detalle = etDetallePlato.text.toString().trim()
            val imageUrl = etImagenUrl.text.toString().trim()

            if (seccion.isBlank() || nombre.isBlank()) {
                Toast.makeText(this, R.string.error_plato_invalido, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val guarnicion = if (
                MenuIdentity.normalizeSectionId(rawSectionId = null, rawSectionName = seccion) == MenuIdentity.SECTION_MAIN
            ) {
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

            val currentEditingDishId = editingDishId
            val action: (((Boolean) -> Unit) -> Unit) = if (currentEditingDishId == null) {
                { onComplete ->
                    MenuRepository.agregarPlato(
                        seccion = seccion,
                        nombre = nombre,
                        detalle = detalle,
                        imageUrl = imageUrl,
                        guarnicion = guarnicion,
                        onComplete = onComplete
                    )
                }
            } else {
                { onComplete ->
                    MenuRepository.actualizarPlato(
                        dishId = currentEditingDishId,
                        seccion = seccion,
                        nombre = nombre,
                        detalle = detalle,
                        imageUrl = imageUrl,
                        guarnicion = guarnicion,
                        onComplete = onComplete
                    )
                }
            }

            action { ok ->
                runOnUiThread {
                    if (!ok) {
                        val errorRes = if (currentEditingDishId == null) {
                            R.string.error_guardar_menu
                        } else {
                            R.string.error_actualizar_menu
                        }
                        Toast.makeText(this, errorRes, Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }

                    salirModoEdicion()
                    refrescarListadoSecciones()
                    val messageRes = if (currentEditingDishId == null) {
                        R.string.mensaje_plato_agregado
                    } else {
                        R.string.mensaje_plato_actualizado
                    }
                    Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
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

private data class AdminDishListItem(
    val sectionId: String,
    val sectionName: String,
    val dish: MenuDish
)
