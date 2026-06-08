package com.example.stepwars2.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.stepDataStore: DataStore<Preferences> by preferencesDataStore(name = "step_data")

class StepRepository(private val context: Context) {

    companion object {
        private val DAILY_STEPS = intPreferencesKey("daily_steps")
        private val TOTAL_STEPS = longPreferencesKey("total_steps")
        private val LAST_DATE = stringPreferencesKey("last_date")
        private val STEP_GOLD_EARNED = intPreferencesKey("step_gold_earned_today")
    }

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val dailySteps: Flow<Int> = context.stepDataStore.data.map { prefs ->
        val lastDate = prefs[LAST_DATE] ?: ""
        val today = LocalDate.now().format(dateFormatter)
        if (lastDate != today) 0 else prefs[DAILY_STEPS] ?: 0
    }

    val totalSteps: Flow<Long> = context.stepDataStore.data.map { prefs ->
        prefs[TOTAL_STEPS] ?: 0L
    }

    val stepGoldEarnedToday: Flow<Int> = context.stepDataStore.data.map { prefs ->
        val lastDate = prefs[LAST_DATE] ?: ""
        val today = LocalDate.now().format(dateFormatter)
        if (lastDate != today) 0 else prefs[STEP_GOLD_EARNED] ?: 0
    }

    suspend fun updateSteps(sensorSteps: Int) {
        context.stepDataStore.edit { prefs ->
            val today = LocalDate.now().format(dateFormatter)
            val lastDate = prefs[LAST_DATE] ?: ""

            if (lastDate != today) {
                prefs[DAILY_STEPS] = sensorSteps
                prefs[LAST_DATE] = today
                prefs[STEP_GOLD_EARNED] = 0
            } else {
                prefs[DAILY_STEPS] = sensorSteps
            }
        }
    }

    suspend fun addStepGold(amount: Int) {
        context.stepDataStore.edit { prefs ->
            prefs[STEP_GOLD_EARNED] = (prefs[STEP_GOLD_EARNED] ?: 0) + amount
        }
    }
}
