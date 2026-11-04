package com.nexus.tv

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("nexus_tv", Context.MODE_PRIVATE)

    var accent: Int
        get() = sp.getInt("accent", 0xFF4FC3F7.toInt())
        set(v) = sp.edit().putInt("accent", v).apply()

    var columns: Int
        get() = sp.getInt("columns", 5)
        set(v) = sp.edit().putInt("columns", v).apply()

    var showNonTvApps: Boolean
        get() = sp.getBoolean("non_tv", false)
        set(v) = sp.edit().putBoolean("non_tv", v).apply()

    var favorites: List<String>
        get() = sp.getString("fav", "")!!.split("|").filter { it.isNotEmpty() }
        set(v) = sp.edit().putString("fav", v.joinToString("|")).apply()

    fun toggleFavorite(pkg: String) {
        val l = favorites.toMutableList()
        if (!l.remove(pkg)) l.add(pkg)
        favorites = l
    }

    fun moveFavorite(pkg: String, delta: Int) {
        val l = favorites.toMutableList()
        val i = l.indexOf(pkg)
        val j = i + delta
        if (i < 0 || j !in l.indices) return
        l[i] = l[j].also { l[j] = l[i] }
        favorites = l
    }
}
