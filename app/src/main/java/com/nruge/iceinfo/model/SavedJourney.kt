package com.nruge.iceinfo.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedJourney(
    val id: String,
    val trainType: String,
    val trainNumber: String,
    val originStation: String,
    val destinationStation: String,
    val date: String,               // "23.05.2025"
    val departureTime: String,      // "14:02"
    val arrivalTime: String,        // "18:47"
    val delayMinutes: Int,
    val distanceKm: Int,
    val topSpeedKmh: Int,
    val avgSpeedKmh: Int,
    val durationMinutes: Int,
    val stopsCount: Int,
    val recordedGps: Boolean = false,
    val trackPoints: List<TrackPoint> = emptyList(),
    // Automatisch bei der Aufzeichnung erfasst
    val tzn: String = "",           // Triebzugnummer (z. B. "ICE0304")
    val series: String = "",        // Baureihe (z. B. "412")
    // Vom Nutzer editierbar
    val name: String = "",          // optionaler Name der Fahrt
    val purpose: String = "",       // Grund/Anlass (z. B. Pendeln, Urlaub)
    val ticketType: String = "",    // Ticketart (z. B. Sparpreis, Deutschland-Ticket)
    val price: String = "",         // Preis, frei formatiert (z. B. "39,90 €")
    val seat: String = "",          // Wagen/Sitzplatz (z. B. "Wg. 23, Pl. 81")
    val notes: String = "",         // freie Notizen
    // Statistik-Crowdsourcing: wurde diese Fahrt an api.iceinfo.de geteilt?
    val shared: Boolean = false,
    // Server-Hash der geteilten Fahrt (Primärschlüssel) — für den Detail-Link
    // stats.iceinfo.de/j/<hash>. Leer, solange nicht geteilt.
    val sharedHash: String = "",
    // Verspätung am Zug-Endbahnhof — zur Vergleichbarkeit zwischen Nutzern.
    // delayMinutes ist am Ausstiegshalt des Nutzers gemessen; finalDelayMinutes
    // bezieht sich auf den Endbahnhof des Zuges. finalDelayIsPrognosis = true,
    // wenn der Nutzer vor dem Endbahnhof ausgestiegen ist → dann ist der Wert nur
    // die Prognose zum Ausstiegszeitpunkt, nicht die tatsächliche Ankunftsverspätung.
    val finalStation: String = "",
    val finalDelayMinutes: Int = 0,
    val finalDelayIsPrognosis: Boolean = false,
    // Fahrtverlauf (Halte des gefahrenen Abschnitts mit Zeiten/Verspätungen)
    val stops: List<JourneyStop> = emptyList()
)
