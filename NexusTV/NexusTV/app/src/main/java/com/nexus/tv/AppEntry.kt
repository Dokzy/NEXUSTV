package com.nexus.tv

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class AppEntry(
    val packageName: String,
    val component: ComponentName,
    val label: String,
    val image: Drawable,
    val hasBanner: Boolean,
    val isTvApp: Boolean,
    val favorite: Boolean = false
)
