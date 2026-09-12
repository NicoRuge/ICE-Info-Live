package com.nruge.iceinfo.model

/**
 * Verbundenes Träwelling-Konto. Vorhandensein (≠ null) bedeutet „verbunden".
 * [username] kann null sein, wenn der Name nicht abgerufen werden konnte
 * (der Scope `write-statuses` deckt die Profil-Abfrage nicht zwingend ab).
 */
data class TraewellingAccount(
    val username: String? = null
)
