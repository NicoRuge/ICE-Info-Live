package com.nruge.iceinfo.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.nruge.iceinfo.model.TrainStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {
    fun update(context: Context, status: TrainStatus, isMockMode: Boolean, targetStopName: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(TrainWidget::class.java)
            
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[TrainWidget.KEY_CONNECTED] = status.isConnected
                    prefs[TrainWidget.KEY_TRAIN_NAME] = "${status.trainType} ${status.trainNumber}"
                    prefs[TrainWidget.KEY_SPEED] = status.speed
                    prefs[TrainWidget.KEY_NEXT_STOP] = status.nextStop
                    prefs[TrainWidget.KEY_TARGET_STOP] = targetStopName ?: ""
                    prefs[TrainWidget.KEY_DELAY] = status.delayMinutes
                    prefs[TrainWidget.KEY_MOCK_MODE] = isMockMode
                }
            }
            TrainWidget().updateAll(context)
        }
    }
}
