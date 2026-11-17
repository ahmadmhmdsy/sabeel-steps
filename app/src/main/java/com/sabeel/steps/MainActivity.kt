package com.sabeel.steps

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sabeel.steps.ui.SabeelRoot
import com.sabeel.steps.ui.i18n.AppLocale
import com.sabeel.steps.ui.theme.SabeelTheme

/**
 * Single activity. Two things matter here:
 *  - attachBaseContext wraps the base context with the stored locale so Compose lays out RTL/LTR
 *    before the first frame (no flicker, no appcompat dependency);
 *  - enableEdgeToEdge() is required by targetSdk 36; every screen consumes its insets through the
 *    root scaffold so nothing hides under the status or navigation bars.
 */
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SabeelTheme {
                SabeelRoot(container = (application as SabeelApp).container)
            }
        }
    }
}
