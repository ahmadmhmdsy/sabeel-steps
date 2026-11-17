package com.sabeel.steps.data.export

import android.content.Context
import android.net.Uri
import com.sabeel.steps.AppContainer
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.data.db.toDomain
import com.sabeel.steps.data.db.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * SAF-facing CSV export/import. All I/O runs on [Dispatchers.IO]; no network is involved.
 * The Storage Access Framework gives us a content:// URI chosen by the user.
 */
object CsvIo {

    const val HEADER = CsvCodec.HEADER

    /** Writes every DailySteps row (oldest first) to [uri]. @return rows written. */
    suspend fun export(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val container = container(context)
        val rows = container.database.dailyStepsDao().getAll()
            .asReversed()
            .map { it.toDomain() }
        val text = CsvCodec.toCsv(rows)
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot open the selected file for writing")
        stream.use { output -> output.write(text.toByteArray(Charsets.UTF_8)) }
        rows.size
    }

    /**
     * Validates the header and upserts every valid row (dates already present are replaced).
     * Malformed lines are skipped; an invalid/absent header throws [CsvCodec.CsvFormatException].
     * @return rows imported.
     */
    suspend fun import(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val container = container(context)
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open the selected file for reading")
        val text = stream.use { input -> input.readBytes().toString(Charsets.UTF_8) }
        val decoded = CsvCodec.decode(text, System.currentTimeMillis())
        val dao = container.database.dailyStepsDao()
        var imported = 0
        decoded.rows.forEach { day ->
            dao.upsert(day.toEntity())
            imported++
        }
        imported
    }

    private fun container(context: Context): AppContainer {
        val application = context.applicationContext as? SabeelApp
            ?: throw IllegalStateException("CsvIo must be used from the Sabeel application")
        return application.container
    }
}
