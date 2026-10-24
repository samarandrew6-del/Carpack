package pl.statusaut

import android.content.Context
import android.content.SharedPreferences

/** Proste ustawienia lokalne: zapamiętane imię, telefon i kod dostępu do tablicy. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences("status_aut", Context.MODE_PRIVATE)
    }

    var boardCode: String
        get() = sp.getString("board_code", "") ?: ""
        set(value) { sp.edit().putString("board_code", value).apply() }

    var name: String
        get() = sp.getString("name", "") ?: ""
        set(value) { sp.edit().putString("name", value).apply() }

    var phone: String
        get() = sp.getString("phone", "") ?: ""
        set(value) { sp.edit().putString("phone", value).apply() }
}
