package com.example.reservasapp

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminArchivoExportacionActivity : BaseActivity() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", spanishDateLocale)
    private val fileDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var desdeMillis: Long? = null
    private var hastaMillis: Long = Calendar.getInstance().clearTime().timeInMillis
    private var retentionDays: Int? = null
    private var retentionLimitMillis: Long? = null
    private var lastArchivedMillis: Long? = null
    private var pendingCsvContent: String? = null
    private var pendingExportRows: List<ReservasRepository.ReservaExportable> = emptyList()
    private var pendingArchivoOperacion: ReservasRepository.ArchivoReservasOperacion? = null
    private var bookingConfig: BookingAvailabilityConfig = BookingAvailabilityRepository.obtenerConfiguracionActual()
    private var userChangedHasta = false

    private lateinit var tvDesde: TextView
    private lateinit var tvHasta: TextView
    private lateinit var tvLastArchived: TextView
    private lateinit var tvOverlapWarning: TextView
    private lateinit var tvRetention: TextView
    private lateinit var tvStatus: TextView
    private lateinit var retentionInput: EditText
    private lateinit var btnDesde: Button
    private lateinit var btnHasta: Button
    private lateinit var btnGenerar: Button
    private lateinit var btnGuardarRetention: Button

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri == null) {
            pendingArchivoOperacion?.id?.let { operacionId ->
                ReservasRepository.cancelarOperacionArchivoReservas(
                    operacionId,
                    getString(R.string.admin_export_log_cancelled)
                )
            }
            clearPendingExport()
            setLoading(false)
            tvStatus.text = getString(R.string.admin_export_status_cancelled)
            Toast.makeText(this, R.string.admin_export_status_cancelled, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        guardarCsvEnUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAdminAccess()) {
            return
        }

        setContentView(R.layout.activity_admin_archivo_exportacion)

        tvDesde = findViewById(R.id.tvAdminExportFrom)
        tvHasta = findViewById(R.id.tvAdminExportTo)
        tvLastArchived = findViewById(R.id.tvAdminExportLastArchived)
        tvOverlapWarning = findViewById(R.id.tvAdminExportOverlapWarning)
        tvRetention = findViewById(R.id.tvAdminExportRetention)
        tvStatus = findViewById(R.id.tvAdminExportStatus)
        retentionInput = findViewById(R.id.etAdminExportRetentionDays)
        btnDesde = findViewById(R.id.btnAdminExportFrom)
        btnHasta = findViewById(R.id.btnAdminExportTo)
        btnGenerar = findViewById(R.id.btnAdminExportGenerate)
        btnGuardarRetention = findViewById(R.id.btnAdminExportSaveRetention)

        renderSelectedRange()
        renderLastArchivedDate(null)
        loadExportConfiguration()
        loadLastArchivedDate()

        btnDesde.setOnClickListener {
            showDatePicker(desdeMillis ?: hastaMillis) { selectedMillis ->
                desdeMillis = selectedMillis
                renderSelectedRange()
            }
        }

        btnHasta.setOnClickListener {
            showDatePicker(hastaMillis) { selectedMillis ->
                userChangedHasta = true
                hastaMillis = selectedMillis
                renderSelectedRange()
            }
        }

        btnGenerar.setOnClickListener {
            exportarRango()
        }

        btnGuardarRetention.setOnClickListener {
            guardarRetentionPolicy()
        }
    }

    private fun loadLastArchivedDate() {
        ReservasRepository.obtenerUltimaFechaArchivadaExportada { ok, lastDateMillis ->
            runOnUiThread {
                renderLastArchivedDate(lastDateMillis)
                renderSelectedRange()
                if (!ok) {
                    Toast.makeText(this, R.string.admin_export_error_last_archived_load, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadExportConfiguration() {
        setRetentionControlsEnabled(false)
        BookingAvailabilityRepository.cargarConfiguracion { ok, config ->
            runOnUiThread {
                bookingConfig = config
                renderRetentionPolicy(config.archiveRetentionDays)
                setRetentionControlsEnabled(true)

                if (!ok) {
                    Toast.makeText(this, R.string.error_cargar_archive_retention, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun guardarRetentionPolicy() {
        val retentionInputValue = retentionInput.text.toString().trim()
        val archiveRetentionDays = if (retentionInputValue.isEmpty()) {
            null
        } else {
            retentionInputValue.toIntOrNull()
        }

        if (retentionInputValue.isNotEmpty() && (archiveRetentionDays == null || archiveRetentionDays < 0)) {
            Toast.makeText(this, R.string.error_guardar_archive_retention_days, Toast.LENGTH_SHORT).show()
            return
        }

        val sanitizedArchiveRetentionDays = sanitizeArchiveRetentionDays(archiveRetentionDays)
        setRetentionControlsEnabled(false)
        BookingAvailabilityRepository.guardarConfiguracion(
            enabledWeekdays = bookingConfig.enabledWeekdays,
            initialDelayDays = bookingConfig.initialDelayDays,
            windowLengthDays = bookingConfig.windowLengthDays,
            archiveRetentionDays = sanitizedArchiveRetentionDays
        ) { ok ->
            runOnUiThread {
                setRetentionControlsEnabled(true)
                if (!ok) {
                    Toast.makeText(this, R.string.error_guardar_archive_retention, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                bookingConfig = bookingConfig.copy(archiveRetentionDays = sanitizedArchiveRetentionDays)
                renderRetentionPolicy(sanitizedArchiveRetentionDays)
                Toast.makeText(this, R.string.admin_archive_retention_saved, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportarRango() {
        val validationError = validarRangoSeleccionado()
        if (validationError != null) {
            tvStatus.text = validationError
            Toast.makeText(this, validationError, Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        tvStatus.text = getString(R.string.admin_export_status_loading)

        val desde = desdeMillis ?: run {
            setLoading(false)
            return
        }

        ReservasRepository.obtenerReservasExportablesPorRango(desde, hastaMillis) { ok, rows ->
            runOnUiThread {
                if (!ok) {
                    setLoading(false)
                    tvStatus.text = getString(R.string.admin_export_error_load)
                    Toast.makeText(this, R.string.admin_export_error_load, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                if (rows.isEmpty()) {
                    setLoading(false)
                    tvStatus.text = getString(R.string.admin_export_status_empty)
                    Toast.makeText(this, R.string.admin_export_status_empty, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                if (!ReservasRepository.puedeArchivarCantidad(rows.size)) {
                    setLoading(false)
                    tvStatus.text = getString(
                        R.string.admin_export_error_archive_limit,
                        rows.size,
                        ReservasRepository.obtenerMaximoReservasArchivablesPorOperacion()
                    )
                    Toast.makeText(this, tvStatus.text, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                showArchiveConfirmation(rows, desde, hastaMillis)
            }
        }
    }

    private fun showArchiveConfirmation(
        rows: List<ReservasRepository.ReservaExportable>,
        desde: Long,
        hasta: Long
    ) {
        val rangeLabel = formatRange(desde, hasta)
        val overlapNote = if (buildOverlapWarningMessage(desde, hasta) != null) {
            getString(R.string.admin_export_confirm_archive_overlap_note)
        } else {
            ""
        }
        val message = getString(
            R.string.admin_export_confirm_archive_message,
            rangeLabel,
            rows.size
        ) + overlapNote

        AlertDialog.Builder(this)
            .setTitle(R.string.admin_export_confirm_archive_title)
            .setMessage(message)
            .setNegativeButton(R.string.admin_export_confirm_archive_cancel) { _, _ ->
                setLoading(false)
                tvStatus.text = getString(R.string.admin_export_status_pre_archive_cancelled)
                renderOverlapWarning()
                Toast.makeText(this, R.string.admin_export_status_pre_archive_cancelled, Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton(R.string.admin_export_confirm_archive_confirm) { _, _ ->
                iniciarGuardadoCsv(rows, desde, hasta)
            }
            .setOnCancelListener {
                setLoading(false)
                tvStatus.text = getString(R.string.admin_export_status_pre_archive_cancelled)
                renderOverlapWarning()
            }
            .show()
    }

    private fun iniciarGuardadoCsv(
        rows: List<ReservasRepository.ReservaExportable>,
        desde: Long,
        hasta: Long
    ) {
        val fileName = buildFileName(desde, hasta)
        pendingCsvContent = ReservasRepository.generarCsvReservas(rows)
        pendingExportRows = rows
        ReservasRepository.iniciarOperacionArchivoReservas(rows, desde, hasta, fileName) { started, operacion ->
            runOnUiThread {
                if (!started || operacion == null) {
                    clearPendingExport()
                    setLoading(false)
                    tvStatus.text = getString(R.string.admin_export_error_log_start)
                    Toast.makeText(this, R.string.admin_export_error_log_start, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                pendingArchivoOperacion = operacion
                tvStatus.text = getString(R.string.admin_export_status_saving)
                createDocumentLauncher.launch(fileName)
            }
        }
    }

    private fun guardarCsvEnUri(uri: Uri) {
        val csvContent = pendingCsvContent
        val archivoOperacion = pendingArchivoOperacion
        if (csvContent.isNullOrEmpty()) {
            setLoading(false)
            tvStatus.text = getString(R.string.admin_export_error_save)
            Toast.makeText(this, R.string.admin_export_error_save, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer ->
                if (writer == null) {
                    throw IOException("No se pudo abrir el destino del archivo")
                }
                writer.write(csvContent)
                writer.flush()
            }

            if (archivoOperacion == null) {
                throw IOException(getString(R.string.admin_export_error_log_missing))
            }

            ReservasRepository.marcarOperacionArchivoCsvGuardado(archivoOperacion.id) { ok ->
                runOnUiThread {
                    if (!ok) {
                        ReservasRepository.fallarOperacionArchivoReservas(
                            archivoOperacion.id,
                            getString(R.string.admin_export_log_csv_saved_update_failed)
                        )
                        tvStatus.text = getString(R.string.admin_export_error_log_update)
                        Toast.makeText(this, R.string.admin_export_error_log_update, Toast.LENGTH_SHORT).show()
                        clearPendingExport()
                        setLoading(false)
                        return@runOnUiThread
                    }

                    tvStatus.text = getString(R.string.admin_export_status_archiving)
                    archivarReservasExportadas()
                }
            }
        } catch (exception: Exception) {
            archivoOperacion?.id?.let { operacionId ->
                ReservasRepository.fallarOperacionArchivoReservas(
                    operacionId,
                    exception.message ?: getString(R.string.admin_export_log_save_failed)
                )
            }
            tvStatus.text = getString(R.string.admin_export_error_save)
            Toast.makeText(this, R.string.admin_export_error_save, Toast.LENGTH_SHORT).show()
            clearPendingExport()
            setLoading(false)
        }
    }

    private fun archivarReservasExportadas() {
        val rows = pendingExportRows
        val archivoOperacion = pendingArchivoOperacion
        if (rows.isEmpty()) {
            tvStatus.text = getString(R.string.admin_export_error_archive)
            Toast.makeText(this, R.string.admin_export_error_archive, Toast.LENGTH_SHORT).show()
            clearPendingExport()
            setLoading(false)
            return
        }

        val desde = desdeMillis ?: run {
            tvStatus.text = getString(R.string.admin_export_error_missing_from)
            Toast.makeText(this, R.string.admin_export_error_missing_from, Toast.LENGTH_SHORT).show()
            clearPendingExport()
            setLoading(false)
            return
        }

        if (archivoOperacion == null) {
            tvStatus.text = getString(R.string.admin_export_error_log_missing)
            Toast.makeText(this, R.string.admin_export_error_log_missing, Toast.LENGTH_SHORT).show()
            clearPendingExport()
            setLoading(false)
            return
        }

        ReservasRepository.archivarReservasExportadas(
            rows,
            desde,
            hastaMillis,
            archivoOperacion.id,
            archivoOperacion.archivoNombre,
            archivoOperacion.operadorUid,
            archivoOperacion.operadorEmail
        ) { ok, result ->
            runOnUiThread {
                if (!ok || result == null) {
                    ReservasRepository.fallarOperacionArchivoReservas(
                        archivoOperacion.id,
                        getString(R.string.admin_export_log_archive_failed)
                    )
                    tvStatus.text = getString(R.string.admin_export_error_archive)
                    Toast.makeText(this, R.string.admin_export_error_archive, Toast.LENGTH_LONG).show()
                } else {
                    renderLastArchivedDate(result.ultimaFechaArchivadaMillis)
                    tvStatus.text = getString(R.string.admin_export_status_success, result.cantidadReservas)
                    Toast.makeText(
                        this,
                        getString(R.string.admin_export_status_success, result.cantidadReservas),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                clearPendingExport()
                setLoading(false)
            }
        }
    }

    private fun validarRangoSeleccionado(requireDesde: Boolean = true): String? {
        val desde = desdeMillis ?: return if (requireDesde) {
            getString(R.string.admin_export_error_missing_from)
        } else {
            null
        }

        if (desde > hastaMillis) {
            return getString(R.string.admin_export_error_invalid_range)
        }

        val retentionLimit = retentionLimitMillis
        if (retentionLimit != null && hastaMillis > retentionLimit) {
            val configuredRetentionDays = retentionDays ?: 0
            return getString(
                R.string.admin_export_error_retention,
                dateFormatter.format(Date(retentionLimit)),
                configuredRetentionDays
            )
        }

        return null
    }

    private fun renderSelectedRange() {
        tvDesde.text = desdeMillis?.let {
            getString(R.string.admin_export_from_value, dateFormatter.format(Date(it)))
        } ?: getString(R.string.admin_export_from_empty)
        tvHasta.text = getString(R.string.admin_export_to_value, dateFormatter.format(Date(hastaMillis)))
        updateRangeFeedback()
    }

    private fun renderLastArchivedDate(lastArchivedMillis: Long?) {
        this.lastArchivedMillis = lastArchivedMillis?.let(::normalizeDate)
        val value = lastArchivedMillis?.let { dateFormatter.format(Date(it)) }
            ?: getString(R.string.admin_export_last_archived_placeholder)
        tvLastArchived.text = getString(R.string.admin_export_last_archived_value, value)
        renderOverlapWarning()
    }

    private fun renderRetentionPolicy(configuredRetentionDays: Int?) {
        retentionDays = configuredRetentionDays
        retentionInput.setText(configuredRetentionDays?.toString().orEmpty())
        val limitMillis = calculateArchiveRetentionLimitMillis(
            referenceMillis = System.currentTimeMillis(),
            retentionDays = configuredRetentionDays
        )
        retentionLimitMillis = limitMillis
        if (!userChangedHasta) {
            hastaMillis = limitMillis ?: Calendar.getInstance().clearTime().timeInMillis
        }
        tvRetention.text = if (limitMillis != null && configuredRetentionDays != null) {
            getString(
                R.string.admin_export_retention_configured,
                configuredRetentionDays,
                dateFormatter.format(Date(limitMillis))
            )
        } else {
            getString(R.string.admin_export_retention_not_configured)
        }
        updateRangeFeedback()
    }

    private fun updateRangeFeedback() {
        if (!::tvStatus.isInitialized) {
            return
        }

        renderOverlapWarning()
        tvStatus.text = validarRangoSeleccionado(requireDesde = false)
            ?: getString(R.string.admin_export_status_idle)
    }

    private fun renderOverlapWarning() {
        if (!::tvOverlapWarning.isInitialized) {
            return
        }

        val warningMessage = buildOverlapWarningMessage(desdeMillis, hastaMillis)
        if (warningMessage == null) {
            tvOverlapWarning.visibility = View.GONE
            tvOverlapWarning.text = ""
            return
        }

        tvOverlapWarning.visibility = View.VISIBLE
        tvOverlapWarning.text = warningMessage
    }

    private fun buildOverlapWarningMessage(desde: Long?, hasta: Long): String? {
        val desdeNormalizado = desde ?: return null
        val validationError = validarRangoSeleccionado(requireDesde = false)
        if (validationError != null) {
            return null
        }

        val lastArchived = lastArchivedMillis ?: return null
        if (desdeNormalizado > lastArchived) {
            return null
        }

        return getString(
            R.string.admin_export_overlap_warning,
            formatRange(desdeNormalizado, hasta),
            dateFormatter.format(Date(lastArchived))
        )
    }

    private fun formatRange(desde: Long, hasta: Long): String {
        return "${dateFormatter.format(Date(desde))} - ${dateFormatter.format(Date(hasta))}"
    }

    private fun showDatePicker(initialMillis: Long, onDateSelected: (Long) -> Unit) {
        createSpanishDatePickerDialog(initialMillis, onDateSelected).show()
    }

    private fun buildFileName(desdeMillis: Long, hastaMillis: Long): String {
        return "reservas_${fileDateFormatter.format(Date(desdeMillis))}_a_${fileDateFormatter.format(Date(hastaMillis))}.csv"
    }

    private fun normalizeDate(value: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = value
        }.clearTime().timeInMillis
    }

    private fun setLoading(loading: Boolean) {
        btnDesde.isEnabled = !loading
        btnHasta.isEnabled = !loading
        btnGenerar.isEnabled = !loading
    }

    private fun setRetentionControlsEnabled(enabled: Boolean) {
        retentionInput.isEnabled = enabled
        btnGuardarRetention.isEnabled = enabled
    }

    private fun clearPendingExport() {
        pendingCsvContent = null
        pendingExportRows = emptyList()
        pendingArchivoOperacion = null
    }

    private fun Calendar.clearTime(): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
