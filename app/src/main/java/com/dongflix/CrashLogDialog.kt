package com.dongflix

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object CrashLogDialog {

	@JvmStatic
	fun showIfAvailable(activity: Activity?) {
		if (activity == null || activity.isFinishing || activity.isDestroyed) return

		val prefs = activity.getSharedPreferences("dongflix_crash_prefs", Context.MODE_PRIVATE)
		val log = prefs.getString("last_crash_log", "") ?: ""
		if (log.trim().isEmpty()) return

		val scroll = ScrollView(activity)
		val pad = (16 * activity.resources.displayMetrics.density).toInt()
		scroll.setPadding(pad, pad, pad, pad)

		val tv = TextView(activity).apply {
			text = log
			setTextColor(Color.parseColor("#F8FAFC"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
			setTextIsSelectable(true)
		}
		scroll.addView(tv)

		AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
			.setTitle("Log Crash Terakhir")
			.setView(scroll)
			.setCancelable(false)
			.setPositiveButton("Salin Error") { _, _ ->
				val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
				if (cm != null) {
					val clip = ClipData.newPlainText("Dongflix Crash Log", log)
					cm.setPrimaryClip(clip)
					Toast.makeText(activity, "Log error disalin ke clipboard", Toast.LENGTH_SHORT).show()
				}
				prefs.edit().remove("last_crash_log").apply()
			}
			.setNegativeButton("Hapus Log") { _, _ ->
				prefs.edit().remove("last_crash_log").apply()
			}
			.show()
	}
}
