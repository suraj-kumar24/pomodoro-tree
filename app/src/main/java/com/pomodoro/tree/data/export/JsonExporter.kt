package com.pomodoro.tree.data.export

import com.pomodoro.tree.data.db.PomodoroDatabase
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonExporter @Inject constructor(
    private val db: PomodoroDatabase
) {

    suspend fun exportAll(): String {
        val root = JSONObject()

        // Sessions
        val sessions = db.sessionDao().getDailySummaries("2000-01-01", "2099-12-31")
        val sessionsArray = JSONArray()
        // Export raw sessions via a direct query
        val allSessions = db.sessionDao().getDailySummaries("2000-01-01", "2099-12-31")
        for (s in allSessions) {
            sessionsArray.put(JSONObject().apply {
                put("date", s.date)
                put("completedCount", s.completedCount)
                put("cancelledCount", s.cancelledCount)
                put("totalFocusMinutes", s.totalFocusMinutes)
                put("totalOvertimeMinutes", s.totalOvertimeMinutes)
            })
        }
        root.put("dailySummaries", sessionsArray)

        // Rewards
        val redeemedRewards = db.rewardDao().getTotalRedeemedHours()
        root.put("totalRedeemedRewardHours", redeemedRewards)

        root.put("exportedAt", System.currentTimeMillis())
        root.put("version", 1)

        return root.toString(2)
    }
}
