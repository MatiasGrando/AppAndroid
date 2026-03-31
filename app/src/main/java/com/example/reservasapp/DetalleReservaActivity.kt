package com.example.reservasapp

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.reservasapp.booking.BookingConfirmationNavigator
import com.example.reservasapp.booking.BookingConfirmationRoute
import com.example.reservasapp.booking.BookingDetailEntry
import com.example.reservasapp.booking.BookingDetailEntryResolution
import com.example.reservasapp.booking.BookingFlowService
import com.example.reservasapp.booking.BookingMenuCoordinator
import com.example.reservasapp.booking.BookingSectionEvent
import com.example.reservasapp.booking.BookingSectionNavigation
import com.example.reservasapp.booking.BookingSectionNavigator
import com.example.reservasapp.booking.BookingSectionRenderState
import com.example.reservasapp.branding.BrandingConfig
import com.example.reservasapp.branding.AppRuntime
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de composicion del pedido: guia la seleccion por secciones y prepara la confirmacion final.
 */
class DetalleReservaActivity : BaseActivity() {

    companion object {
        const val EXTRA_DATE_MILLIS = "extra_date_millis"
        const val EXTRA_RESERVA_ID = "extra_reserva_id"

        fun createIntent(context: Context, dateMillis: Long): Intent {
            val navigation = detalleReservaNavigationForCreate(dateMillis)
            return Intent(context, DetalleReservaActivity::class.java).apply {
                putExtra(EXTRA_DATE_MILLIS, navigation.selectedDateMillis)
            }
        }

        fun editIntent(context: Context, reserva: Reserva): Intent {
            val navigation = detalleReservaNavigationForEdit(reserva)
            return Intent(context, DetalleReservaActivity::class.java).apply {
                putExtra(EXTRA_RESERVA_ID, navigation.reservaId)
                putExtra(EXTRA_DATE_MILLIS, navigation.selectedDateMillis)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAuthenticatedSession()) {
            return
        }

        val entry = resolveEntryPoint() ?: run {
            finish()
            return
        }

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
        val branding = AppRuntime.branding

        titleText.setText(branding.homeTitleRes)
        title = getString(branding.appNameRes)

        val reservaEnEdicion = entry.reservaEnEdicion
        val selectedDateMillis = entry.selectedDateMillis
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = formatter.format(Date(selectedDateMillis))
        dateText.text = getString(R.string.fecha_seleccionada, fechaFormateada)

        // Detalle ya consume el coordinador de menu en forma directa para que FlowService
        // siga acotado a validaciones de entrada y confirmacion final.
        val sectionNavigator = BookingSectionNavigator(
            initialSections = BookingMenuCoordinator.obtenerMenuCache(),
            initialSelections = reservaEnEdicion?.selecciones ?: emptyMap()
        )

        val pagerAdapter = MenuSectionsPagerAdapter(
            sections = sectionNavigator.currentSections,
            selections = sectionNavigator.currentSelections
        ) { sectionId, selectedOptionId, isDoubleTap ->
            val event = sectionNavigator.onOptionSelected(sectionId, selectedOptionId, isDoubleTap)
            aplicarEstadoSecciones(
                renderState = event.renderState,
                tabLayout = tabLayout,
                viewPager = viewPager,
                btnContinuar = btnContinuar,
                secciones = sectionNavigator.currentSections,
                event = event
            )

            if (event.shouldAutoAdvance) {
                btnContinuar.post {
                    if (btnContinuar.isEnabled) {
                        btnContinuar.performClick()
                    }
                }
            }
        }

        viewPager.adapter = pagerAdapter
        viewPager.isUserInputEnabled = false

        pagerAdapter.updateTheme(MenuThemeRegistry.palette())

        applyMenuTheme(
            branding = branding,
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
            tab.text = sectionNavigator.currentSections.getOrNull(position)?.nombre.orEmpty()
        }
        tabMediator.attach()

        bloquearInteraccionManualTabs(tabLayout)
        aplicarEstadoSecciones(
            renderState = sectionNavigator.currentRenderState(),
            tabLayout = tabLayout,
            viewPager = viewPager,
            btnContinuar = btnContinuar,
            secciones = sectionNavigator.currentSections
        )

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                aplicarEstadoSecciones(
                    renderState = sectionNavigator.onPageSelected(position),
                    tabLayout = tabLayout,
                    viewPager = viewPager,
                    btnContinuar = btnContinuar,
                    secciones = sectionNavigator.currentSections
                )
            }
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (val navigation = sectionNavigator.onBackPressed()) {
                    BookingSectionNavigation.Exit -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }

                    is BookingSectionNavigation.Section -> {
                        navegarSiExiste(viewPager, navigation.index)
                    }

                    BookingSectionNavigation.Stay,
                    BookingSectionNavigation.Confirmation -> Unit
                }
            }
        })

        BookingMenuCoordinator.cargarMenuParaDetalle(selectedDateMillis) { ok, loadedSections ->
            runOnUiThread {
                pagerAdapter.updateSections(loadedSections)

                val event = sectionNavigator.onSectionsReloaded(loadedSections)
                viewPager.setCurrentItem(0, false)

                bloquearInteraccionManualTabs(tabLayout)
                aplicarEstadoSecciones(
                    renderState = event.renderState,
                    tabLayout = tabLayout,
                    viewPager = viewPager,
                    btnContinuar = btnContinuar,
                    secciones = sectionNavigator.currentSections,
                    event = event
                )

                if (!ok) {
                    Toast.makeText(this, R.string.error_cargar_menu, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnContinuar.setOnClickListener {
            when (val navigation = sectionNavigator.onContinue()) {
                is BookingSectionNavigation.Section -> {
                    val renderState = sectionNavigator.currentRenderState()
                    aplicarEstadoSecciones(
                        renderState = renderState,
                        tabLayout = tabLayout,
                        viewPager = viewPager,
                        btnContinuar = btnContinuar,
                        secciones = sectionNavigator.currentSections,
                        event = BookingSectionEvent(
                            renderState = renderState,
                            shouldRefreshSideSection = true
                        )
                    )
                    navegarSiExiste(viewPager, navigation.index)
                }

                BookingSectionNavigation.Confirmation -> {
                    navegarAConfirmacion(
                        BookingConfirmationRoute(
                            selectedDateMillis = selectedDateMillis,
                            reservaEnEdicion = reservaEnEdicion,
                            selecciones = sectionNavigator.currentSelections
                        )
                    )
                }

                BookingSectionNavigation.Stay,
                BookingSectionNavigation.Exit -> Unit
            }
        }
    }

    private fun bloquearInteraccionManualTabs(tabLayout: TabLayout) {
        tabLayout.touchables.forEach { view ->
            view.setOnClickListener { }
        }
    }

    /**
     * Interpreta el Intent de entrada y descarta accesos fuera de la ventana habilitada.
     */
    private fun resolveEntryPoint(): BookingDetailEntry? {
        return when (
            val resolution = BookingFlowService.resolverEntradaDetalle(
                reservaId = intent.getStringExtra(EXTRA_RESERVA_ID).orEmpty(),
                selectedDateMillis = intent.getLongExtra(EXTRA_DATE_MILLIS, -1L)
                    .takeIf { intent.hasExtra(EXTRA_DATE_MILLIS) },
                hasDateExtra = intent.hasExtra(EXTRA_DATE_MILLIS)
            )
        ) {
            is BookingDetailEntryResolution.Invalid -> {
                Toast.makeText(this, resolution.messageRes, Toast.LENGTH_SHORT).show()
                null
            }

            is BookingDetailEntryResolution.Valid -> resolution.entry
        }
    }

    /**
     * Detalle entrega contexto minimo del flujo; el armado del payload final vive en el navigator.
     */
    private fun navegarAConfirmacion(route: BookingConfirmationRoute) {
        startActivity(BookingConfirmationNavigator.createIntent(this, route))
    }

    private fun actualizarEstadoTabGuarnicion(
        tabLayout: TabLayout,
        secciones: List<MenuSection>,
        guarnicionesHabilitadas: Boolean
    ) {
        val indiceGuarnicion = obtenerIndiceGuarnicion(secciones)
        if (indiceGuarnicion == -1) return

        val tabGuarnicion = tabLayout.getTabAt(indiceGuarnicion) ?: return
        tabGuarnicion.view.alpha = if (guarnicionesHabilitadas) 1f else 0.4f
    }

    private fun refrescarSeccionGuarniciones(viewPager: ViewPager2, secciones: List<MenuSection>) {
        val indiceGuarnicion = obtenerIndiceGuarnicion(secciones)
        if (indiceGuarnicion == -1) return
        viewPager.adapter?.notifyItemChanged(indiceGuarnicion)
    }

    private fun actualizarEstadoBotonContinuar(
        btnContinuar: Button,
        isEnabled: Boolean
    ) {
        btnContinuar.isEnabled = isEnabled
        btnContinuar.alpha = if (isEnabled) 1f else 0.5f
        btnContinuar.text = getString(R.string.continuar)
    }

    /**
     * Aplica en lote el estado visual derivado de la navegacion para que los callbacks no repitan wiring UI.
     */
    private fun aplicarEstadoSecciones(
        renderState: BookingSectionRenderState,
        tabLayout: TabLayout,
        viewPager: ViewPager2,
        btnContinuar: Button,
        secciones: List<MenuSection>,
        event: BookingSectionEvent? = null
    ) {
        actualizarEstadoTabGuarnicion(tabLayout, secciones, renderState.isSideEnabled)
        if (event?.shouldRefreshSideSection == true) {
            refrescarSeccionGuarniciones(viewPager, secciones)
        }
        actualizarEstadoBotonContinuar(btnContinuar, renderState.isContinueEnabled)
    }

    private fun obtenerIndiceGuarnicion(secciones: List<MenuSection>): Int {
        return secciones.indexOfFirst { it.id == MenuIdentity.SECTION_SIDE }
    }

    private fun navegarSiExiste(viewPager: ViewPager2, index: Int) {
        if (index >= 0 && index < (viewPager.adapter?.itemCount ?: 0)) {
            viewPager.setCurrentItem(index, true)
        }
    }

    private fun applyMenuTheme(
        branding: BrandingConfig,
        root: View,
        header: LinearLayout,
        bottomBar: LinearLayout,
        titleText: TextView,
        dateText: TextView,
        tabLayout: TabLayout,
        selectionHint: TextView,
        btnContinuar: Button
    ) {
        val titleColor = ContextCompat.getColor(this, branding.confirmationTitleColorRes)
        val bodyColor = ContextCompat.getColor(this, branding.confirmationBodyTextColorRes)
        val cardColor = ContextCompat.getColor(this, branding.confirmationCardBackgroundColorRes)
        val strokeColor = ContextCompat.getColor(this, branding.confirmationCardStrokeColorRes)

        root.setBackgroundResource(branding.homeBackgroundRes)
        header.setBackgroundColor(cardColor)
        bottomBar.setBackgroundColor(cardColor)

        titleText.setTextColor(titleColor)
        dateText.setTextColor(bodyColor)
        selectionHint.setTextColor(titleColor)

        tabLayout.setSelectedTabIndicatorColor(strokeColor)
        tabLayout.setTabTextColors(bodyColor, titleColor)

        btnContinuar.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, branding.primaryActionColorRes)
        )
        btnContinuar.setTextColor(ContextCompat.getColor(this, branding.actionTextColorRes))
    }
}

private class MenuSectionsPagerAdapter(
    private var sections: List<MenuSection>,
    private val selections: Map<String, String?>,
    private val onOptionSelected: (sectionId: String, selectedOptionId: String, isDoubleTap: Boolean) -> Unit
) : RecyclerView.Adapter<MenuSectionsPagerAdapter.SectionPageViewHolder>() {

    private var currentPalette = MenuThemeRegistry.palette()

    fun updateSections(newSections: List<MenuSection>) {
        val previousSize = sections.size
        sections = newSections
        when {
            previousSize == 0 && newSections.isNotEmpty() -> notifyItemRangeInserted(0, newSections.size)
            newSections.isEmpty() && previousSize > 0 -> notifyItemRangeRemoved(0, previousSize)
            previousSize == newSections.size -> notifyItemRangeChanged(0, newSections.size)
            previousSize < newSections.size -> {
                notifyItemRangeChanged(0, previousSize)
                notifyItemRangeInserted(previousSize, newSections.size - previousSize)
            }

            else -> {
                notifyItemRangeChanged(0, newSections.size)
                notifyItemRangeRemoved(newSections.size, previousSize - newSections.size)
            }
        }
    }

    fun updateTheme(palette: MenuThemePalette) {
        currentPalette = palette
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SectionPageViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_section_page, parent, false)
        return SectionPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionPageViewHolder, position: Int) {
        val section = sections[position]
        val items = MenuVisualRepository.buildItemsForSection(section.opciones)

        holder.bind(items, selections[section.id], currentPalette) { selectionEvent ->
            onOptionSelected(section.id, selectionEvent.option.id, selectionEvent.isDoubleTap)
        }
    }

    override fun getItemCount(): Int = sections.size

    class SectionPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
