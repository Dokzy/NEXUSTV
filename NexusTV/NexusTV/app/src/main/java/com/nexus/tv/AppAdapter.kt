package com.nexus.tv

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val horizontal: Boolean,
    private val accent: () -> Int,
    private val onClick: (AppEntry) -> Unit,
    private val onMenu: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    private var items: List<AppEntry> = emptyList()

    fun submit(list: List<AppEntry>) { items = list; notifyDataSetChanged() }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card: View = v.findViewById(R.id.card)
        val icon: ImageView = v.findViewById(R.id.icon)
        val star: TextView = v.findViewById(R.id.star)
        val label: TextView = v.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val app = items[position]
        val d = h.itemView.resources.displayMetrics.density

        if (horizontal) h.itemView.layoutParams.width = (200 * d).toInt()

        h.icon.setImageDrawable(app.image)
        h.icon.scaleType = if (app.hasBanner) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
        val pad = if (app.hasBanner) 0 else (18 * d).toInt()
        h.icon.setPadding(pad, pad, pad, pad)
        h.label.text = app.label
        h.star.visibility = if (app.favorite) View.VISIBLE else View.GONE
        h.star.setTextColor(accent())

        h.card.background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), box(0xFF1E2538.toInt(), accent(), d))
            addState(intArrayOf(), box(0xFF151B2B.toInt(), Color.TRANSPARENT, d))
        }

        h.itemView.setOnClickListener { onClick(app) }
        h.itemView.setOnLongClickListener { onMenu(app); true }
        h.itemView.setOnFocusChangeListener { v, focused ->
            val s = if (focused) 1.1f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            v.elevation = if (focused) 12 * d else 0f
            h.label.setTextColor(if (focused) Color.WHITE else 0xFFB0B8C8.toInt())
        }
    }

    private fun box(fill: Int, stroke: Int, d: Float) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = 10 * d
        if (stroke != Color.TRANSPARENT) setStroke((3 * d).toInt(), stroke)
    }
}
