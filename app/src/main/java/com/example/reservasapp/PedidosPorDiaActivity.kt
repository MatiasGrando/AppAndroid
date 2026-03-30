package com.example.reservasapp

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PedidosPorDiaActivity : BaseActivity() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", spanishDateLocale)
    private var selectedDateMillis: Long = Calendar.getInstance().clearTime().timeInMillis
    private var pendingCsvContent: String? = null

    private lateinit var btnSeleccionarFecha: Button
    private lateinit var btnGenerarResumen: Button
    private lateinit var btnExportarCsv: Button
    private lateinit var tvResumen: TextView

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri == null) {
            pendingCsvContent = null
            setLoading(false)
            Toast.makeText(this, R.string.admin_csv_export_cancelled, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        guardarCsvEnUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAdminAccess()) {
            return
        }

        setContentView(R.layout.activity_pedidos_por_dia)

        val tvFechaSeleccionada = findViewById<TextView>(R.id.tvFechaPedidosSeleccionada)
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFechaPedidos)
        btnGenerarResumen = findViewById(R.id.btnGenerarResumenPedidos)
        btnExportarCsv = findViewById(R.id.btnExportarResumenPedidosCsv)
        tvResumen = findViewById(R.id.tvResumenPedidos)

        fun actualizarTextoFecha() {
            tvFechaSeleccionada.text = getString(
                R.string.pedidos_por_dia_fecha,
                dateFormatter.format(Date(selectedDateMillis))
            )
        }

        fun renderResumen(groupedCounts: Map<String, Map<String, Int>>) {
            val preferredOrder = listOf(
                "Plato principal",
                "Guarniciones",
                "Postres"
            )

            val sectionOrder = preferredOrder + groupedCounts.keys.filterNot { it in preferredOrder }.sorted()
            val lines = mutableListOf<String>()

            var isFirstSection = true
            sectionOrder.forEach { section ->
                val dishes = groupedCounts[section].orEmpty()
                if (dishes.isEmpty()) return@forEach

                if (isFirstSection) {
                    lines += getString(R.string.pedidos_por_dia_header)
                    isFirstSection = false
                } else {
                    lines += ""
                    lines += section
                }

                dishes.toList()
                    .sortedBy { it.first }
                    .forEach { (dishName, count) ->
                        lines += String.format(Locale.getDefault(), "%-30s %d", dishName, count)
                    }
            }

            tvResumen.text = if (lines.isEmpty()) {
                getString(R.string.pedidos_por_dia_sin_datos)
            } else {
                lines.joinToString("\n")
            }
        }

        actualizarTextoFecha()

        btnSeleccionarFecha.setOnClickListener {
            createSpanishDatePickerDialog(selectedDateMillis) { selectedMillis ->
                selectedDateMillis = selectedMillis
                actualizarTextoFecha()
            }.show()
        }

        btnGenerarResumen.setOnClickListener {
            setLoading(true)
            tvResumen.text = getString(R.string.pedidos_por_dia_cargando)
            ReservasRepository.obtenerResumenPedidosPorFecha(selectedDateMillis) { ok, groupedCounts ->
                runOnUiThread {
                    setLoading(false)
                    if (!ok) {
                        tvResumen.text = getString(R.string.pedidos_por_dia_sin_datos)
                        Toast.makeText(this, R.string.error_cargar_pedidos_por_dia, Toast.LENGTH_SHORT)
                            .show()
                        return@runOnUiThread
                    }
                    renderResumen(groupedCounts)
                }
            }
        }

        btnExportarCsv.setOnClickListener {
            exportarResumenCsv()
        }
    }

    private fun exportarResumenCsv() {
        setLoading(true)
        Toast.makeText(this, R.string.admin_csv_export_loading, Toast.LENGTH_SHORT).show()

        ReservasRepository.obtenerResumenPedidosPorFecha(selectedDateMillis) { ok, groupedCounts ->
            runOnUiThread {
                if (!ok) {
                    setLoading(false)
                    Toast.makeText(this, R.string.error_cargar_pedidos_por_dia, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                if (groupedCounts.isEmpty()) {
                    setLoading(false)
                    Toast.makeText(this, R.string.pedidos_por_dia_sin_datos, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                pendingCsvContent = AdminCsvExportHelper.buildResumenPedidosCsv(selectedDateMillis, groupedCounts)
                createDocumentLauncher.launch(AdminCsvExportHelper.buildResumenPedidosFileName(selectedDateMillis))
            }
        }
    }

    private fun guardarCsvEnUri(uri: Uri) {
        val csvContent = pendingCsvContent
        if (csvContent.isNullOrEmpty()) {
            setLoading(false)
            Toast.makeText(this, R.string.admin_csv_export_save_error, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            AdminCsvExportHelper.writeCsv(contentResolver, uri, csvContent)
            Toast.makeText(this, R.string.admin_csv_export_success, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.admin_csv_export_save_error, Toast.LENGTH_SHORT).show()
        } finally {
            pendingCsvContent = null
            setLoading(false)
        }
    }

    private fun setLoading(loading: Boolean) {
        btnSeleccionarFecha.isEnabled = !loading
        btnGenerarResumen.isEnabled = !loading
        btnExportarCsv.isEnabled = !loading
    }

    private fun Calendar.clearTime(): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
