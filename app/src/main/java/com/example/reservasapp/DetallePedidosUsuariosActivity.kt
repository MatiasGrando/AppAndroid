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

class DetallePedidosUsuariosActivity : BaseActivity() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", spanishDateLocale)
    private var selectedDateMillis: Long = Calendar.getInstance().clearTime().timeInMillis
    private var pendingCsvContent: String? = null

    private lateinit var btnSeleccionarFecha: Button
    private lateinit var btnGenerarDetalle: Button
    private lateinit var btnExportarCsv: Button
    private lateinit var tvDetalle: TextView

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

        setContentView(R.layout.activity_detalle_pedidos_usuarios)

        val tvFechaSeleccionada = findViewById<TextView>(R.id.tvFechaDetallePedidosSeleccionada)
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFechaDetallePedidos)
        btnGenerarDetalle = findViewById(R.id.btnGenerarDetallePedidos)
        btnExportarCsv = findViewById(R.id.btnExportarDetallePedidosCsv)
        tvDetalle = findViewById(R.id.tvDetallePedidosUsuarios)

        fun actualizarTextoFecha() {
            tvFechaSeleccionada.text = getString(
                R.string.pedidos_por_dia_fecha,
                dateFormatter.format(Date(selectedDateMillis))
            )
        }

        fun renderDetalle(rows: List<ReservasRepository.DetallePedidoUsuario>) {
            if (rows.isEmpty()) {
                tvDetalle.text = getString(R.string.detalle_pedidos_usuarios_sin_datos)
                return
            }

            val fechaTitulo = SimpleDateFormat("EEEE dd/MM", Locale("es", "ES"))
                .format(Date(selectedDateMillis))
                .uppercase(Locale("es", "ES"))

            val lines = mutableListOf(fechaTitulo)
            var empresaActual = ""

            rows.forEach { row ->
                if (row.empresa != empresaActual) {
                    empresaActual = row.empresa
                    lines += ""
                    lines += getString(R.string.detalle_pedidos_empresa, empresaActual)
                }

                val nombreCompleto = listOf(row.nombre, row.apellido)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { getString(R.string.detalle_pedidos_usuario_sin_nombre) }

                val guarnicion = row.guarnicion.trim()
                val tieneGuarnicion = guarnicion.isNotEmpty() && guarnicion != "-"

                lines += if (tieneGuarnicion) {
                    String.format(
                        Locale.getDefault(),
                        "%-18s %-24s %-16s %s",
                        nombreCompleto,
                        row.platoPrincipal,
                        guarnicion,
                        row.postre
                    )
                } else {
                    String.format(
                        Locale.getDefault(),
                        "%-18s %-24s %s",
                        nombreCompleto,
                        row.platoPrincipal,
                        row.postre
                    )
                }
            }

            tvDetalle.text = lines.joinToString("\n")
        }

        actualizarTextoFecha()

        btnSeleccionarFecha.setOnClickListener {
            createSpanishDatePickerDialog(selectedDateMillis) { selectedMillis ->
                selectedDateMillis = selectedMillis
                actualizarTextoFecha()
            }.show()
        }

        btnGenerarDetalle.setOnClickListener {
            setLoading(true)
            tvDetalle.text = getString(R.string.pedidos_por_dia_cargando)
            ReservasRepository.obtenerDetallePedidosPorFecha(selectedDateMillis) { ok, detalle ->
                runOnUiThread {
                    setLoading(false)
                    if (!ok) {
                        tvDetalle.text = getString(R.string.detalle_pedidos_usuarios_sin_datos)
                        Toast.makeText(
                            this,
                            R.string.error_cargar_detalle_pedidos_usuarios,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@runOnUiThread
                    }
                    renderDetalle(detalle)
                }
            }
        }

        btnExportarCsv.setOnClickListener {
            exportarDetalleCsv()
        }
    }

    private fun exportarDetalleCsv() {
        setLoading(true)
        Toast.makeText(this, R.string.admin_csv_export_loading, Toast.LENGTH_SHORT).show()

        ReservasRepository.obtenerDetallePedidosPorFecha(selectedDateMillis) { ok, detalle ->
            runOnUiThread {
                if (!ok) {
                    setLoading(false)
                    Toast.makeText(this, R.string.error_cargar_detalle_pedidos_usuarios, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                if (detalle.isEmpty()) {
                    setLoading(false)
                    Toast.makeText(this, R.string.detalle_pedidos_usuarios_sin_datos, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                pendingCsvContent = AdminCsvExportHelper.buildDetallePedidosCsv(selectedDateMillis, detalle)
                createDocumentLauncher.launch(AdminCsvExportHelper.buildDetallePedidosFileName(selectedDateMillis))
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
        btnGenerarDetalle.isEnabled = !loading
        btnExportarCsv.isEnabled = !loading
    }

    private fun Calendar.clearTime(): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
