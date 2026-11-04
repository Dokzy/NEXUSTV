package com.nexus.tv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/** Discovers launchable apps. Must be called off the main thread (loads icons). */
class AppRepository(private val ctx: Context) {

    fun load(includeNonTv: Boolean): List<AppEntry> {
        val pm = ctx.packageManager
        val result = LinkedHashMap<String, AppEntry>()

        query(pm, Intent.CATEGORY_LEANBACK_LAUNCHER).forEach { toEntry(pm, it, true)?.let { e -> result.putIfAbsent(e.packageName, e) } }
        if (includeNonTv) {
            query(pm, Intent.CATEGORY_LAUNCHER).forEach { toEntry(pm, it, false)?.let { e -> result.putIfAbsent(e.packageName, e) } }
        }
        return result.values.sortedBy { it.label.lowercase() }
    }

    private fun query(pm: PackageManager, category: String): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        return if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0)
        }
    }

    private fun toEntry(pm: PackageManager, ri: ResolveInfo, tv: Boolean): AppEntry? {
        val ai = ri.activityInfo ?: return null
        if (ai.packageName == ctx.packageName) return null
        val banner = if (tv) ai.loadBanner(pm) else null
        val image = banner ?: ai.loadIcon(pm)
        return AppEntry(
            packageName = ai.packageName,
            component = android.content.ComponentName(ai.packageName, ai.name),
            label = ri.loadLabel(pm).toString(),
            image = image,
            hasBanner = banner != null,
            isTvApp = tv
        )
    }
}
