package com.sabeel.steps.diagnostics

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.sabeel.steps.BuildConfig
import java.io.File

/** Hands the ZIP to any app the user picks. The app has no INTERNET permission, so nothing else can happen. */
object DiagnosticsShare {

    fun providerUri(context: Context, file: File): android.net.Uri =
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    fun share(context: Context, file: File): Boolean = runCatching {
        val uri = providerUri(context, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Sabeel diagnostics " + BuildConfig.VERSION_NAME)
            clipData = ClipData.newRawUri("sabeel-report", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Sabeel diagnostics")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(chooser)
        true
    }.getOrDefault(false)
}
