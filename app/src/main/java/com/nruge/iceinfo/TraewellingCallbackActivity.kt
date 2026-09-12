package com.nruge.iceinfo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Empfängt den OAuth-Redirect-Deep-Link iceinfo://traewelling/callback
 * (der Server leitet die https-Redirect-URI per 302 hierher weiter), tauscht den
 * Code gegen ein Token und bringt danach die MainActivity wieder in den Vordergrund.
 * Transluzentes Theme → kein sichtbares Aufblitzen.
 */
class TraewellingCallbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        val code = data?.getQueryParameter("code")
        val state = data?.getQueryParameter("state")
        if (code != null) {
            lifecycleScope.launch {
                TraewellingAuth.handleRedirect(applicationContext, code, state)
                returnToApp()
            }
        } else {
            returnToApp()
        }
    }

    private fun returnToApp() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        finish()
    }
}
