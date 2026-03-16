package com.example.reservasapp

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleReservaActivity : BaseActivity() {

    private var shouldAutoContinueOnNextSelection = false

    companion object {
        const val EXTRA_DATE_MILLIS = "extra_date_millis"
        const val EXTRA_RESERVA_ID = "extra_reserva_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_reserva)

        val dateText = findViewById<TextView>(R.id.tvFechaSeleccionada)
        val titleText = findViewById<TextView>(R.id.tvTituloDetalle)
        val tabLayout = findViewById<TabLayout>(R.id.tabSections)
        val viewPager = findViewById<ViewPager2>(R.id.viewPagerSections)
        val btnContinuar = findViewById<Button>(R.id.btnConfirmar)
        val root = findViewById<View>(R.id.rootDetalleReserva)
        val header = findViewById<LinearLayout>(R.id.header)
        val bottomBar = findViewById<LinearLayout>(R.id.bottomBar)
        val selectionHint = findViewById<TextView>(R.id.tvSeleccionHint)

        val reservaId = intent.getStringExtra(EXTRA_RESERVA_ID).orEmpty()
        val reservaEnEdicion = if (reservaId.isNotBlank()) {
            ReservasRepository.obtenerReservaPorId(reservaId)
        } else {
            null
        }

        if (reservaId.isNotBlank() && reservaEnEdicion == null) {
            Toast.makeText(this, R.string.error_cargar_reservas, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val selectedDateMillis = reservaEnEdicion?.fechaMillis
            ?: intent.getLongExtra(EXTRA_DATE_MILLIS, System.currentTimeMillis())
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = formatter.format(Date(selectedDateMillis))
        dateText.text = getString(R.string.fecha_seleccionada, fechaFormateada)

        var secciones = MenuRepository.obtenerSeccionesCache()
        val selecciones = linkedMapOf<String, String?>().apply {
            putAll(reservaEnEdicion?.selecciones ?: emptyMap())
        }
        var currentSectionIndex = 0
        var guarnicionesHabilitadas = estaGuarnicionHabilitada(secciones, selecciones)

        val pagerAdapter = MenuSectionsPagerAdapter(
            sections = secciones,
            selections = selecciones
        ) { sectionName, selectedOption, isDoubleTap ->
            selecciones[sectionName] = selectedOption

            shouldAutoContinueOnNextSelection = isDoubleTap

            if (sectionName.equals("Plato principal", ignoreCase = true)) {
                guarnicionesHabilitadas = false
                selecciones["Guarniciones"] = null
                actualizarEstadoTabGuarnicion(tabLayout, secciones, guarnicionesHabilitadas)
                refrescarSeccionGuarniciones(viewPager, secciones)
            }

            actualizarEstadoBotonContinuar(btnContinuar, secciones, selecciones, currentSectionIndex)

            if (shouldAutoContinueOnNextSelection) {
                btnContinuar.post {
                    if (shouldAutoContinueOnNextSelection && btnContinuar.isEnabled) {
                        shouldAutoContinueOnNextSelection = false
                        btnContinuar.performClick()
                    }
                }
            }
        }

        viewPager.adapter = pagerAdapter
        viewPager.isUserInputEnabled = false

        val currentTheme = if (AppThemePreference.isDarkModeEnabled(this)) {
            MenuVisualTheme.DARK
        } else {
            MenuVisualTheme.LIGHT
        }
        val initialPalette = MenuThemeRegistry.palette(currentTheme)
        pagerAdapter.updateTheme(initialPalette)

        applyMenuTheme(
            palette = initialPalette,
            root = root,
            header = header,
            bottomBar = bottomBar,
            titleText = titleText,
            dateText = dateText,
            tabLayout = tabLayout,
            selectionHint = selectionHint,
            btnContinuar = btnContinuar
        )

        val tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = secciones.getOrNull(position)?.nombre.orEmpty()
        }
        tabMediator.attach()

        bloquearInteraccionManualTabs(tabLayout)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentSectionIndex = position
                actualizarEstadoBotonContinuar(btnContinuar, secciones, selecciones, currentSectionIndex)
            }
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentSectionIndex == 0) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    return
                }

                val indicePrincipal = obtenerIndiceSeccion(secciones, "Plato principal")
                val indiceGuarnicion = obtenerIndiceSeccion(secciones, "Guarniciones")
                val destino = when {
                    currentSectionIndex > indiceGuarnicion && !guarnicionesHabilitadas && indicePrincipal != -1 -> indicePrincipal
                    else -> currentSectionIndex - 1
                }
                navegarSiExiste(viewPager, destino)
            }
        })

        MenuRepository.cargarSecciones(selectedDateMillis) { ok, loadedSections ->
            runOnUiThread {
                secciones = loadedSections
                pagerAdapter.updateSections(secciones)
                pagerAdapter.notifyDataSetChanged()

                guarnicionesHabilitadas = estaGuarnicionHabilitada(secciones, selecciones)
                if (!guarnicionesHabilitadas) {
                    selecciones["Guarniciones"] = null
                    refrescarSeccionGuarniciones(viewPager, secciones)
                }

                currentSectionIndex = 0
                viewPager.setCurrentItem(0, false)

                actualizarEstadoTabGuarnicion(tabLayout, secciones, guarnicionesHabilitadas)
                bloquearInteraccionManualTabs(tabLayout)
                actualizarEstadoBotonContinuar(btnContinuar, secciones, selecciones, currentSectionIndex)

                if (!ok) {
                    Toast.makeText(this, R.string.error_cargar_menu, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnContinuar.setOnClickListener {
            shouldAutoContinueOnNextSelection = false
            val section = secciones.getOrNull(currentSectionIndex) ?: return@setOnClickListener
            val seleccionActual = selecciones[section.nombre]
            if (seleccionActual.isNullOrBlank()) return@setOnClickListener

            if (section.nombre.equals("Plato principal", ignoreCase = true)) {
                val platoPrincipal = section.opciones.firstOrNull { it.nombre == seleccionActual }
                guarnicionesHabilitadas = platoPrincipal?.guarnicion == true

                if (!guarnicionesHabilitadas) {
                    selecciones["Guarniciones"] = null
                    refrescarSeccionGuarniciones(viewPager, secciones)
                }

                actualizarEstadoTabGuarnicion(tabLayout, secciones, guarnicionesHabilitadas)

                val indiceDestino = if (guarnicionesHabilitadas) {
                    obtenerIndiceSeccion(secciones, "Guarniciones")
                } else {
                    obtenerIndiceSeccion(secciones, "Postres")
                }
                navegarSiExiste(viewPager, indiceDestino)
                return@setOnClickListener
            }

            if (currentSectionIndex < secciones.lastIndex) {
                val siguiente = currentSectionIndex + 1
                navegarSiExiste(viewPager, siguiente)
            } else {
                val seleccionesFinales = selecciones
                    .filterValues { !it.isNullOrBlank() }
                    .mapValues { it.value.orEmpty() }

                val intentConfirmacion = Intent(this, ConfirmacionReservaActivity::class.java).apply {
                    putExtra(ConfirmacionReservaActivity.EXTRA_FECHA, fechaFormateada)
                    putExtra(ConfirmacionReservaActivity.EXTRA_FECHA_MILLIS, selectedDateMillis)
                    putExtra(ConfirmacionReservaActivity.EXTRA_DETALLE, ReservasRepository.formatearSelecciones(seleccionesFinales))
                    putExtra(
                        ConfirmacionReservaActivity.EXTRA_SELECCIONES_PENDIENTES,
                        HashMap(seleccionesFinales)
                    )
                }

                if (reservaEnEdicion != null) {
                    intentConfirmacion.putExtra(ConfirmacionReservaActivity.EXTRA_RESERVA_ID, reservaEnEdicion.id)
                    intentConfirmacion.putExtra(ConfirmacionReservaActivity.EXTRA_ES_EDICION, true)
                    startActivity(intentConfirmacion)
                    return@setOnClickListener
                }

                startActivity(intentConfirmacion)
            }
        }
    }

    private fun bloquearInteraccionManualTabs(tabLayout: TabLayout) {
        tabLayout.touchables.forEach { view ->
            view.setOnTouchListener { _, _ -> true }
            view.isClickable = false
        }
    }

    private fun actualizarEstadoTabGuarnicion(
        tabLayout: TabLayout,
        secciones: List<MenuSection>,
        guarnicionesHabilitadas: Boolean
    ) {
        val indiceGuarnicion = obtenerIndiceSeccion(secciones, "Guarniciones")
        if (indiceGuarnicion == -1) return

        val tabGuarnicion = tabLayout.getTabAt(indiceGuarnicion) ?: return
        tabGuarnicion.view.alpha = if (guarnicionesHabilitadas) 1f else 0.4f
    }

    private fun refrescarSeccionGuarniciones(viewPager: ViewPager2, secciones: List<MenuSection>) {
        val indiceGuarnicion = obtenerIndiceSeccion(secciones, "Guarniciones")
        if (indiceGuarnicion == -1) return
        viewPager.adapter?.notifyItemChanged(indiceGuarnicion)
    }

    private fun actualizarEstadoBotonContinuar(
        btnContinuar: Button,
        secciones: List<MenuSection>,
        selecciones: Map<String, String?>,
        currentSectionIndex: Int
    ) {
        val section = secciones.getOrNull(currentSectionIndex)
        val isSelected = section != null && !selecciones[section.nombre].isNullOrBlank()
        btnContinuar.isEnabled = isSelected
        btnContinuar.alpha = if (isSelected) 1f else 0.5f
        btnContinuar.text = getString(R.string.continuar)
    }

    private fun obtenerIndiceSeccion(secciones: List<MenuSection>, nombre: String): Int {
        return secciones.indexOfFirst { it.nombre.equals(nombre, ignoreCase = true) }
    }

    private fun navegarSiExiste(viewPager: ViewPager2, index: Int) {
        if (index >= 0 && index < (viewPager.adapter?.itemCount ?: 0)) {
            viewPager.setCurrentItem(index, true)
        }
    }

    private fun estaGuarnicionHabilitada(
        secciones: List<MenuSection>,
        selecciones: Map<String, String?>
    ): Boolean {
        val principal = secciones.firstOrNull { it.nombre.equals("Plato principal", ignoreCase = true) }
            ?: return false
        val seleccionPrincipal = selecciones[principal.nombre] ?: return false
        return principal.opciones.firstOrNull { it.nombre == seleccionPrincipal }?.guarnicion == true
    }

    private fun applyMenuTheme(
        palette: MenuThemePalette,
        root: View,
        header: LinearLayout,
        bottomBar: LinearLayout,
        titleText: TextView,
        dateText: TextView,
        tabLayout: TabLayout,
        selectionHint: TextView,
        btnContinuar: Button
    ) {
        root.setBackgroundResource(palette.backgroundDrawableRes)
        header.setBackgroundColor(palette.panelBackgroundColor)
        bottomBar.setBackgroundColor(palette.panelBackgroundColor)

        titleText.setTextColor(palette.titleColor)
        dateText.setTextColor(palette.bodyTextColor)
        selectionHint.setTextColor(palette.hintTextColor)

        tabLayout.setSelectedTabIndicatorColor(palette.tabIndicatorColor)
        tabLayout.setTabTextColors(palette.tabUnselectedColor, palette.tabSelectedColor)

        btnContinuar.backgroundTintList = ColorStateList.valueOf(palette.buttonBackgroundColor)
        btnContinuar.setTextColor(palette.buttonTextColor)
    }
}

private class MenuSectionsPagerAdapter(
    private var sections: List<MenuSection>,
    private val selections: Map<String, String?>,
    private val onOptionSelected: (sectionName: String, selectedOption: String, isDoubleTap: Boolean) -> Unit
) : RecyclerView.Adapter<MenuSectionsPagerAdapter.SectionPageViewHolder>() {

    private var currentPalette = MenuThemeRegistry.palette(MenuVisualTheme.DARK)

    fun updateSections(newSections: List<MenuSection>) {
        sections = newSections
    }

    fun updateTheme(palette: MenuThemePalette) {
        currentPalette = palette
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SectionPageViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_section_page, parent, false)
        return SectionPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionPageViewHolder, position: Int) {
        val section = sections[position]
        val items = MenuVisualRepository.buildItemsForSection(section.opciones)

        holder.bind(items, selections[section.nombre], currentPalette) { selectionEvent ->
            onOptionSelected(section.nombre, selectionEvent.option.name, selectionEvent.isDoubleTap)
        }
    }

    override fun getItemCount(): Int = sections.size

    class SectionPageViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val recycler = itemView.findViewById<RecyclerView>(R.id.recyclerSectionOptions)
        private var onSelection: ((OptionSelectionEvent) -> Unit)? = null
        private val adapter = MenuOptionAdapter(emptyList()) { selectedOption, isDoubleTap ->
            onSelection?.invoke(OptionSelectionEvent(selectedOption, isDoubleTap))
        }

        fun bind(
            items: List<MenuItemOption>,
            selectedName: String?,
            palette: MenuThemePalette,
            onSelection: (OptionSelectionEvent) -> Unit
        ) {
            this.onSelection = onSelection
            adapter.updateTheme(palette)
            adapter.updateItems(items, selectedName)
        }

        init {
            recycler.layoutManager = LinearLayoutManager(itemView.context)
            recycler.adapter = adapter
        }
    }

    data class OptionSelectionEvent(
        val option: MenuItemOption,
        val isDoubleTap: Boolean
    )
}
