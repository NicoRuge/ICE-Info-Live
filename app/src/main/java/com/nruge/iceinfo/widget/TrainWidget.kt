package com.nruge.iceinfo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nruge.iceinfo.MainActivity

class TrainWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val KEY_CONNECTED = booleanPreferencesKey("isConnected")
        val KEY_TRAIN_NAME = stringPreferencesKey("trainName")
        val KEY_SPEED = intPreferencesKey("speed")
        val KEY_NEXT_STOP = stringPreferencesKey("nextStop")
        val KEY_TARGET_STOP = stringPreferencesKey("targetStop")
        val KEY_DELAY = intPreferencesKey("delay")
        val KEY_MOCK_MODE = booleanPreferencesKey("isMockMode")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            TrainWidgetContent(prefs)
        }
    }

    @Composable
    private fun TrainWidgetContent(prefs: androidx.datastore.preferences.core.Preferences) {
        val isConnected = prefs[KEY_CONNECTED] ?: false
        val trainName = prefs[KEY_TRAIN_NAME] ?: ""
        val speed = prefs[KEY_SPEED] ?: 0
        val nextStop = prefs[KEY_NEXT_STOP] ?: ""
        val targetStop = prefs[KEY_TARGET_STOP] ?: ""
        val delay = prefs[KEY_DELAY] ?: 0
        val isMockMode = prefs[KEY_MOCK_MODE] ?: false

        val dbRed = Color(0xFFE3000F)
        val bgColor = ColorProvider(com.nruge.iceinfo.R.color.widget_background)
        val textColor = ColorProvider(com.nruge.iceinfo.R.color.widget_text_primary)
        val grayText = ColorProvider(com.nruge.iceinfo.R.color.widget_text_secondary)
        val dividerColor = ColorProvider(com.nruge.iceinfo.R.color.widget_divider)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(bottom = 4.dp) // Little space at bottom
        ) {
            // ICE Red Top Bar
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(ColorProvider(dbRed))
            ) {}

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                if (!isConnected && !isMockMode) {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Suche Verbindung...",
                            style = TextStyle(fontSize = 14.sp, color = grayText, fontWeight = FontWeight.Medium)
                        )
                    }
                } else {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.Top
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = trainName,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = grayText
                                )
                            )
                            Row(verticalAlignment = Alignment.Vertical.Bottom) {
                                Text(
                                    text = "$speed",
                                    style = TextStyle(
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(dbRed)
                                    )
                                )
                                Spacer(GlanceModifier.width(2.dp))
                                Text(
                                    text = "km/h",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(dbRed)
                                    ),
                                    modifier = GlanceModifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        
                        if (isMockMode) {
                            Box(
                                modifier = GlanceModifier
                                    .padding(top = 2.dp)
                                    .background(ColorProvider(Color(0xFFFFEB3B))) // Yellow for demo
                                    .cornerRadius(4.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DEMO",
                                    style = TextStyle(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(Color.Black)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(GlanceModifier.height(2.dp))
                    
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(dividerColor)
                    ) {}
                    
                    Spacer(GlanceModifier.height(6.dp))

                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        // Nächster Halt
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "Nächster Halt",
                                style = TextStyle(fontSize = 10.sp, color = grayText, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = nextStop,
                                style = TextStyle(
                                    fontSize = 15.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                ),
                                maxLines = 1
                            )
                            if (delay > 0) {
                                Text(
                                    text = "+$delay min",
                                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xFFD32F2F)), fontWeight = FontWeight.Bold)
                                )
                            } else {
                                Text(
                                    text = "pünktlich",
                                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xFF388E3C)), fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(GlanceModifier.width(8.dp))

                        // Ausstieg / Ziel
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "Dein Ausstieg",
                                style = TextStyle(fontSize = 10.sp, color = grayText, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (targetStop.isNotEmpty()) targetStop else "kein Ausstieg gewählt",
                                style = TextStyle(
                                    fontSize = 15.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetStop.isNotEmpty()) textColor else grayText
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    if (targetStop.isNotEmpty() && nextStop.equals(targetStop, ignoreCase = true)) {
                        Spacer(GlanceModifier.height(8.dp))
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(Color(0xFF388E3C)))
                                .cornerRadius(8.dp)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Am nächsten Bahnhof aussteigen!",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(Color.White)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
