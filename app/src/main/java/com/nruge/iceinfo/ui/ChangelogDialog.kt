package com.nruge.iceinfo.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R

internal data class ChangelogEntry(
    val version: String,
    val newFeatures: List<String> = emptyList(),
    val fixes: List<String> = emptyList()
)

internal val changelog = listOf(
    ChangelogEntry(
        version = "6.4",
        newFeatures = listOf(
            "„Meine Fahrten“ überarbeitet: Fahrtkarten sind jetzt aufgeräumt und übersichtlich)"
        )
    ),
    ChangelogEntry(
        version = "6.3",
        newFeatures = listOf(
            "Aufgezeichnete Fahrten können jetzt benannt und bearbeitet werden",
            "Map mit allen Fahrten (Alle zusammen oder einzelne Tage)",
            "Neuer Anschluss- /Abfahrtmonitor",
            "Fahrten anonym hochladen und zur Statistik auf stats.iceinfo.de beitragen",
            "Fahrt Ansehen öffner einen In-App-Browser mit der hochgeladenen Fahrt"
        ),
        fixes = listOf(
            "Fix für Live Notification auf älteren Android Versionen",
            "Wenn der Zug steht, werden bei aktiver Fahrtaufzeichnung keie GPS punkte mehr gespeichert.",
            "Die Fahrtaufzeichnung beinhaltet jetzt die Baurehihe",
            "Die Koordinaten in Fahrtaufteichnungen werden nur noch mit 5 Nachkommastellen gespeichert",
            "Anschlüsse vorrübergehend aus dem unteren Menüband entfernt - der API entpunkt ist momentan nicht verwendbar",
            "Neue Ausstiegs-Erkennung reachedNext - der isNext-Halt gilt als Ausstieg, wenn der Zug ihn erreicht hat",
            "UI-Änderungen"
        )
    ),
    ChangelogEntry(
        version = "6.2",
        newFeatures = listOf(
            "Zug-Seite zeigt nun den Taufnamen des Zuges",
            "Gesperrte Wagen werden jetzt als gesperrt in der Wagenreihung angezeigt",
            "Teilen-Button um einen Textblock mit Infos deiner Reise an Freunde zu schicken",
            "Import/Export aller aufgezeichneter Fahrten ",
            "[Issue #24] Im Streckenverlauf sind nun Fahrtrichtungswechsel angegeben",
            "[Issue #18] Fahrt Teilen: Nun kannst du im Dreipunkt-Menü den aktuellen Stand deiner Fahrt teilen, mit Geschwindigkeit, Zugnummer, nächstem Halt und deinem Zielhalt"
        ),
        fixes = listOf(
            "[Issue #22] 'Aufzeichnung läuft'-Banner nun im Header, neben dem Live-Indikator'",
            "[Issue #21] Fahrtaufzeichnungen werden jetzt zwischengespeichert und automatisch gespeichert, wenn die Reise abgeschlossen ist",
            "[Issue #17] Die Live-Notification beendet nun bei Verlust des Zug WLANs, statt in den Demo-Modus zu springen"
        )
    ),
    ChangelogEntry(
        version = "6.0.1",
        fixes = listOf(
            "SSL-Verbindungsfehler zum ICE Portal behoben – die App verbindet sich jetzt auch bei ungültiger Zertifikatskette zuverlässig",
            "Die Wagenreihung bei Doppeltraktion zeigt jetzt in der mitte nicht mehr zwei Triebköpfe nach links",
            "Debug Dialog ist nun hilfreicher und die Textdatei zum Teilen beinhaltet mehr Daten"
        )
    ),
    ChangelogEntry(
        version = "6",
        newFeatures = listOf(
            "SPEISEKARTE - Jetzt die Live-Speisekarte des Bordrestaurants direkt in der App ansehen",
            "FAHRTEN AUFZEICHNEN - Jetzt kannst du deine Fahrten aufzeichnen und in der App speichen, mit Daten wie Durchschnittsgeschwindigkeit, Verspätung, oder Karte mit dem Fahrtverlauf (Als .GPX exportierbar)",
            "Streckenkarte ersetzt durch einen Daten-Tab mit Streckeninfos zu Höchstgeschwindigkeit, Elektrifizierung, Gleisanzahl, Streckentyp, POIs",
            "Halte und Anschlüsse: Zeiten wechseln alle 5 Sekunden zwischen Uhrzeit HH:MM und Restzeit ('in X min') - kein rechnen mehr ;)",
            "Swipen um Seiten zu wechseln"
        ),
        fixes = listOf(
            "API Abfragen wurden reduziert"
        )
    ),
    ChangelogEntry(
        version = "6.0.1",
        fixes = listOf(
            "SSL-Verbindungsfehler zum ICE Portal behoben – die App verbindet sich jetzt auch bei ungültiger Zertifikatskette zuverlässig",
            "Die Wagenreihung bei Doppeltraktion zeigt jetzt in der mitte nicht mehr zwei Triebköpfe nach links",
            "Debug Dialog ist nun hilfreicher und die Textdatei zum Teilen beinhaltet mehr Daten"
        )
    ),
    ChangelogEntry(
        version = "6",
        newFeatures = listOf(
            "SPEISEKARTE - Jetzt die Live-Speisekarte des Bordrestaurants direkt in der App ansehen",
            "FAHRTEN AUFZEICHNEN - Jetzt kannst du deine Fahrten aufzeichnen und in der App speichen, mit Daten wie Durchschnittsgeschwindigkeit, Verspätung, oder Karte mit dem Fahrtverlauf (Als .GPX exportierbar)",
            "Streckenkarte ersetzt durch einen Daten-Tab mit Streckeninfos zu Höchstgeschwindigkeit, Elektrifizierung, Gleisanzahl, Streckentyp, POIs",
            "Halte und Anschlüsse: Zeiten wechseln alle 5 Sekunden zwischen Uhrzeit HH:MM und Restzeit ('in X min') - kein rechnen mehr ;)",
            "Swipen um Seiten zu wechseln"
        ),
        fixes = listOf(
            "API Abfragen wurden reduziert"
        )
    ),
    ChangelogEntry(
        version = "5.0.2",
        fixes = listOf(
            "Anschlüsse: Fehlende Ziele werden jetzt aus den Abfahrtsdaten (transport.rest) ergänzt, da die ICE Portal API das Ziel nicht immer liefert"
        )
    ),
    ChangelogEntry(
        version = "5.0",
        newFeatures = listOf(
            "Live Update (Android 16+): Die Reise wird jetzt als interaktive Fortschritts-Notification auf dem Sperrbildschirm und im Always-On-Display angezeigt",
            "Status-Chip in der Statusleiste: zeigt die aktuelle Geschwindigkeit; bei Verspätung wechselt es zwischen Speed und Verspätungs-Minuten",
            "Dynamische Material You Farben wieder eingebaut",
            "'Bahnhof'-Seite listet Live Infos zum gewählten Zielbahnhof auf",
            "Zurück-Gesten um zum Status oder zum Willkommensbildschirm zurückzukehren",
            "Service-Seite in Bahnhof umgewandelt. Mit Live Daten zu Aufzügen, Rolltreppen und Ausstattung"
        ),
        fixes = listOf(
            "Statusbildschirm angepasst. Konsistenters Material Design.",
            "Neu Gestalteter Willkommensbildschirm",
            "[Issue #9] Bei Aktiver Wlan-Verbindung fällt die App nicht mehr in den Willkommensbildschirm, sondern zeigt eine 'Keine API Verbingung'-Meldung an.",
            "[Issue #7] Textfarben von normalem Text nicht mehr Hellrot",
            "Neues, stabileres Mapping der Baureihe"
        )
    ),
    ChangelogEntry(
        version = "4.0.2",
        fixes = listOf(
            "Hotfix: App stürzte beim Start ab, ist gefixt"
        )
    ),
    ChangelogEntry(
        version = "4.0 (Release Candidate 1)",
        newFeatures = listOf(
            "Erster Release-Kandidat für die öffentliche Veröffentlichung im Google Play Store",
            "Optionale, anonyme Absturzberichte via Firebase Crashlytics — standardmäßig aus, in den Einstellungen jederzeit umschaltbar",
            "Einmaliger Opt-in-Dialog beim ersten Start und nach jedem Update fragt aktiv nach Zustimmung zum Crash-Reporting",
            "Widget und Benachrichtigung vollständig lokalisiert (Deutsch / Englisch) — keine fest verdrahteten Texte mehr",
            "Theme-Auswahl (System/Hell/Dunkel) wird jetzt dauerhaft gespeichert und überlebt App-Neustarts",
            "Versionsnummer im Info-Dialog wird automatisch aus dem Build übernommen"
        ),
        fixes = listOf(
            "Anschlüsse und Abfahrten zeigen keine Beispieldaten mehr, wenn keine echten Daten verfügbar sind — stattdessen klare Empty States",
            "Points-of-Interest-Karte wird ausgeblendet, wenn keine POIs in der Nähe sind, statt Demo-POIs zu zeigen",
            "Akku- und Datenverbrauch reduziert: Polling-Intervall der Benachrichtigung von 3 auf 5 Sekunden erhöht, mit exponentiellem Backoff (bis 60 s) bei Netzfehlern",
            "Cleartext-Verbindungen sind nur noch für iceportal.de erlaubt, alle übrigen Endpunkte werden HTTPS-erzwungen",
            "Unbenutzte Speicher-Berechtigung (WRITE_EXTERNAL_STORAGE) entfernt",
            "Code-Verschleierung und Resource-Shrinking (R8) aktiviert — APK ist deutlich kleiner",
            "Mehrere potenzielle Absturzursachen durch sichere Null-Behandlung in Netzwerk- und Service-Code entfernt",
            "WLAN-Statusabfrage und UI-Datenströme reagieren jetzt auf den App-Lebenszyklus — keine unnötige Aktivität im Hintergrund",
            "Statusleiste passt sich beim Wechsel des Themes sauberer an"
        )
    ),
    ChangelogEntry(
        version = "3.9",
        newFeatures = listOf(
            "Streckenverlauf: Ankunfts- und Abfahrtszeiten werden jetzt pro Halt nebeneinander angezeigt",
            "Streckenverlauf: Ausgefallene Halte werden mit durchgestrichenem Namen und 'Entfällt'-Badge dargestellt",
            "Debug-Dialog in den Einstellungen: zeigt Geräteinformationen, OS-Version, App-Version sowie Auszüge der API-Antworten; mit Kopieren- und Teilen-Funktion"
        ),
        fixes = listOf(
            "Zeitanzeige im Streckenverlauf verwendet jetzt Monospace-Schrift für saubere Ausrichtung",
            "Anschlussverbindungen nutzen jetzt konsistentes Design"
        )
    ),
    ChangelogEntry(
        version = "3.8",
        newFeatures = listOf(
            "Halte - Zeigt jetzt einen Indikator an Halten, falls dieser halt ein Zusatzhalt ist",
            "In-App Pop-Up bei neuem Update"
        ),
        fixes = listOf(
            "Benachrichtigung: Text im Dark Mode war schwarz auf dunklem Hintergrund und damit nicht lesbar – Textfarben werden jetzt korrekt je nach System-Theme (Hell/Dunkel) aufgelöst",
            "Design Tweaks - Sehr viel mehr konsistenz im App Design"
        )
    ),
    ChangelogEntry(
        version = "3.6",
        newFeatures = listOf(
            "Einstellung 'Reduzierte Bewegung': deaktiviert die ICE-Animation und Übergänge zwischen Screens.",
            "Abfahrtsmonitor 'Weitere Abfahrten' auf der Anschlüsse-Seite: zeigt alle Abfahrten am gewählten Ausstiegsbahnhof (oder nächsten Halt) ab dem Zeitpunkt deiner Ankunft. Datenquelle: v6.db.transport.rest",
            "Sprach-Switch in den Einstellungen: Deutsch/Englisch umschaltbar, initial folgt der Systemsprache. App-Sprache erscheint auch in den Android-Systemeinstellungen unter 'App-Sprachen'"
        ),
        fixes = listOf(
            "TravelSummaryCard: Verbleibende Reisezeit wird jetzt aus geplanter Ankunftszeit + Verspätung berechnet, statt aus Distanz/Zuggeschwindigkeit",
            "Anschlüsse: zeigt jetzt die Verbindungen am gewählten Ausstiegsbahnhof statt immer am nächsten Halt",
            "Übersetzung: zahlreiche feste deutsche Labels in String-Ressourcen extrahiert und ins Englische übersetzt"
        )
    ),
    ChangelogEntry(
        version = "3.5",
        newFeatures = listOf(
            "Änderungsprotokoll im Menü",
            "Points of Interest: Material 3 Redesign mit echten Icons, Typ-Beschriftung und Distanz-Chip",
            "Theme-Auswahl: Buttons zu 'System / Hell / Dunkel' verkürzt für bessere Darstellung",
            "Info-Dialog: Material 3 Redesign mit gruppierten Karten"
        ),
        fixes = listOf(
            "POI-Endpunkt auf /pois/map/{bbox} umgestellt – der alte /pois/all-Endpunkt funktionierte nicht",
            "Netzwerk: HTTPS-Timeout von 2s auf 5s erhöht, HTTP-Fallback ergänzt (ältere ICE-Züge)",
            "Netzwerk: POI-Antwort wird jetzt sowohl als Objekt {pois:[…]} als auch als Array […] korrekt geparst",
            "WLAN-Erkennung: SSID-Abfrage auf Android 12+ ohne Standortberechtigung repariert",
            "Benachrichtigung: zeigte beim Start immer Hamburg–München als Beispieldaten statt 'Verbinde…'",
            "Widget: Ausstieg-Warnung verglich Haltenamen statt EVA-Nummern – funktioniert jetzt zuverlässig",
            "Widget: CoroutineScope wurde bei jedem Update neu erstellt (Speicherleck) – jetzt ein geteilter Scope",
            "TravelSummaryCard: Ankunftszeit bei zukünftigen Halten war leer (actualArrival noch nicht bekannt)",
            "Einstellungen werden jetzt in einer einzigen SharedPreferences-Datei gespeichert",
            "Debug-Dateien ThemeGallery und ThemeTestScreen aus dem Produktionscode entfernt",
            "Zeitformatierung thread-sicher gemacht (DateTimeFormatter statt SimpleDateFormat)"
        )
    ),
    ChangelogEntry(
        version = "3.0",
        newFeatures = listOf(
            "Auswahl eines persönlichen Zielbahnhofes"
        ),
        fixes = listOf(
            "NotificationService Service ID geändert",
            "Anzeige der Geschwindigkeit und des gewählten Ausstieges stabilisiert, Icon in der Android Status leiste hinzugefügt, das die Geschwindigkeit anzeigt",
            "evaNr in der API-Abfrage verbessert"
        )
    ),
    ChangelogEntry(
        version = "2.3",
        newFeatures = listOf(
            "Englisch als Appsprache eingebaut",
            "Separierung des Status-Bildschirmes auf mehrere Seiten:",
            "- Halte",
            "- Karte",
            "- Service (WIP)",
            "- Anschlüsse"
        ),
        fixes = listOf(
            "Zuganimation auf der Statusseite überarbeitet",
        )
    ),
    ChangelogEntry(
        version = "1.5",
        newFeatures = listOf(
            "Animierte Zug-Kopfzeile mit scrollenden Gleisen und verbesserter ICE-Klassifizierungslogik hinzugefügt",
            "Dunkles Design",

        ),
        fixes = listOf(
            "Modularisierung des UI in separate Dateien",
            "Migration der Netzwerkschicht von manuellen `HttpURLConnection`-Aufrufen zu Ktor mit `kotlinx.serialization`",
            "Implementierung von `MainViewModel` für die Zustandsverwaltung und die Abfrage-Logik"
            )
    ),
    ChangelogEntry(
        version = "1.0",
        newFeatures = listOf(
            "Status seite mit Geschwindigkeit, Zugnummer und -klasse, nächste Halte, Karte mit aktuellem Standort",
            "Dunkles Design",
            )
    )

)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogDialog(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp, end = 24.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.NewReleases, contentDescription = null)
                    Text(
                        stringResource(R.string.changelog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            itemsIndexed(changelog) { index, entry ->
                VersionSection(entry, expandedByDefault = index == 0)
            }
        }
    }
}

@Composable
private fun VersionSection(entry: ChangelogEntry, expandedByDefault: Boolean = false) {
    var expanded by rememberSaveable { mutableStateOf(expandedByDefault) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AssistChip(
                    onClick = { expanded = !expanded },
                    label = {
                        Text(
                            "v${entry.version}",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (entry.newFeatures.isNotEmpty()) {
                        ChangeGroup(
                            icon = Icons.Default.AutoAwesome,
                            title = stringResource(R.string.changelog_new_features),
                            items = entry.newFeatures
                        )
                    }
                    if (entry.fixes.isNotEmpty()) {
                        ChangeGroup(
                            icon = Icons.Default.BugReport,
                            title = stringResource(R.string.changelog_fixes),
                            items = entry.fixes
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeGroup(
    icon: ImageVector,
    title: String,
    items: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnnotatedChangelogText(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AnnotatedChangelogText(
    text: String,
    style: TextStyle,
    color: Color = Color.Unspecified
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val annotatedString = buildAnnotatedString {
        val issuePattern = Regex("\\[Issue #(\\d+)]")
        var lastMatchEnd = 0
        issuePattern.findAll(text).forEach { matchResult ->
            append(text.substring(lastMatchEnd, matchResult.range.first))
            val issueNumber = matchResult.groupValues[1]
            val url = "https://github.com/NicoRuge/ICE-Info-Live/issues/$issueNumber"

            withLink(
                LinkAnnotation.Url(
                    url = url,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = primaryColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    )
                )
            ) {
                append(matchResult.value)
            }

            lastMatchEnd = matchResult.range.last + 1
        }
        append(text.substring(lastMatchEnd))
    }

    Text(
        text = annotatedString,
        style = style,
        color = color
    )
}

@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    val entry = changelog.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.NewReleases, contentDescription = null) },
        title = { Text("Version ${entry.version}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (entry.newFeatures.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.changelog_new_features),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        entry.newFeatures.forEach { feature ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                AnnotatedChangelogText(
                                    text = feature,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                if (entry.fixes.isNotEmpty()) {
                    if (entry.newFeatures.isNotEmpty()) HorizontalDivider()
                    Text(
                        text = stringResource(R.string.changelog_fixes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        entry.fixes.forEach { fix ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.BugReport,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AnnotatedChangelogText(
                                    text = fix,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.info_close))
            }
        }
    )
}
