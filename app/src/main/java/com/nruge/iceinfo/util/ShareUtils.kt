package com.nruge.iceinfo.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus

/**
 * Baut den teilbaren Textblock zur aktuellen Fahrt und ruft das System-Share-Sheet auf.
 *
 * Ziel-Halt + dessen Ankunftszeit folgen derselben Logik wie [com.nruge.iceinfo.ui.components.TravelSummaryCard]:
 * wenn der Nutzer einen Ausstiegshalt gewählt hat (`targetStopEva`), wird dieser verwendet,
 * sonst der Endbahnhof der Fahrt.
 */
fun buildTrainShareText(context: Context, status: TrainStatus): String {
    val targetStop = status.stops.find { it.evaNr == status.targetStopEva }

    val exitName = targetStop?.name?.takeIf { it.isNotEmpty() } ?: status.destination
    val exitEta = targetStop
        ?.let { it.actualArrival.takeIf { t -> t.isNotEmpty() } ?: it.scheduledArrival }
        ?.takeIf { it.isNotEmpty() }
        ?: status.destinationEta

    val train = "${status.trainType} ${status.trainNumber}".trim()

    return context.getString(
        R.string.share_trip_text,
        train,
        status.destination,
        status.speed,
        status.nextStop,
        status.eta,
        exitName,
        exitEta
    )
}

/** Erzeugt den Share-Text und öffnet das System-Share-Sheet (ACTION_SEND, text/plain). */
fun shareTrainStatus(context: Context, status: TrainStatus) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, buildTrainShareText(context, status))
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.share_trip_chooser))
    )
}

/** Öffnet das System-Share-Sheet mit einem einzelnen Link (text/plain). */
fun shareLink(context: Context, url: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

/** Öffnet eine URL im In-App-Browser (Custom Tab); Fallback auf den Standard-Browser. */
fun openInAppBrowser(context: Context, url: String) {
    val uri = url.toUri()
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            // Kein Browser installiert — nichts zu tun
        }
    }
}
