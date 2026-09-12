package com.nruge.iceinfo.model

import kotlinx.serialization.Serializable

/**
 * Ein Halt im Fahrtverlauf einer aufgezeichneten Fahrt — für die Anzeige als
 * Timeline (in der App und auf der geteilten Detailseite). Enthält nur
 * Fahrplan-/Verspätungsdaten (öffentlich), keine persönlichen Angaben.
 */
@Serializable
data class JourneyStop(
    val name: String,
    val time: String = "",          // geplante Zeit "HH:mm" (Ankunft, sonst Abfahrt)
    val delayMinutes: Int = 0,      // Verspätung an diesem Halt
    val cancelled: Boolean = false, // entfallener Halt
    val additional: Boolean = false, // Zusatzhalt (nicht im Regelfahrplan)
    // Halt liegt hinter dem Ausstieg des Nutzers → Wert ist nur die Prognose zum
    // Ausstiegszeitpunkt, nicht gemessen. Für die Visualisierung „ab hier ungenau".
    val prognosis: Boolean = false
)
