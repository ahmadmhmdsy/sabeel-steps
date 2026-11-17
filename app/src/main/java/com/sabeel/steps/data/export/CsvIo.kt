package com.sabeel.steps.data.export

import android.content.Context
import android.net.Uri
import com.sabeel.steps.AppContainer
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.data.db.toDomain
import com.sabeel.steps.data.repo.RestoreSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * SAF-facing CSV export/import. All I/O runs on [Dispatchers.IO]; no network is involved.
 * The Storage Access Framework gives us a content:// URI chosen by the user.
 *
 * The file contains one row per local day (daily totals only). Walk sessions are not part of this
 * format; the Settings copy says so instead of implying a full backup.
 */
object CsvIo {

    const val HEADER = CsvCodec.HEADER

    /** Guards against importing a huge or hostile file into memory. */
    const val MAX_IMPORT_BYTES = 5 * 1024 * 1024
    const val MAX_IMPORT_ROWS = 20_000

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
     * Validates the header and restores every valid row through one transaction with a max-merge
     * per date. Malformed lines are skipped; an invalid/absent header throws
     * [CsvCodec.CsvFormatException]. Oversized files are refused before decoding.
     */
    suspend fun import(context: Context, uri: Uri): RestoreSummary = withContext(Dispatchers.IO) {
        val container = container(context)
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open the selected file for reading")
        val bytes = stream.use { input -> input.readBytes() }
        if (bytes.size > MAX_IMPORT_BYTES) {
            throw CsvCodec.CsvFormatException("File is too large to import (limit 5 MB)")
        }
        val text = bytes.toString(Charsets.UTF_8)
        val decoded = CsvCodec.decode(text, System.currentTimeMillis())
        if (decoded.rows.size > MAX_IMPORT_ROWS) {
            throw CsvCodec.CsvFormatException("Too many rows to import (limit 20000)")
        }
        container.steps.restore(decoded.rows)
    }

    private fun container(context: Context): AppContainer {
        val application = context.applicationContext as? SabeelApp
            ?: throw IllegalStateException("CsvIo must be used from the Sabeel application")
        return application.container
    }
}
