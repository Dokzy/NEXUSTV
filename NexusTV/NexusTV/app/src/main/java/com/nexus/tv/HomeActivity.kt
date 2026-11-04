package com.nexus.tv

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class HomeActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepository
    private lateinit var root: View
    private lateinit var title: TextView
    private lateinit var favLabel: TextView
    private lateinit var favRow: RecyclerView
    private lateinit var allGrid: RecyclerView

    private val executor = Executors.newSingleThreadExecutor()
    private var firstLoad = true

    private val favAdapter by lazy { AppAdapter(true, { prefs.accent }, ::launch, ::showOptions) }
    private val allAdapter by lazy { AppAdapter(false, { prefs.accent }, ::launch, ::showOptions) }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = reload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        prefs = Prefs(this)
        repo = AppRepository(this)

        root = findViewById(R.id.root)
        title = findViewById(R.id.title)
        favLabel = findViewById(R.id.favLabel)
        favRow = findViewById(R.id.favRow)
        allGrid = findViewById(R.id.allGrid)

        favRow.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        favRow.adapter = favAdapter
        allGrid.adapter = allAdapter
        findViewById<Button>(R.id.settingsBtn).setOnClickListener { showSettings() }

        // A launcher must never "exit" on Back; it just returns to the top.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = focusTop()
        })

        applyTheme()
    }

    override fun onStart() {
        super.onStart()
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, f, ContextCompat.RECEIVER_EXPORTED)
        reload()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(packageReceiver)
    }

    /** Called when the HOME button is pressed while we're already running. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) focusTop()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    // ---------- data ----------

    private fun reload() {
        executor.execute {
            val loaded = repo.load(prefs.showNonTvApps)
            val favs = prefs.favorites
            val all = loaded.map { it.copy(favorite = it.packageName in favs) }
            val byPkg = all.associateBy { it.packageName }
            val favEntries = favs.mapNotNull { byPkg[it] }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                allAdapter.submit(all)
                favAdapter.submit(favEntries)
                val has = favEntries.isNotEmpty()
                favLabel.visibility = if (has) View.VISIBLE else View.GONE
                favRow.visibility = if (has) View.VISIBLE else View.GONE
                if (firstLoad) { firstLoad = false; focusTop() }
            }
        }
    }

    private fun focusTop() {
        val target = if (favRow.visibility == View.VISIBLE) favRow else allGrid
        target.scrollToPosition(0)
        allGrid.scrollToPosition(0)
        target.post { target.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
    }

    // ---------- actions ----------

    private fun launch(app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(if (app.isTvApp) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
            component = app.component
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        try { startActivity(intent) }
        catch (e: Exception) { Toast.makeText(this, "Can't open ${app.label}", Toast.LENGTH_SHORT).show() }
    }

    /** Long-press OK on an app. */
    private fun showOptions(app: AppEntry) {
        val items = mutableListOf<Pair<String, () -> Unit>>()
        if (app.favorite) {
            items += "Remove from favorites" to { prefs.toggleFavorite(app.packageName); reload() }
            items += "Move earlier" to { prefs.moveFavorite(app.packageName, -1); reload() }
            items += "Move later" to { prefs.moveFavorite(app.packageName, +1); reload() }
        } else {
            items += "Add to favorites" to { prefs.toggleFavorite(app.packageName); reload() }
        }
        items += "App info" to {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
        }
        items += "Uninstall" to {
            startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
        }
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(items.map { it.first }.toTypedArray()) { _, i ->
                try { items[i].second() } catch (e: Exception) {
                    Toast.makeText(this, "Not available on this device", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    // ---------- customization ----------

    private fun applyTheme() {
        val accent = prefs.accent
        title.setTextColor(accent)
        val base = 0xFF0B0F1A.toInt()
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(base, ColorUtils.blendARGB(base, accent, 0.28f))
        )
        allGrid.layoutManager = GridLayoutManager(this, prefs.columns)
        allAdapter.notifyDataSetChanged()
        favAdapter.notifyDataSetChanged()
    }

    private fun showSettings() {
        val labels = arrayOf(
            "Accent color",
            "Grid columns: ${prefs.columns}",
            "Show non-TV apps: ${if (prefs.showNonTvApps) "ON" else "OFF"}",
            "Set NEXUS TV as default launcher",
            "Clear favorites"
        )
        AlertDialog.Builder(this).setTitle("NEXUS TV settings")
            .setItems(labels) { _, i ->
                when (i) {
                    0 -> pickAccent()
                    1 -> pickColumns()
                    2 -> { prefs.showNonTvApps = !prefs.showNonTvApps; reload() }
                    3 -> openDefaultLauncherSettings()
                    4 -> { prefs.favorites = emptyList(); reload() }
                }
            }.show()
    }

    private fun pickAccent() {
        val names = arrayOf("Cyan", "Violet", "Green", "Orange", "Pink", "Red")
        val colors = intArrayOf(0xFF4FC3F7, 0xFFB388FF, 0xFF69F0AE, 0xFFFFB74D, 0xFFFF80AB, 0xFFFF5252)
            .map { it.toInt() }
        AlertDialog.Builder(this).setTitle("Accent color")
            .setItems(names) { _, i -> prefs.accent = colors[i]; applyTheme() }.show()
    }

    private fun pickColumns() {
        val opts = arrayOf("4", "5", "6", "7")
        AlertDialog.Builder(this).setTitle("Grid columns")
            .setItems(opts) { _, i -> prefs.columns = opts[i].toInt(); applyTheme() }.show()
    }

    private fun openDefaultLauncherSettings() {
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            // Many Android TV builds hide the Home picker. Fall back to the system chooser.
            try {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            } catch (_: Exception) { }
            AlertDialog.Builder(this)
                .setTitle("Set as default")
                .setMessage(
                    "Press the HOME button and choose NEXUS TV with \"Always\" if prompted.\n\n" +
                    "If your device doesn't ask, run this from a computer over ADB (no root needed):\n\n" +
                    "adb shell cmd package set-home-activity com.nexus.tv/.HomeActivity"
                )
                .setPositiveButton("OK", null).show()
        }
    }
}
