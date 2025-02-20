package com.blu.flagsmith

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.blu.flagsmith.util.ResultEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class BluFlagsmithAnalytics constructor(
    private val context: Context,
    private val dataSource: FlagsmithRemoteDataSource,
    private val flushPeriod: Int
) {
    private val applicationContext: Context = context.applicationContext
    private val currentEvents = getMap()

    private val job = CoroutineScope(Dispatchers.IO).launch {
        if (currentEvents.isNotEmpty())
            dataSource.postAnalytics(currentEvents).apply {
                when(this){
                    is ResultEntity.Error -> {
                        Log.e(
                            "FLAGSMITH",
                            "Failed posting analytics - ${this.error.message}"
                        )
                    }
                    ResultEntity.Loading -> Unit
                    is ResultEntity.Success -> resetMap()
                }
            }

        delay(flushPeriod.toLong() * 1000)
    }


    init {
        job.start()
    }

    companion object {
        private const val EVENTS_KEY = "events"
    }

    /// Counts the instances of a `Flag` being queried.
    fun trackEvent(flagName: String) {
        val currentFlagCount = currentEvents[flagName] ?: 0
        currentEvents[flagName] = currentFlagCount + 1

        // Update events cache
        setMap(currentEvents)
    }

    private fun setMap(updateMap: Map<String, Int?>) {
        val pSharedPref: SharedPreferences =
            context.getSharedPreferences(EVENTS_KEY, Context.MODE_PRIVATE)

        val jsonObject = JSONObject(updateMap)
        val jsonString: String = jsonObject.toString()
        pSharedPref.edit()
            .remove(EVENTS_KEY)
            .putString(EVENTS_KEY, jsonString)
            .apply()
    }

    private fun getMap(): MutableMap<String, Int?> {
        val outputMap: MutableMap<String, Int?> = HashMap()
        val pSharedPref: SharedPreferences =
            applicationContext.getSharedPreferences(EVENTS_KEY, Context.MODE_PRIVATE)
        try {
            val jsonString = pSharedPref.getString(EVENTS_KEY, JSONObject().toString())
            if (jsonString != null) {
                val jsonObject = JSONObject(jsonString)
                val keysItr = jsonObject.keys()
                while (keysItr.hasNext()) {
                    val key = keysItr.next()
                    val value = jsonObject.getInt(key)
                    outputMap[key] = value
                }
            }
        } catch (e: JSONException) {
            Log.e("FLAGSMITH", "Exception in getMap Analytics - ${e.stackTraceToString()}")
        }
        return outputMap
    }

    private fun resetMap() {
        currentEvents.clear()
        setMap(currentEvents)
    }
}
