package com.sabeel.steps.data.export

import android.content.Context
import android.net.Uri
import com.sabeel.steps.AppContainer
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.data.db.toDomain
import com.sabeel.steps.domain.session.WalkWithLegs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Storage Access Framework write layer for the opt-in sessions export (plan Phase 3). It only moves
 * bytes: formatting lives in [SessionExport] and stays pure/unit-tested. There is no INTERNET, no
 * storage permission and no network code; the user picks the destination URI with SAF and the civil
 * [Context] is used solely to reach the app container and the content resolver.
 *
 * All work runs on [Dispatchers.IO]. Failures surface as [IOException] for the caller (the Settings
 * UI shows a message); coroutine cancellation is rethrown untouched. A missing session is not an
 * error: [exportSessionGpx] returns false and writes nothing.
 *
 * CSV exports are bounded by [MAX_EXPORT_SESSIONS] so a pathological history cannot exhaust memory;
 * the limit is checked before any row is formatted. The CSV format itself is unchanged (one row per
 * session, oldest start first), and an empty history writes the header-only file and returns 0.
 */
object SessionExportIo {

    /** Upper bound on sessions per CSV export; refused before formatting so memory stays bounded. */
    const val MAX_EXPORT_SESSIONS = 20_000

    /**
     * Writes every saved session (oldest start first) as the sessions CSV to [uri].
     *
     * @return the number of sessions written; 0 means the header-only file was written.
     * @throws IOException when the history exceeds [MAX_EXPORT_SESSIONS], the destination cannot be
     *   opened, the write fails, or the session rows cannot be read.
     */
    suspend fun exportAllSessionsCsv(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        asExportFailure {
            val walkDao = container(context).database.walkDao()
            val newestFirst = walkDao.getAll()
            if (newestFirst.size > MAX_EXPORT_SESSIONS) {
                throw IOException(
                    "Too many sessions to export (limit " + MAX_EXPORT_SESSIONS + ")",
                )
            }
            val sessions = newestFirst.asReversed().map { entity ->
                val walk = entity.toDomain()
                WalkWithLegs(
                    walk = walk,
                    legs = walkDao.legsFor(walk.id).map { leg -> leg.toDomain() },
                )
            }
            writeText(context, uri, SessionExport.buildSessionCsv(sessions))
            sessions.size
        }
    }

    /**
     * Writes one saved session as a single-track GPX 1.1 file to [uri].
     *
     * @return true when the session existed and was written; false when no walk has [walkId] (the
     *   destination is left untouched).
     * @throws IOException when the destination cannot be opened or the write fails.
     */
    suspend fun exportSessionGpx(context: Context, uri: Uri, walkId: Long): Boolean =
        withContext(Dispatchers.IO) {
            asExportFailure {
                val walkDao = container(context).database.walkDao()
                val entity = walkDao.get(walkId) ?: return@asExportFailure false
                val session = WalkWithLegs(
                    walk = entity.toDomain(),
                    legs = walkDao.legsFor(walkId).map { leg -> leg.toDomain() },
                )
                writeText(context, uri, SessionExport.buildSessionGpx(session, sessionName(walkId)))
                true
            }
        }

    /** Runs [block], turning anything that is not a cancellation or an [IOException] into one. */
    private suspend fun <T> asExportFailure(block: suspend () -> T): T = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (io: IOException) {
        throw io
    } catch (failure: Exception) {
        throw IOException("Session export failed", failure)
    }

    /** Truncates and writes [text] as UTF-8 through the SAF URI; never leaves a half-written buffer. */
    private fun writeText(context: Context, uri: Uri, text: String) {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot open the selected file for writing")
        stream.use { output -> output.write(text.toByteArray(Charsets.UTF_8)) }
    }

    /** Same app-container lookup as [CsvIo] so exports read the installed database, not a new one. */
    private fun container(context: Context): AppContainer {
        val application = context.applicationContext as? SabeelApp
            ?: throw IllegalStateException("SessionExportIo must be used from the Sabeel application")
        return application.container
    }
}

/**
 * Stable, non-localized GPX track name for one session ("Sabeel session 42"). Pure and JVM-tested:
 * the data layer must not depend on Android resources or the user's language, so the export file is
 * always English and reproducible. A non-positive id (unsaved session) falls back to the plain name.
 */
fun sessionName(walkId: Long): String = if (walkId > 0L) "Sabeel session " + walkId else "Sabeel session"
