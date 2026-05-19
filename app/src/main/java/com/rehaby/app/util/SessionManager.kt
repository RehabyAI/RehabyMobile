package com.rehaby.app.util

import android.content.Context
import com.rehaby.app.data.remote.CreatePatientResponseDto

object SessionManager {

    private const val PREF_NAME = "rehaby_session"
    private const val KEY_PATIENT_ID = "patient_id"
    private const val KEY_FULL_NAME = "full_name"

    fun savePatient(context: Context, response: CreatePatientResponseDto) {
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_PATIENT_ID, response.patient_id)
            .putString(KEY_FULL_NAME, response.full_name)
            .apply()
    }

    fun getPatientId(context: Context): String? =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PATIENT_ID, null)

    fun getFullName(context: Context): String? =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FULL_NAME, null)

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .clear()
            .apply()
    }
}
