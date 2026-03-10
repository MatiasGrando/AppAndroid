package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleReservaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DATE_MILLIS = "extra_date_millis"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_reserva)

        val dateText = findViewById<TextView>(R.id.tvFechaSeleccionada)
        val tabLayout = findViewById<TabLayout>(R.id.tabSections)
        val viewPager = findViewById<ViewPager2>(R.id.viewPagerSections)
        val btnContinuar = findViewById<Button>(R.id.btnConfirmar)

        val selectedDateMillis = intent.getLongExtra(EXTRA_DATE_MILLIS, System.currentTimeMillis())
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = formatter.format(Date(selectedDateMillis))
        dateText.text = getString(R.string.fecha_seleccionada, fechaFormateada)

        var secciones = MenuRepository.obtenerSeccionesCache()
        val selecciones = linkedMapOf<String, String?>()
        var currentSectionIndex = 0
        var guarnicionesHabilitadas = false

        val pagerAdapter = MenuSectionsPagerAdapter(
            sections = secciones,
            selections = selecciones
        ) { sectionName, selectedOption ->
            selecciones[sectionName] = selectedOption

            if (sectionName.equals("Plato principal", ignoreCase = true)) {
                guarnicionesHabilitadas = false
                selecciones["Guarniciones"] = null
                actualizarEstadoTabGuarnicion(tabLayout, secciones, guarnicionesHabilitadas)
                refrescarSeccionGuarniciones(viewPager, secciones)
            }

            actualizarEstadoBotonContinuar(btnContinuar, secciones, selecciones, currentSectionIndex)
        }

        viewPager.adapter = pagerAdapter
        viewPager.isUserInputEnabled = false

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

        MenuRepository.cargarSecciones { ok, loadedSections ->
            runOnUiThread {
                secciones = loadedSections
                pagerAdapter.updateSections(secciones)
                pagerAdapter.notifyDataSetChanged()

                guarnicionesHabilitadas = false
                selecciones["Guarniciones"] = null
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
                ReservasRepository.agregarReserva(
                    fechaMillis = selectedDateMillis,
                    selecciones = selecciones
                        .filterValues { !it.isNullOrBlank() }
                        .mapValues { it.value.orEmpty() }
                ) { reserva ->
                    if (reserva == null) {
                        Toast.makeText(this, R.string.error_guardar_reserva, Toast.LENGTH_LONG).show()
                        return@agregarReserva
                    }

                    val resumen = ReservasRepository.formatearSelecciones(reserva.selecciones)
                    val intent = Intent(this, ConfirmacionReservaActivity::class.java).apply {
                        putExtra(ConfirmacionReservaActivity.EXTRA_FECHA, fechaFormateada)
                        putExtra(ConfirmacionReservaActivity.EXTRA_DETALLE, resumen)
                        putExtra(ConfirmacionReservaActivity.EXTRA_RESERVA_ID, reserva.id)
                    }
                    startActivity(intent)
                }
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
}

private class MenuSectionsPagerAdapter(
    private var sections: List<MenuSection>,
    private val selections: Map<String, String?>,
    private val onOptionSelected: (sectionName: String, selectedOption: String) -> Unit
) : RecyclerView.Adapter<MenuSectionsPagerAdapter.SectionPageViewHolder>() {

    fun updateSections(newSections: List<MenuSection>) {
        sections = newSections
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SectionPageViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_section_page, parent, false)
        return SectionPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionPageViewHolder, position: Int) {
        val section = sections[position]
        val items = MenuVisualRepository.buildItemsForSection(section.opciones)

        holder.adapter.updateItems(items, selections[section.nombre])
        holder.adapter.onSelection = { option ->
            onOptionSelected(section.nombre, option.name)
        }
    }

    override fun getItemCount(): Int = sections.size

    class SectionPageViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val recycler = itemView.findViewById<RecyclerView>(R.id.recyclerSectionOptions)
        val adapter = BindableMenuOptionAdapter(emptyList())

        init {
            recycler.layoutManager = LinearLayoutManager(itemView.context)
            recycler.adapter = adapter
        }
    }
}

private class BindableMenuOptionAdapter(
    items: List<MenuItemOption>
) : RecyclerView.Adapter<MenuOptionAdapter.MenuOptionViewHolder>() {

    private var itemsState: List<MenuItemOption> = items
    private var selectedPosition = RecyclerView.NO_POSITION
    var onSelection: ((MenuItemOption) -> Unit)? = null

    fun updateItems(newItems: List<MenuItemOption>, selectedName: String?) {
        itemsState = newItems
        selectedPosition = selectedName?.let { name ->
            itemsState.indexOfFirst { it.name == name }
        }?.takeIf { it >= 0 } ?: RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): MenuOptionAdapter.MenuOptionViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_option, parent, false)
        return MenuOptionAdapter.MenuOptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuOptionAdapter.MenuOptionViewHolder, position: Int) {
        val item = itemsState[position]
        holder.bind(item, position == selectedPosition)
        holder.itemView.setOnClickListener {
            val prev = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
            onSelection?.invoke(item)
        }
    }

    override fun getItemCount(): Int = itemsState.size
}
