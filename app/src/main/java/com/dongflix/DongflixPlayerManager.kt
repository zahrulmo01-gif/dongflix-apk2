package com.dongflix

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Outline
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur

class DongflixPlayerManager {

	companion object {
		const val CHANNEL_ID = "dongflix_playback_channel"
		const val NOTIF_ID = 7788
		const val ACTION_TOGGLE_PLAY = "com.dongflix.ACTION_TOGGLE_PLAY"
		const val ACTION_CLOSE_PLAYER = "com.dongflix.ACTION_CLOSE_PLAYER"

		@Volatile
		private var instance: DongflixPlayerManager? = null

		@JvmStatic
		fun getInstance(): DongflixPlayerManager {
			return instance ?: synchronized(this) {
				instance ?: DongflixPlayerManager().also { instance = it }
			}
		}
	}

	@JvmField var sharedMediaPlayer: MediaPlayer? = null
	@JvmField var isMiniPlayerActive: Boolean = false

	@JvmField var activeSlug: String = ""
	@JvmField var activeTitle: String = ""
	@JvmField var activeEpisodeLabel: String = ""
	@JvmField var activeCoverUrl: String = ""
	@JvmField var activeStreamUrl: String = ""
	@JvmField var activeEpisodeIndex: Int = 0
	@JvmField var activeEpisodeList: ArrayList<HashMap<String, Any>> = ArrayList()

	@JvmField var cachedAllCatalogJson: String = ""
	@JvmField var cachedEpisodesJson: String = ""
	@JvmField var activeServerName: String = ""
	@JvmField var activeSelectedServerIndex: Int = 0
	@JvmField var activeServerNames: ArrayList<String> = ArrayList()
	@JvmField var activeServerUrls: ArrayList<String> = ArrayList()
	@JvmField var activeQualityNames: ArrayList<String> = ArrayList()
	@JvmField var activeQualityUrls: ArrayList<String> = ArrayList()
	@JvmField var activeSelectedQualityIndex: Int = 0
	@JvmField var activeIsAutoQuality: Boolean = true

	@JvmField var isScrolledDownCompact: Boolean = false

	private var isReceiverRegistered = false
	private var appContext: Application? = null
	private var startedActivityCount = 0

	fun initWithApplication(app: Application) {
		if (appContext != null) return
		appContext = app
		createNotificationChannel(app)
		registerNotificationReceiver(app)

		app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
			override fun onActivityStarted(activity: Activity) {
				startedActivityCount++
			}

			override fun onActivityStopped(activity: Activity) {
				startedActivityCount = maxOf(0, startedActivityCount - 1)
				if (startedActivityCount == 0 && sharedMediaPlayer != null) {
					try {
						if (sharedMediaPlayer?.isPlaying == true) {
							sharedMediaPlayer?.pause()
							if (isMiniPlayerActive) {
								updatePlaybackNotification(activity)
							}
						}
					} catch (ignored: Exception) {
					}
				}
			}

			override fun onActivityResumed(activity: Activity) {
				if (activity is WatchActivity) {
					removeMiniPlayerFromActivity(activity)
				} else if (isMiniPlayerActive && sharedMediaPlayer != null) {
					attachMiniPlayerToActivity(activity)
					updatePlaybackNotification(activity)
				}
			}

			override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
			override fun onActivityPaused(activity: Activity) {}
			override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
			override fun onActivityDestroyed(activity: Activity) {}
		})
	}

	private fun createNotificationChannel(ctx: Context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				CHANNEL_ID,
				"Pemutar Video Dongflix",
				NotificationManager.IMPORTANCE_LOW
			).apply {
				description = "Kontrol pemutaran Dongflix"
				setShowBadge(false)
			}
			val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
			nm?.createNotificationChannel(channel)
		}
	}

	private fun registerNotificationReceiver(ctx: Context) {
		if (isReceiverRegistered) return
		val filter = IntentFilter().apply {
			addAction(ACTION_TOGGLE_PLAY)
			addAction(ACTION_CLOSE_PLAYER)
		}

		val receiver = object : BroadcastReceiver() {
			override fun onReceive(context: Context, intent: Intent) {
				when (intent.action) {
					ACTION_TOGGLE_PLAY -> {
						sharedMediaPlayer?.let { mp ->
							try {
								if (mp.isPlaying) {
									mp.pause()
								} else {
									mp.start()
								}
								updatePlaybackNotification(context)
							} catch (ignored: Exception) {
							}
						}
					}
					ACTION_CLOSE_PLAYER -> {
						stopAndCloseEverything(context)
					}
				}
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
		} else {
			ctx.registerReceiver(receiver, filter)
		}
		isReceiverRegistered = true
	}

	fun updatePlaybackNotification(ctx: Context) {
		if (!isMiniPlayerActive || sharedMediaPlayer == null) return

		var isPlaying = false
		try {
			isPlaying = sharedMediaPlayer?.isPlaying == true
		} catch (ignored: Exception) {
		}

		val openWatchIntent = Intent(ctx, WatchActivity::class.java).apply {
			putExtra("slug", activeSlug)
			putExtra("resume_mini_player", true)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
		}

		val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		} else {
			PendingIntent.FLAG_UPDATE_CURRENT
		}

		val contentPi = PendingIntent.getActivity(ctx, 100, openWatchIntent, flags)
		val togglePi = PendingIntent.getBroadcast(
			ctx, 101, Intent(ACTION_TOGGLE_PLAY).setPackage(ctx.packageName), flags
		)
		val closePi = PendingIntent.getBroadcast(
			ctx, 102, Intent(ACTION_CLOSE_PLAYER).setPackage(ctx.packageName), flags
		)

		val subText = if (activeEpisodeLabel.isEmpty()) "Sedang memutar" else "Episode $activeEpisodeLabel"

		val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_play1)
			.setContentTitle(if (activeTitle.isEmpty()) "Dongflix" else activeTitle)
			.setContentText(subText)
			.setContentIntent(contentPi)
			.setOngoing(isPlaying)
			.setOnlyAlertOnce(true)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.addAction(
				if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play1,
				if (isPlaying) "Jeda" else "Putar",
				togglePi
			)
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Tutup", closePi)

		val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
		nm?.notify(NOTIF_ID, builder.build())
	}

	fun clearCache() {
		cachedAllCatalogJson = ""
		cachedEpisodesJson = ""
		activeServerName = ""
		activeSelectedServerIndex = 0
		activeServerNames.clear()
		activeServerUrls.clear()
		activeQualityNames.clear()
		activeQualityUrls.clear()
		activeSelectedQualityIndex = 0
		activeIsAutoQuality = true
	}

	fun stopAndCloseEverything(ctx: Context) {
		isMiniPlayerActive = false
		isScrolledDownCompact = false
		clearCache()
		sharedMediaPlayer?.let { mp ->
			try {
				if (mp.isPlaying) mp.stop()
				mp.reset()
				mp.release()
			} catch (ignored: Exception) {
			}
		}
		sharedMediaPlayer = null

		val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
		nm?.cancel(NOTIF_ID)

		if (ctx is Activity) {
			removeMiniPlayerFromActivity(ctx)
			if (ctx is MainActivity) {
				ctx.resetNavbarToExpanded()
			}
		}
	}

	fun removeMiniPlayerFromActivity(activity: Activity) {
		val decor = activity.window.decorView as? ViewGroup
		val existing = decor?.findViewWithTag<View>("DONGFLIX_MINI_PLAYER")
		if (existing?.parent is ViewGroup) {
			(existing.parent as ViewGroup).removeView(existing)
		}
		val inlineSlot = activity.findViewById<FrameLayout>(R.id.mini_player_inline_slot)
		if (inlineSlot != null) {
			inlineSlot.removeAllViews()
			inlineSlot.visibility = View.GONE
		}
	}

	fun setCompactScrollMode(activity: Activity, compact: Boolean) {
		if (!isMiniPlayerActive || sharedMediaPlayer == null) {
			isScrolledDownCompact = false
			return
		}
		if (isScrolledDownCompact == compact) return
		isScrolledDownCompact = compact
		attachMiniPlayerToActivity(activity)
	}

	private fun buildMiniPlayerPillView(activity: Activity, isInlineCompact: Boolean): View {
		val d = activity.resources.displayMetrics.density

		val glassRoot = FrameLayout(activity).apply {
			tag = "DONGFLIX_MINI_PLAYER"
			outlineProvider = object : ViewOutlineProvider() {
				override fun getOutline(view: View, outline: Outline) {
					outline.setRoundRect(0, 0, view.width, view.height, 1000f)
				}
			}
			clipToOutline = true
		}

		val blurBg = BlurView(activity).apply {
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			)
		}
		var rootMain = activity.findViewById<ViewGroup>(R.id.root_main_container)
		if (rootMain == null && activity.window.decorView is ViewGroup) {
			rootMain = activity.window.decorView as ViewGroup
		}
		if (rootMain != null) {
			val windowBackground = activity.window.decorView.background
			blurBg.setupWith(rootMain, RenderScriptBlur(activity))
				.setFrameClearDrawable(windowBackground)
				.setBlurRadius(20f)
			blurBg.setOverlayColor(Color.parseColor("#66141824"))
		}

		val glassBorder = GradientDrawable().apply {
			setColor(Color.TRANSPARENT)
			cornerRadius = 1000f
			setStroke((1 * d).toInt(), Color.parseColor("#33FFFFFF"))
		}
		glassRoot.foreground = glassBorder
		glassRoot.addView(blurBg)

		val hPad = if (isInlineCompact) (8 * d).toInt() else (10 * d).toInt()
		val vPad = if (isInlineCompact) (6 * d).toInt() else (8 * d).toInt()
		val row = LinearLayout(activity).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding(hPad, vPad, hPad, vPad)
		}
		glassRoot.addView(row)

		val thumbW = if (isInlineCompact) (44 * d).toInt() else (56 * d).toInt()
		val thumbH = (44 * d).toInt()
		val thumbCard = CardView(activity).apply {
			layoutParams = LinearLayout.LayoutParams(thumbW, thumbH)
			radius = if (isInlineCompact) 22 * d else 12 * d
			cardElevation = 0f
			setCardBackgroundColor(Color.BLACK)
		}

		val miniTexture = TextureView(activity).apply {
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			)
			surfaceTextureListener = object : TextureView.SurfaceTextureListener {
				override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
					sharedMediaPlayer?.let { mp ->
						try {
							mp.setSurface(Surface(surface))
						} catch (ignored: Exception) {
						}
					}
				}

				override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {}
				override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean = true
				override fun onSurfaceTextureUpdated(s: SurfaceTexture) {}
			}
		}
		thumbCard.addView(miniTexture)
		row.addView(thumbCard)

		val infoCol = LinearLayout(activity).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
				setMargins((10 * d).toInt(), 0, (6 * d).toInt(), 0)
			}
		}

		val tvTitle = TextView(activity).apply {
			text = if (activeTitle.isEmpty()) "Dongflix" else activeTitle
			setTextColor(Color.WHITE)
			textSize = if (isInlineCompact) 13f else 14f
			setTypeface(null, Typeface.BOLD)
			maxLines = 1
			ellipsize = TextUtils.TruncateAt.END
		}
		infoCol.addView(tvTitle)

		val tvSub = TextView(activity).apply {
			text = if (activeEpisodeLabel.isEmpty()) "Dongflix" else "Episode $activeEpisodeLabel"
			setTextColor(Color.parseColor("#9CA3AF"))
			textSize = if (isInlineCompact) 11f else 12f
			maxLines = 1
			ellipsize = TextUtils.TruncateAt.END
		}
		infoCol.addView(tvSub)

		row.addView(infoCol)

		var playingNow = false
		try {
			playingNow = sharedMediaPlayer?.isPlaying == true
		} catch (ignored: Exception) {
		}

		val btnMiniPlay = ImageView(activity).apply {
			layoutParams = LinearLayout.LayoutParams((34 * d).toInt(), (34 * d).toInt())
			setPadding((7 * d).toInt(), (7 * d).toInt(), (7 * d).toInt(), (7 * d).toInt())
			setColorFilter(Color.WHITE)
			setImageResource(if (playingNow) R.drawable.ic_pause else R.drawable.ic_play1)
			setOnClickListener {
				sharedMediaPlayer?.let { mp ->
					try {
						if (mp.isPlaying) {
							mp.pause()
							setImageResource(R.drawable.ic_play1)
						} else {
							mp.start()
							setImageResource(R.drawable.ic_pause)
						}
						setColorFilter(Color.WHITE)
						updatePlaybackNotification(activity)
					} catch (ignored: Exception) {
					}
				}
			}
		}
		row.addView(btnMiniPlay)

		if (!isInlineCompact) {
			val btnClose = TextView(activity).apply {
				layoutParams = LinearLayout.LayoutParams((30 * d).toInt(), (30 * d).toInt()).apply {
					leftMargin = (2 * d).toInt()
				}
				gravity = Gravity.CENTER
				text = "✕"
				setTextColor(Color.parseColor("#CBD5E1"))
				textSize = 13f
				setTypeface(null, Typeface.BOLD)
				setOnClickListener {
					stopAndCloseEverything(activity)
				}
			}
			row.addView(btnClose)
		}

		glassRoot.setOnClickListener {
			val intent = Intent(activity, WatchActivity::class.java).apply {
				putExtra("slug", activeSlug)
				putExtra("resume_mini_player", true)
			}
			activity.startActivity(intent)
			activity.overridePendingTransition(R.anim.slide_up_enter, R.anim.no_anim)
		}

		return glassRoot
	}

	fun attachMiniPlayerToActivity(activity: Activity) {
		if (!isMiniPlayerActive || sharedMediaPlayer == null) return

		removeMiniPlayerFromActivity(activity)
		val d = activity.resources.displayMetrics.density

		val inlineSlot = activity.findViewById<FrameLayout>(R.id.mini_player_inline_slot)
		val bottomNav = activity.findViewById<View>(R.id.navbar_wrapper)

		if (isScrolledDownCompact && inlineSlot != null) {
			inlineSlot.visibility = View.VISIBLE
			val inlinePill = buildMiniPlayerPillView(activity, true)
			inlineSlot.addView(
				inlinePill,
				FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT
				)
			)
			return
		}

		val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

		val isMainScreen = activity is MainActivity
		val sideMarginPx = (14 * d).toInt()
		val bottomMarginPx = if (isMainScreen) {
			if (bottomNav != null && bottomNav.visibility == View.VISIBLE) (92 * d).toInt() else (16 * d).toInt()
		} else {
			if (bottomNav != null && bottomNav.visibility == View.VISIBLE) (102 * d).toInt() else (28 * d).toInt()
		}

		val floatingPill = buildMiniPlayerPillView(activity, false)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			floatingPill.elevation = 24 * d
		}

		val rootLp = FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			(60 * d).toInt()
		).apply {
			gravity = Gravity.BOTTOM
			setMargins(sideMarginPx, 0, sideMarginPx, bottomMarginPx)
		}
		contentRoot.addView(floatingPill, rootLp)
	}
}
