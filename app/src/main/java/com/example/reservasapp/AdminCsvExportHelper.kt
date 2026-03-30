package com.example.reservasapp

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AdminCsvExportHelper {
    private val csvDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val preferredSectionOrder = listOf(
        MenuIdentity.sectionDisplayName(MenuIdentity.SECTION_MAIN),
        MenuIdentity.sectionDisplayName(MenuIdentity.SECTION_SIDE),
        MenuIdentity.sectionDisplayName(MenuIdentity.SECTION_DESSERT)
    )

    fun buildResumenPedidosFileName(fechaMillis: Long): String {
        return "resumen_pedidos_${csvDateFormatter.format(Date(fechaMillis))}.csv"
    }

    fun buildDetallePedidosFileName(fechaMillis: Long): String {
        return "detalle_pedidos_${csvDateFormatter.format(Date(fechaMillis))}.csv"
    }

    fun buildResumenPedidosCsv(
        fechaMillis: Long,
        groupedCounts: Map<String, Map<String, Int>>
    ): String {
        val fecha = csvDateFormatter.format(Date(fechaMillis))
        val builder = StringBuilder()
        builder.append("\uFEFF")
        builder.appendLine("fecha,seccion,plato,cantidad")

        orderedSections(groupedCounts).forEach { section ->
            groupedCounts[section]
                .orEmpty()
                .toList()
                .sortedBy { it.first.lowercase(Locale.getDefault()) }
                .forEach { (dishName, count) ->
                    builder.appendCsvRow(fecha, section, dishName, count.toString())
                }
        }

        return builder.toString()
    }

    fun buildDetallePedidosCsv(
        fechaMillis: Long,
        rows: List<ReservasRepository.DetallePedidoUsuario>
    ): String {
        val fecha = csvDateFormatter.format(Date(fechaMillis))
        val builder = StringBuilder()
        builder.append("\uFEFF")
        builder.appendLine("fecha,empresa,nombre,apellido,plato_principal,guarnicion,postre")

        rows.sortedWith(
            compareBy<ReservasRepository.DetallePedidoUsuario> { it.empresa.lowercase(Locale.getDefault()) }
                .thenBy { it.apellido.lowercase(Locale.getDefault()) }
                .thenBy { it.nombre.lowercase(Locale.getDefault()) }
        ).forEach { row ->
            builder.appendCsvRow(
                fecha,
                row.empresa,
                row.nombre,
                row.apellido,
                row.platoPrincipal,
                row.guarnicion,
                row.postre
            )
        }

        return builder.toString()
    }

    @Throws(IOException::class)
    fun writeCsv(contentResolver: ContentResolver, uri: Uri, csvContent: String) {
        contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer ->
            if (writer == null) {
                throw IOException("No se pudo abrir el destino del archivo")
            }
            writer.write(csvContent)
            writer.flush()
        }
    }

    private fun orderedSections(groupedCounts: Map<String, Map<String, Int>>): List<String> {
        return preferredSectionOrder + groupedCounts.keys
            .filterNot { it in preferredSectionOrder }
            .sortedBy { it.lowercase(Locale.getDefault()) }
    }
}

private fun StringBuilder.appendCsvRow(vararg values: String) {
    appendLine(values.joinToString(",") { value ->
        val escaped = value.replace("\"", "\"\"")
        "\"$escaped\""
    })
}
