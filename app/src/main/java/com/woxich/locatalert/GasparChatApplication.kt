package com.woxich.locatalert

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LocatAlertApplication @Inject constructor(): Application()

/**
 * Name of the application's shared preferences.
 */
const val GASPAR_CHAT_PREFERENCES = "gaspar_chat_preferences"