package com.allowance.manager.core.datastore

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    val BUDGET = longPreferencesKey("budget")
    val REMAINING_BALANCE = longPreferencesKey("remaining_balance")
    val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
}
