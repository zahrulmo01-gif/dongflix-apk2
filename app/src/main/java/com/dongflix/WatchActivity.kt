package com.dongflix

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class WatchActivity : AppCompatActivity() {

	companion object {
		private const val FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app"
		private const val CATALOG_URL = "https://dongflix-api1.zahrulmo01.workers.dev/"
		private const val ADMIN_EMAIL = "zahrulms01@gmail.com"

		private val DEFAULT_STICKERS = arrayOf(
			"https://cdn-icons-png.flaticon.com/128/9408/9408175.png",
			"https://media.tenor.com/On7KVXhzuf8AAAAj/chibi-cat.gif",
			"https://cdn-icons-png.flaticon.com/128/9408/9408201.png",
			"https://media.tenor.com/2s_jOsl0e8MAAAAj/dance-happy.gif",
			"https://cdn-icons-png.flaticon.com/128/9408/9408166.png",
			"https://media.tenor.com/kHIdycPoP6AAAAAj/anime-cry.gif",
			"https://cdn-icons-png.flaticon.com/128/9408/9408224.png",
			"https://media.tenor.com/gK2J_J7KkE8AAAAj/cute-love.gif"
		)

		private val STOP_WORDS = setOf(
			"yang", "dan", "di", "ke", "dari", "ini", "itu", "untuk", "dengan", "pada", "adalah",
			"sebagai", "dalam", "tidak", "akan", "oleh", "ia", "dia", "mereka", "nya", "atau",
			"the", "of", "and", "in", "to", "a", "is", "for", "with", "on", "as", "by", "at",
			"sub", "indo", "episode", "season", "donghua", "anime", "cerita", "kisah", "dunia"
		)
	}

	private var eps = ArrayList<HashMap<String, Any>>()
	private lateinit var reqWatch: RequestNetwork
	private lateinit var reqWatchRequestListener: RequestNetwork.RequestListener

	private var playerTextureView: TextureView? = null
	private var fallbackWebView: WebView? = null
	private var layoutPlayer: FrameLayout? = null
	private var playerControlsOverlay: View? = null
	private var gradientTop: View? = null
	private var gradientBottom: View? = null
	private var topBar: View? = null
	private var bottomControls: View? = null
	private var dividerLineView: View? = null

	private var btnBack: ImageView? = null
	private var btnCast: ImageView? = null
	private var btnLock: ImageView? = null
	private var btnSettings: ImageView? = null
	private var btnPrevEpisode: ImageView? = null
	private var btnRewind: ImageView? = null
	private var btnPlay: ImageView? = null
	private var btnForward: ImageView? = null
	private var btnNextEpisode: ImageView? = null
	private var btnFullscreen: ImageView? = null
	private var imagePoster: ImageView? = null
	private var iconExpandDetail: ImageView? = null
	private var iconCommentChevron: ImageView? = null

	private var textPlayerTitle: TextView? = null
	private var textTime: TextView? = null
	private var btnFullscreenEpisodes: TextView? = null
	private var btnQuality: TextView? = null
	private var textJudul: TextView? = null
	private var textRating: TextView? = null
	private var textYear: TextView? = null
	private var textGenre: TextView? = null
	private var textActionServer: TextView? = null
	private var textSortEpisodes: TextView? = null
	private var textCommentPreviewLabel: TextView? = null
	private var textCommentCount: TextView? = null

	private var animSeekLeft: LinearLayout? = null
	private var animSeekRight: LinearLayout? = null
	private var btnUnlockScreen: LinearLayout? = null
	private var pillSpeedHold: LinearLayout? = null
	private var btnFill: LinearLayout? = null
	private var btnOpenDetail: LinearLayout? = null
	private var btnSortEpisodes: LinearLayout? = null
	private var btnActionServer: LinearLayout? = null
	private var btnActionDownload: LinearLayout? = null
	private var btnActionShare: LinearLayout? = null
	private var btnActionReport: LinearLayout? = null
	private var btnCommentSection: LinearLayout? = null

	private var playerLoading: ProgressBar? = null
	private var miniVideoProgressBar: ProgressBar? = null
	private var playerSeekBar: SeekBar? = null
	private var recyclerEpisode: RecyclerView? = null
	private var recyclerRekomendasi: RecyclerView? = null

	private var currentSlug = ""
	private var detailTitle = ""
	private var detailSynopsis = ""
	private var detailStatus = "Ongoing"
	private var detailYear = "2026"
	private var detailFirstGenre = "-"
	private var detailAllGenres = "-"
	private var detailCover = ""
	private val currentGenresList = ArrayList<String>()

	private var isNewestFirst = true
	private var isScreenLocked = false
	private var hideUnlockBtnRunnable: Runnable? = null

	private var isHolding2xSpeed = false

	private var shimmerAnimator: ValueAnimator? = null
	private var shimmerPhase = 0f
	private var isDetailLoading = true
	private var isEpisodesLoading = true
	private var isRekomLoading = true

	@JvmField var mediaPlayer: MediaPlayer? = null
	@JvmField var playerHandler: Handler? = null
	@JvmField var playerSurface: Surface? = null
	@JvmField var progressLoop: Runnable? = null
	@JvmField var hideControlsRunnable: Runnable? = null
	@JvmField var autoDowngradeRunnable: Runnable? = null
	@JvmField var areControlsVisible = true
	@JvmField var currentPlayingUrl = ""
	@JvmField var currentStreamReferer = ""
	@JvmField var isFullscreen = false
	@JvmField var savedSeekPosition = 0

	@JvmField var currentVideoWidth = 0
	@JvmField var currentVideoHeight = 0
	@JvmField var isFillModeActive = false

	@JvmField var isAutoQualityMode = true
	@JvmField var selectedQualityIndex = 0
	@JvmField var currentPlaybackSpeed = 1.0f

	@JvmField var currentEpisodeIndex = 0
	@JvmField var episodeList = ArrayList<HashMap<String, Any>>()

	@JvmField var currentEpisodeServerNames = ArrayList<String>()
	@JvmField var currentEpisodeServerUrls = ArrayList<String>()
	@JvmField var selectedServerIndex = 0

	@JvmField var qualityNames = ArrayList<String>()
	@JvmField var qualityUrls = ArrayList<String>()

	@JvmField var dmSessionCookie = ""
	private var isWebViewFallbackActive = false
	private var isResumingFromMiniPlayer = false

	private var pendingHistoryEpisode = ""
	private var pendingHistoryEpisodeIndex = -1
	private var pendingHistoryPositionMs = 0L
	private var historyLoaded = false
	private var initialEpisodeStarted = false
	private var lastHistorySyncMs = 0L
	private var hasRewardedWatchExp = false

	private var commentsRef: DatabaseReference? = null
	private var commentsListener: ValueEventListener? = null
	private val userProfileMap = HashMap<String, HashMap<String, String>>()
	private val allUsersDirectory = ArrayList<HashMap<String, String>>()
	private val mainCommentList = ArrayList<HashMap<String, Any>>()
	private val repliesByParentMap = HashMap<String, MutableList<HashMap<String, Any>>>()
	private val stickerList = ArrayList<String>()

	private var activeCommentSheetAdapter: TikTokCommentAdapter? = null
	private var activeCommentSheetHeader: TextView? = null
	private var outfitFont: Typeface? = null

	private fun interface OnStickerPickedListener {
		fun onStickerPicked(url: String)
	}

	private fun interface OnReplyClickListener {
		fun onReplyRequested(parentCommentId: String, mentionTag: String)
	}

	private fun interface OnSheetItemClickListener {
		fun onItemClick(index: Int)
	}

	private data class ScoredAnimeItem(
		val item: HashMap<String, Any>,
		val score: Int
	)

	override fun onCreate(_savedInstanceState: Bundle?) {
		super.onCreate(_savedInstanceState)
		setContentView(R.layout.activity_watch)
		DongflixPlayerManager.getInstance().initWithApplication(application)
		initialize(_savedInstanceState)
		initializeLogic()
	}

	private fun getOutfitTypeface(): Typeface {
		outfitFont?.let { return it }
		val candidates = arrayOf(
			"fonts/outfit_regular.ttf",
			"fonts/outfit_medium.ttf",
			"fonts/outfit.ttf",
			"fonts/Outfit-Regular.ttf",
			"fonts/productsans_reg.ttf"
		)
		for (path in candidates) {
			try {
				val tf = Typeface.createFromAsset(assets, path)
				if (tf != null) {
					outfitFont = tf
					return tf
				}
			} catch (ignored: Exception) {
			}
		}
		outfitFont = Typeface.DEFAULT
		return outfitFont!!
	}

	private fun applyOutfitToAllViews() {
		val tf = getOutfitTypeface()
		val content = window.decorView.findViewById<View>(android.R.id.content) ?: return

		val queue: Queue<View> = LinkedList()
		queue.add(content)
		while (queue.isNotEmpty()) {
			val v = queue.poll()
			if (v is TextView) {
				v.setTypeface(tf, Typeface.NORMAL)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					v.letterSpacing = -0.01f
				}
			} else if (v is ViewGroup) {
				for (i in 0 until v.childCount) {
					queue.add(v.getChildAt(i))
				}
			}
		}
	}

	private fun resolveVectorDrawable(resName: String, fallback: Drawable): Drawable {
		val id = resources.getIdentifier(resName, "drawable", packageName)
		if (id != 0) {
			val d = ContextCompat.getDrawable(this, id)
			if (d != null) return d.mutate()
		}
		return fallback
	}

	private fun createStickerSvgDrawable(color: Int, strokePx: Float): Drawable {
		return object : Drawable() {
			private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
				this.color = color
				style = Paint.Style.STROKE
				strokeWidth = strokePx
				strokeCap = Paint.Cap.ROUND
				strokeJoin = Paint.Join.ROUND
			}
			private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
				this.color = color
				style = Paint.Style.FILL
			}
			private val path = Path()

			override fun draw(canvas: Canvas) {
				val b = bounds
				val w = b.width().toFloat()
				val h = b.height().toFloat()
				if (w <= 0f || h <= 0f) return

				val cx = b.exactCenterX()
				val cy = b.exactCenterY()
				val r = minOf(w, h) * 0.40f

				val oval = RectF(cx - r, cy - r, cx + r, cy + r)
				path.reset()
				path.addArc(oval, 90f, 270f)
				path.lineTo(cx + r, cy)
				path.quadTo(cx, cy, cx, cy + r)
				canvas.drawPath(path, strokePaint)

				path.reset()
				path.moveTo(cx, cy + r)
				path.lineTo(cx + r, cy)
				canvas.drawPath(path, strokePaint)

				val eyeR = maxOf(1.5f, r * 0.11f)
				canvas.drawCircle(cx - r * 0.34f, cy - r * 0.18f, eyeR, fillPaint)
				canvas.drawCircle(cx + r * 0.24f, cy - r * 0.28f, eyeR, fillPaint)

				val smileRect = RectF(cx - r * 0.42f, cy - r * 0.22f, cx + r * 0.32f, cy + r * 0.36f)
				canvas.drawArc(smileRect, 25f, 120f, false, strokePaint)
			}

			override fun setAlpha(alpha: Int) {
				strokePaint.alpha = alpha
				fillPaint.alpha = alpha
			}

			override fun setColorFilter(colorFilter: ColorFilter?) {
				strokePaint.colorFilter = colorFilter
				fillPaint.colorFilter = colorFilter
			}

			@Deprecated("Deprecated in Java")
			override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
		}
	}

	private fun createSendSvgDrawable(color: Int, strokePx: Float): Drawable {
		return object : Drawable() {
			private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
				this.color = color
				style = Paint.Style.STROKE
				strokeWidth = strokePx
				strokeCap = Paint.Cap.ROUND
				strokeJoin = Paint.Join.ROUND
			}
			private val path = Path()

			override fun draw(canvas: Canvas) {
				val b = bounds
				val w = b.width().toFloat()
				val h = b.height().toFloat()
				if (w <= 0f || h <= 0f) return

				val p = minOf(w, h) * 0.18f
				val x1 = b.left + p
				val y1 = b.top + p
				val x2 = b.right - p
				val y2 = b.bottom - p
				val midX = b.exactCenterX()
				val midY = b.exactCenterY()

				path.reset()
				path.moveTo(x2, y1)
				path.lineTo(midX + (w * 0.12f), y2)
				path.lineTo(midX - (w * 0.04f), midY + (h * 0.04f))
				path.lineTo(x1, midY - (h * 0.12f))
				path.close()

				path.moveTo(x2, y1)
				path.lineTo(midX - (w * 0.04f), midY + (h * 0.04f))
				canvas.drawPath(path, paint)
			}

			override fun setAlpha(alpha: Int) {
				paint.alpha = alpha
			}

			override fun setColorFilter(colorFilter: ColorFilter?) {
				paint.colorFilter = colorFilter
			}

			@Deprecated("Deprecated in Java")
			override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
		}
	}

	private fun formatRelativeOrDateTime(ts: Long): String {
		if (ts <= 0L) return "Baru saja"
		val now = System.currentTimeMillis()
		val diffMs = now - ts
		if (diffMs < 60_000L) return "Baru saja"
		val minutes = diffMs / 60_000L
		if (minutes < 60L) return "$minutes menit yang lalu"
		val hours = diffMs / 3_600_000L
		if (hours < 24L) return "$hours jam yang lalu"
		val days = diffMs / 86_400_000L
		if (days <= 7L) return "$days hari yang lalu"

		val commentCal = Calendar.getInstance().apply { timeInMillis = ts }
		val nowCal = Calendar.getInstance().apply { timeInMillis = now }

		val month = commentCal.get(Calendar.MONTH) + 1
		val day = commentCal.get(Calendar.DAY_OF_MONTH)
		if (commentCal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR)) {
			val year2 = commentCal.get(Calendar.YEAR) % 100
			return String.format(Locale.US, "%d-%d-%02d", month, day, year2)
		}
		return String.format(Locale.US, "%d-%d", month, day)
	}

	private fun sanitizeUserKey(email: String?, uid: String?): String {
		if (!email.isNullOrBlank()) {
			return email.trim().lowercase(Locale.US).replace(".", ",")
		}
		return uid?.trim() ?: ""
	}

	private fun getCurrentUserKey(): String {
		val user = FirebaseAuth.getInstance().currentUser ?: return ""
		return sanitizeUserKey(user.email, user.uid)
	}

	private fun calculateLevel(expVal: Long): Int {
		if (expVal <= 0L) return 1
		return ((expVal - 1L) / 1000L).toInt() + 1
	}

	private fun addExpToCurrentUser(amount: Long) {
		val key = getCurrentUserKey()
		if (key.isEmpty() || amount <= 0L) return

		val expRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("users")
			.child(key)
			.child("exp")

		expRef.runTransaction(object : Transaction.Handler {
			override fun doTransaction(currentData: MutableData): Transaction.Result {
				var cur = 0L
				val value = currentData.value
				if (value is Number) {
					cur = value.toLong()
				} else if (value != null) {
					try {
						cur = value.toString().trim().toLong()
					} catch (ignored: Exception) {
					}
				}
				currentData.value = cur + amount
				return Transaction.success(currentData)
			}

			override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
				if (committed && currentData != null) {
					val updatedExp = parseLongSafe(currentData.value, 0L)
					val prof = userProfileMap[key]
					if (prof != null) {
						prof["exp"] = updatedExp.toString()
						activeCommentSheetAdapter?.notifyDataSetChanged()
					}
				}
			}
		})
	}

	private fun loadUsersAndStickersFromFirebase() {
		val rootDb = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference

		rootDb.child("users").addValueEventListener(object : ValueEventListener {
			override fun onDataChange(snap: DataSnapshot) {
				if (isFinishing || isDestroyed || !snap.exists()) return
				userProfileMap.clear()
				allUsersDirectory.clear()
				val seenKeys = HashSet<String>()

				for (child in snap.children) {
					val key = child.key ?: continue

					val email = child.child("email").value?.toString()?.trim() ?: ""
					val username = child.child("username").value?.toString()?.trim() ?: ""
					val name = child.child("name").value?.toString()?.trim() ?: ""
					val photo = child.child("photoURL").value?.toString()?.trim() ?: ""
					val exp = parseLongSafe(child.child("exp").value, 0L)

					val fallbackUser = when {
						username.isNotEmpty() -> username
						email.isNotEmpty() && email.contains("@") -> email.substring(0, email.indexOf("@"))
						else -> key
					}
					val displayName = if (name.isNotEmpty()) name else fallbackUser

					val profile = hashMapOf(
						"key" to key,
						"email" to email.lowercase(Locale.US),
						"username" to fallbackUser,
						"name" to displayName,
						"photoURL" to photo,
						"exp" to exp.toString()
					)

					userProfileMap[key] = profile
					if (email.isNotEmpty()) {
						userProfileMap[email.lowercase(Locale.US).replace(".", ",")] = profile
					}

					val dedupKey = fallbackUser.lowercase(Locale.US)
					if (dedupKey.isNotEmpty() && seenKeys.add(dedupKey)) {
						allUsersDirectory.add(profile)
					}
				}

				activeCommentSheetAdapter?.notifyDataSetChanged()
			}

			override fun onCancelled(error: DatabaseError) {}
		})

		stickerList.clear()
		Collections.addAll(stickerList, *DEFAULT_STICKERS)

		rootDb.child("stickers").addListenerForSingleValueEvent(object : ValueEventListener {
			override fun onDataChange(snap: DataSnapshot) {
				if (snap.exists()) {
					val loaded = ArrayList<String>()
					for (child in snap.children) {
						val value = child.value
						if (value is String && value.trim().isNotEmpty()) {
							loaded.add(value.trim())
						} else if (value is Map<*, *>) {
							val u = value["url"] ?: value["link"] ?: value["image"]
							if (u != null && u.toString().trim().isNotEmpty()) {
								loaded.add(u.toString().trim())
							}
						}
					}
					if (loaded.isNotEmpty()) {
						stickerList.clear()
						stickerList.addAll(loaded)
					}
				}
			}

			override fun onCancelled(error: DatabaseError) {}
		})
	}

	private fun resolveUserProfile(email: String?, uid: String?): HashMap<String, String> {
		val key = sanitizeUserKey(email, uid)
		if (key.isNotEmpty() && userProfileMap.containsKey(key)) {
			return userProfileMap[key]!!
		}
		if (!uid.isNullOrBlank() && userProfileMap.containsKey(uid.trim())) {
			return userProfileMap[uid.trim()]!!
		}
		val derived = if (email != null && email.contains("@")) email.substring(0, email.indexOf("@")) else "User"
		return hashMapOf(
			"email" to (email?.lowercase(Locale.US) ?: ""),
			"username" to derived,
			"name" to derived,
			"photoURL" to "",
			"exp" to "0"
		)
	}

	private fun bindAvatarOrInitial(avatarView: ImageView?, initialView: TextView?, photoUrl: String?, displayName: String?) {
		val initial = if (!displayName.isNullOrBlank()) {
			displayName.trim().substring(0, 1).uppercase(Locale.US)
		} else {
			"U"
		}

		if (!photoUrl.isNullOrBlank()) {
			avatarView?.apply {
				visibility = View.VISIBLE
				clearColorFilter()
				setPadding(0, 0, 0, 0)
				if (!isFinishing && !isDestroyed) {
					Glide.with(this@WatchActivity).load(Uri.parse(photoUrl.trim())).circleCrop()
						.placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(this)
				}
			}
			initialView?.visibility = View.GONE
		} else {
			avatarView?.visibility = View.GONE
			initialView?.apply {
				visibility = View.VISIBLE
				text = initial
				background = GradientDrawable().apply {
					shape = GradientDrawable.OVAL
					setColor(Color.parseColor("#252934"))
				}
			}
		}
	}

	private fun loadWatchHistoryFromFirebase() {
		val userKey = getCurrentUserKey()
		if (userKey.isEmpty() || currentSlug.isEmpty()) {
			historyLoaded = true
			tryStartEpisodeFromHistoryIfReady()
			return
		}

		FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("users")
			.child(userKey)
			.child("history")
			.child(currentSlug)
			.addListenerForSingleValueEvent(object : ValueEventListener {
				override fun onDataChange(snap: DataSnapshot) {
					if (isFinishing || isDestroyed) return
					if (snap.exists()) {
						pendingHistoryEpisode = snap.child("episode").value?.toString()?.trim() ?: ""
						pendingHistoryEpisodeIndex = parseLongSafe(snap.child("episodeIndex").value, -1L).toInt()
						pendingHistoryPositionMs = parseLongSafe(snap.child("positionMs").value, 0L)
						if (pendingHistoryPositionMs <= 0L) {
							pendingHistoryPositionMs = parseLongSafe(snap.child("currentTime").value, 0L) * 1000L
						}
					}
					historyLoaded = true
					tryStartEpisodeFromHistoryIfReady()
				}

				override fun onCancelled(error: DatabaseError) {
					historyLoaded = true
					tryStartEpisodeFromHistoryIfReady()
				}
			})
	}

	private fun tryStartEpisodeFromHistoryIfReady() {
		if (!historyLoaded || initialEpisodeStarted || isResumingFromMiniPlayer) return
		if (episodeList.isEmpty()) return

		initialEpisodeStarted = true
		var targetIdx = 0

		if (pendingHistoryEpisodeIndex in 0 until episodeList.size) {
			targetIdx = pendingHistoryEpisodeIndex
		} else if (pendingHistoryEpisode.isNotEmpty()) {
			val cleanTarget = pendingHistoryEpisode.replace("(?i)^ep(isode)?\\s*".toRegex(), "").trim()
			for (i in episodeList.indices) {
				val epVal = episodeList[i]["episode"]
					?.toString()
					?.replace("(?i)^ep(isode)?\\s*".toRegex(), "")
					?.trim() ?: ""
				if (cleanTarget.isNotEmpty() && cleanTarget.equals(epVal, ignoreCase = true)) {
					targetIdx = i
					break
				}
			}
		}

		val resumeMs = if (pendingHistoryPositionMs > 0L && pendingHistoryPositionMs < Int.MAX_VALUE) {
			pendingHistoryPositionMs.toInt()
		} else {
			0
		}

		playEpisodeByIndex(targetIdx, resumeMs)
	}

	private fun saveWatchHistoryToFirebase(posMs: Int, durMs: Int, force: Boolean) {
		val now = System.currentTimeMillis()
		if (!force && (now - lastHistorySyncMs) < 5000L) return

		val userKey = getCurrentUserKey()
		if (userKey.isEmpty() || currentSlug.isEmpty()) return

		lastHistorySyncMs = now
		var epLabel = (currentEpisodeIndex + 1).toString()
		if (currentEpisodeIndex in 0 until episodeList.size) {
			val epObj = episodeList[currentEpisodeIndex]["episode"]
			if (epObj != null && epObj.toString().trim().isNotEmpty()) {
				epLabel = epObj.toString().replace("Ep ", "").replace("Eps ", "").trim()
			}
		}

		val safePos = maxOf(0L, posMs.toLong())
		val safeDur = maxOf(0L, durMs.toLong())
		val posMin = safePos / 60000L
		val posSec = (safePos / 1000L) % 60L
		val remainingText = if (safeDur > safePos) {
			String.format(
				Locale.US,
				"Menit %02d:%02d • Sisa %d mnt",
				posMin,
				posSec,
				maxOf(1L, (safeDur - safePos) / 60000L)
			)
		} else {
			String.format(Locale.US, "Terakhir di %02d:%02d", posMin, posSec)
		}

		val titleToSave = when {
			detailTitle.isNotEmpty() -> detailTitle
			!textJudul?.text.isNullOrEmpty() -> textJudul!!.text.toString()
			else -> currentSlug
		}

		val data = hashMapOf<String, Any>(
			"slug" to currentSlug,
			"title" to titleToSave,
			"cover" to detailCover,
			"episode" to epLabel,
			"episodeIndex" to currentEpisodeIndex,
			"positionMs" to safePos,
			"durationMs" to safeDur,
			"remainingText" to remainingText,
			"updatedAt" to now
		)

		FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("users")
			.child(userKey)
			.child("history")
			.child(currentSlug)
			.updateChildren(data)
	}

	private fun parseLongSafe(value: Any?, fallback: Long): Long {
		if (value == null) return fallback
		if (value is Number) return value.toLong()
		return try {
			value.toString().trim().toLong()
		} catch (e: Exception) {
			fallback
		}
	}

	private fun setupCommentSection() {
		val commentBtn = btnCommentSection ?: return
		val d = resources.displayMetrics.density

		commentBtn.background = GradientDrawable().apply {
			shape = GradientDrawable.RECTANGLE
			setColor(Color.parseColor("#161A24"))
			cornerRadius = 1000f
			setStroke((1 * d).toInt(), Color.parseColor("#1FFFFFFF"))
		}

		iconCommentChevron?.setColorFilter(Color.parseColor("#94A3B8"), PorterDuff.Mode.SRC_IN)

		commentBtn.setOnClickListener { openTikTokCommentSheet() }

		attachCommentsListener()
	}

	private fun attachCommentsListener() {
		if (currentSlug.isEmpty()) return
		detachCommentsListener()

		val cleanKey = currentSlug.replace("[.#$\\[\\]/]".toRegex(), "_")
		val ref = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("comments")
			.child(cleanKey)
		commentsRef = ref

		val listener = object : ValueEventListener {
			override fun onDataChange(snap: DataSnapshot) {
				if (isFinishing || isDestroyed) return

				val allItems = ArrayList<HashMap<String, Any>>()
				if (snap.exists()) {
					for (child in snap.children) {
						val value = child.value
						if (value is Map<*, *>) {
							val map = HashMap<String, Any>()
							for ((k, v) in value) {
								if (k != null && v != null) map[k.toString()] = v
							}
							child.key?.let { map["id"] = it }
							allItems.add(map)
						}
					}
				}

				mainCommentList.clear()
				repliesByParentMap.clear()

				for (item in allItems) {
					val id = item["id"]?.toString() ?: ""
					val parentId = item["parentId"]?.toString()?.trim() ?: ""
					if (parentId.isNotEmpty() &&
						!"null".equals(parentId, ignoreCase = true) &&
						!"undefined".equals(parentId, ignoreCase = true) &&
						parentId != id
					) {
						val list = repliesByParentMap.getOrPut(parentId) { ArrayList() }
						list.add(item)
					} else {
						mainCommentList.add(item)
					}
				}

				mainCommentList.sortWith { a, b ->
					val tA = parseLongSafe(a["timestamp"], 0L)
					val tB = parseLongSafe(b["timestamp"], 0L)
					tB.compareTo(tA)
				}

				for (rList in repliesByParentMap.values) {
					rList.sortWith { a, b ->
						val tA = parseLongSafe(a["timestamp"], 0L)
						val tB = parseLongSafe(b["timestamp"], 0L)
						tA.compareTo(tB)
					}
				}

				var total = mainCommentList.size
				for (r in repliesByParentMap.values) total += r.size

				textCommentCount?.text = total.toString()
				activeCommentSheetHeader?.text = "Komentar $total"
				activeCommentSheetAdapter?.notifyDataSetChanged()
			}

			override fun onCancelled(error: DatabaseError) {}
		}

		commentsListener = listener
		ref.addValueEventListener(listener)
	}

	private fun detachCommentsListener() {
		if (commentsRef != null && commentsListener != null) {
			commentsRef?.removeEventListener(commentsListener!!)
		}
	}

	private fun showDeleteCommentConfirmDialog(commentId: String?) {
		if (commentId.isNullOrBlank() || currentSlug.isEmpty()) return
		val d = resources.displayMetrics.density

		val dialog = Dialog(this).apply {
			requestWindowFeature(Window.FEATURE_NO_TITLE)
		}

		val card = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((20 * d).toInt(), (20 * d).toInt(), (20 * d).toInt(), (18 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#12141A"))
				cornerRadius = 20 * d
				setStroke((1 * d).toInt(), Color.parseColor("#24FFFFFF"))
			}
		}

		val tvTitle = TextView(this).apply {
			text = "Hapus komentar?"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}
		card.addView(tvTitle)

		val tvDesc = TextView(this).apply {
			text = "Komentar yang dihapus tidak dapat dikembalikan."
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = (6 * d).toInt()
				bottomMargin = (20 * d).toInt()
			}
		}
		card.addView(tvDesc)

		val btnRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.END
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			)
		}

		val btnCancel = TextView(this).apply {
			text = "Batal"
			setTextColor(Color.parseColor("#CBD5E1"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			gravity = Gravity.CENTER
			setPadding((18 * d).toInt(), (10 * d).toInt(), (18 * d).toInt(), (10 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				rightMargin = (10 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#181C26"))
				cornerRadius = 100f
				setStroke((1 * d).toInt(), Color.parseColor("#1FFFFFFF"))
			}
			setOnClickListener { dialog.dismiss() }
		}

		val btnConfirmDelete = TextView(this).apply {
			text = "Hapus"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			gravity = Gravity.CENTER
			setPadding((20 * d).toInt(), (10 * d).toInt(), (20 * d).toInt(), (10 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#DC2626"))
				cornerRadius = 100f
			}
			setOnClickListener {
				val cleanKey = currentSlug.replace("[.#$\\[\\]/]".toRegex(), "_")
				FirebaseDatabase.getInstance(FIREBASE_DB_URL)
					.getReference("comments")
					.child(cleanKey)
					.child(commentId)
					.removeValue()
				dialog.dismiss()
			}
		}

		btnRow.addView(btnCancel)
		btnRow.addView(btnConfirmDelete)
		card.addView(btnRow)

		dialog.setContentView(card)
		dialog.window?.apply {
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			val screenW = resources.displayMetrics.widthPixels
			val dialogW = if (isLandscapeOrFullscreen()) {
				minOf((360 * d).toInt(), screenW - (48 * d).toInt())
			} else {
				minOf((340 * d).toInt(), screenW - (36 * d).toInt())
			}
			setLayout(dialogW, ViewGroup.LayoutParams.WRAP_CONTENT)
		}
		dialog.show()
	}

	private fun openTikTokCommentSheet() {
		val d = resources.displayMetrics.density
		val screenH = resources.displayMetrics.heightPixels
		val videoBottomHolder = intArrayOf((220 * d).toInt())
		val playerView = layoutPlayer
		if (playerView != null && playerView.height > 0) {
			val loc = IntArray(2)
			playerView.getLocationOnScreen(loc)
			videoBottomHolder[0] = loc[1] + playerView.height
		}
		val baseSheetHeight = maxOf((280 * d).toInt(), screenH - videoBottomHolder[0])

		val sheet = BottomSheetDialog(this)
		sheet.window?.apply {
			clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
			@Suppress("DEPRECATION")
			setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
					WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
			)
		}

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, baseSheetHeight)
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#0B0D11"))
				cornerRadii = floatArrayOf(22 * d, 22 * d, 22 * d, 22 * d, 0f, 0f, 0f, 0f)
			}
		}

		val handle = View(this).apply {
			layoutParams = LinearLayout.LayoutParams((38 * d).toInt(), (4 * d).toInt()).apply {
				gravity = Gravity.CENTER_HORIZONTAL
				topMargin = (10 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#1E222B"))
				cornerRadius = 100f
			}
		}
		root.addView(handle)

		val header = FrameLayout(this).apply {
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (44 * d).toInt())
			setPadding((16 * d).toInt(), 0, (12 * d).toInt(), 0)
		}

		var totalCount = mainCommentList.size
		for (r in repliesByParentMap.values) totalCount += r.size

		val tvHeader = TextView(this).apply {
			text = "Komentar $totalCount"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
			)
		}
		activeCommentSheetHeader = tvHeader

		val btnClose = TextView(this).apply {
			text = "✕"
			setTextColor(Color.parseColor("#CBD5E1"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			gravity = Gravity.CENTER
			layoutParams = FrameLayout.LayoutParams((32 * d).toInt(), (32 * d).toInt(), Gravity.END or Gravity.CENTER_VERTICAL)
			background = GradientDrawable().apply {
				shape = GradientDrawable.OVAL
				setColor(Color.parseColor("#181B22"))
			}
			setOnClickListener { sheet.dismiss() }
		}

		header.addView(tvHeader)
		header.addView(btnClose)
		root.addView(header)

		val rvComments = RecyclerView(this).apply {
			layoutManager = LinearLayoutManager(this@WatchActivity)
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
			setPadding((14 * d).toInt(), (6 * d).toInt(), (14 * d).toInt(), (8 * d).toInt())
			clipToPadding = false
		}

		val selectedStickerUrl = arrayOf("")
		val activeReplyParentId = arrayOf("")

		val ivStickerPreview = ImageView(this).apply {
			layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			scaleType = ImageView.ScaleType.FIT_CENTER
		}
		val stickerPreviewBox = FrameLayout(this).apply {
			visibility = View.GONE
			layoutParams = LinearLayout.LayoutParams((64 * d).toInt(), (64 * d).toInt()).apply {
				leftMargin = (54 * d).toInt()
				bottomMargin = (6 * d).toInt()
			}
		}

		val btnRemoveSticker = TextView(this).apply {
			text = "✕"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			gravity = Gravity.CENTER
			setBackgroundColor(Color.parseColor("#EF4444"))
			layoutParams = FrameLayout.LayoutParams((18 * d).toInt(), (18 * d).toInt(), Gravity.TOP or Gravity.END)
		}
		stickerPreviewBox.addView(ivStickerPreview)
		stickerPreviewBox.addView(btnRemoveSticker)

		val replyBanner = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			visibility = View.GONE
			setPadding((16 * d).toInt(), (5 * d).toInt(), (16 * d).toInt(), (5 * d).toInt())
			setBackgroundColor(Color.parseColor("#12151D"))
		}

		val tvReplyBanner = TextView(this).apply {
			setTextColor(Color.parseColor("#38BDF8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}

		val etInput = EditText(this).apply {
			hint = "Tambahkan komentar..."
			setHintTextColor(Color.parseColor("#8E8E93"))
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			maxLines = 3
			setBackgroundColor(Color.TRANSPARENT)
			setPadding((12 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}

		val btnCancelReply = TextView(this).apply {
			text = "Batal"
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			setOnClickListener {
				activeReplyParentId[0] = ""
				replyBanner.visibility = View.GONE
				etInput.setText("")
			}
		}
		replyBanner.addView(tvReplyBanner)
		replyBanner.addView(btnCancelReply)

		val commentAdapter = TikTokCommentAdapter(
			mainCommentList,
			repliesByParentMap
		) { parentCommentId, mentionTag ->
			activeReplyParentId[0] = parentCommentId
			replyBanner.visibility = View.VISIBLE
			tvReplyBanner.text = "Balas @$mentionTag"

			val prefix = "@$mentionTag "
			etInput.setText(prefix)
			etInput.setSelection(prefix.length)
			etInput.requestFocus()
			val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
			imm?.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT)
		}
		activeCommentSheetAdapter = commentAdapter
		rvComments.adapter = commentAdapter
		root.addView(rvComments)

		val rvMentionList = RecyclerView(this).apply {
			layoutManager = LinearLayoutManager(this@WatchActivity)
			visibility = View.GONE
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, (140 * d).toInt()
			).apply {
				setMargins((14 * d).toInt(), 0, (14 * d).toInt(), (6 * d).toInt())
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#12151E"))
				cornerRadius = 14 * d
				setStroke((1 * d).toInt(), Color.parseColor("#1E222B"))
			}
			setPadding((6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt())
		}

		val filteredMentions = ArrayList<HashMap<String, String>>()
		val mentionAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
			override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
				val row = LinearLayout(parent.context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
					layoutParams = RecyclerView.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
					).apply {
						bottomMargin = (4 * d).toInt()
					}
					setPadding((8 * d).toInt(), (6 * d).toInt(), (10 * d).toInt(), (6 * d).toInt())
					background = GradientDrawable().apply {
						setColor(Color.parseColor("#181C26"))
						cornerRadius = 10 * d
					}
				}

				val av = ImageView(parent.context).apply {
					layoutParams = LinearLayout.LayoutParams((26 * d).toInt(), (26 * d).toInt()).apply {
						rightMargin = (10 * d).toInt()
					}
					scaleType = ImageView.ScaleType.CENTER_CROP
					tag = "m_av"
				}

				val avIni = TextView(parent.context).apply {
					layoutParams = LinearLayout.LayoutParams((26 * d).toInt(), (26 * d).toInt()).apply {
						rightMargin = (10 * d).toInt()
					}
					gravity = Gravity.CENTER
					setTextColor(Color.WHITE)
					setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
					setTypeface(getOutfitTypeface(), Typeface.NORMAL)
					tag = "m_ini"
				}

				val txtCol = LinearLayout(parent.context).apply {
					orientation = LinearLayout.VERTICAL
					layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
				}

				val tvN = TextView(parent.context).apply {
					setTextColor(Color.WHITE)
					setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
					setTypeface(getOutfitTypeface(), Typeface.NORMAL)
					tag = "m_name"
				}

				val tvU = TextView(parent.context).apply {
					setTextColor(Color.parseColor("#38BDF8"))
					setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
					setTypeface(getOutfitTypeface(), Typeface.NORMAL)
					tag = "m_user"
				}

				txtCol.addView(tvN)
				txtCol.addView(tvU)
				row.addView(av)
				row.addView(avIni)
				row.addView(txtCol)
				return object : RecyclerView.ViewHolder(row) {}
			}

			override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
				val u = filteredMentions[pos]
				val name = u["name"] ?: "User"
				val rawU = u["username"] ?: ""
				val tag = if (rawU.isNotEmpty()) rawU else name
				val photo = u["photoURL"]

				val av = holder.itemView.findViewWithTag<ImageView>("m_av")
				val avIni = holder.itemView.findViewWithTag<TextView>("m_ini")
				val tvN = holder.itemView.findViewWithTag<TextView>("m_name")
				val tvU = holder.itemView.findViewWithTag<TextView>("m_user")

				bindAvatarOrInitial(av, avIni, photo, name)
				tvN?.text = name
				tvU?.text = "@$tag"

				holder.itemView.setOnClickListener {
					val cur = etInput.text.toString()
					val cursorPos = minOf(cur.length, maxOf(0, etInput.selectionStart))
					val atPos = cur.substring(0, cursorPos).lastIndexOf('@')
					if (atPos >= 0) {
						val next = "${cur.substring(0, atPos)}@$tag ${cur.substring(cursorPos)}"
						etInput.setText(next)
						etInput.setSelection(minOf(next.length, atPos + tag.length + 2))
					}
					rvMentionList.visibility = View.GONE
				}
			}

			override fun getItemCount(): Int = filteredMentions.size
		}
		rvMentionList.adapter = mentionAdapter

		root.addView(rvMentionList)
		root.addView(replyBanner)
		root.addView(stickerPreviewBox)

		val inputDivider = View(this).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, maxOf(1, (1 * d).toInt())
			)
			setBackgroundColor(Color.parseColor("#1E222B"))
		}
		root.addView(inputDivider)

		val bottomBar = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding((14 * d).toInt(), (8 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
			setBackgroundColor(Color.parseColor("#0B0D11"))
		}

		val curUser = FirebaseAuth.getInstance().currentUser
		val myProf = if (curUser != null) {
			resolveUserProfile(curUser.email, curUser.uid)
		} else {
			hashMapOf()
		}
		val myName = myProf["name"] ?: "U"
		val myPhoto = myProf["photoURL"] ?: ""

		val ivMyAvatar = ImageView(this).apply {
			layoutParams = LinearLayout.LayoutParams((32 * d).toInt(), (32 * d).toInt()).apply {
				rightMargin = (10 * d).toInt()
			}
			scaleType = ImageView.ScaleType.CENTER_CROP
		}

		val tvMyInitial = TextView(this).apply {
			layoutParams = LinearLayout.LayoutParams((32 * d).toInt(), (32 * d).toInt()).apply {
				rightMargin = (10 * d).toInt()
			}
			gravity = Gravity.CENTER
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}

		bindAvatarOrInitial(ivMyAvatar, tvMyInitial, myPhoto, myName)
		bottomBar.addView(ivMyAvatar)
		bottomBar.addView(tvMyInitial)

		val inputPill = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding((4 * d).toInt(), (2 * d).toInt(), (6 * d).toInt(), (2 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#181B22"))
				cornerRadius = 1000f
			}
		}

		val btnSticker = ImageView(this).apply {
			layoutParams = LinearLayout.LayoutParams((32 * d).toInt(), (32 * d).toInt())
			setPadding((6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt())
			setImageDrawable(
				resolveVectorDrawable(
					"ic_sticker",
					createStickerSvgDrawable(Color.parseColor("#CBD5E1"), maxOf(1.6f, 1.5f * d))
				)
			)
			setColorFilter(Color.parseColor("#CBD5E1"), PorterDuff.Mode.SRC_IN)
		}

		val btnSendIcon = ImageView(this).apply {
			layoutParams = LinearLayout.LayoutParams((36 * d).toInt(), (36 * d).toInt()).apply {
				leftMargin = (8 * d).toInt()
			}
			setPadding((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt())
			setImageDrawable(
				resolveVectorDrawable(
					"ic_send",
					createSendSvgDrawable(Color.WHITE, maxOf(1.8f, 1.7f * d))
				)
			)
			setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
			background = GradientDrawable().apply {
				shape = GradientDrawable.OVAL
				setColor(Color.parseColor("#2563EB"))
			}
			visibility = View.GONE
		}

		val refreshSendVisibility = Runnable {
			val hasText = etInput.text.toString().trim().isNotEmpty()
			btnSendIcon.visibility = if (hasText) View.VISIBLE else View.GONE
		}

		btnSticker.setOnClickListener {
			openStickerPickerDialog { url ->
				selectedStickerUrl[0] = url
				ivStickerPreview.setImageURI(null)
				Glide.with(this@WatchActivity).load(Uri.parse(url)).into(ivStickerPreview)
				stickerPreviewBox.visibility = View.VISIBLE
			}
		}

		btnRemoveSticker.setOnClickListener {
			selectedStickerUrl[0] = ""
			stickerPreviewBox.visibility = View.GONE
		}

		etInput.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				refreshSendVisibility.run()

				val full = s?.toString() ?: ""
				val cursor = minOf(full.length, maxOf(0, etInput.selectionStart))
				val beforeCursor = full.substring(0, cursor)
				val atIdx = beforeCursor.lastIndexOf('@')
				if (atIdx >= 0 && (atIdx == 0 || Character.isWhitespace(beforeCursor[atIdx - 1]))) {
					val q = beforeCursor.substring(atIdx + 1).lowercase(Locale.US)
					filteredMentions.clear()
					for (u in allUsersDirectory) {
						val n = u["name"]?.lowercase(Locale.US) ?: ""
						val un = u["username"]?.lowercase(Locale.US) ?: ""
						if (q.isEmpty() || n.contains(q) || un.contains(q)) {
							filteredMentions.add(u)
							if (filteredMentions.size >= 8) break
						}
					}
					mentionAdapter.notifyDataSetChanged()
					rvMentionList.visibility = if (filteredMentions.isEmpty()) View.GONE else View.VISIBLE
				} else {
					rvMentionList.visibility = View.GONE
				}
			}

			override fun afterTextChanged(s: Editable?) {}
		})

		inputPill.addView(etInput)
		inputPill.addView(btnSticker)

		btnSendIcon.setOnClickListener {
			val text = etInput.text.toString().trim()
			val stUrl = selectedStickerUrl[0]
			val pId = activeReplyParentId[0]

			if ((text.isEmpty() && stUrl.isEmpty()) || currentSlug.isEmpty()) return@setOnClickListener

			val u = FirebaseAuth.getInstance().currentUser
			if (u == null) {
				Toast.makeText(this@WatchActivity, "Silakan login untuk berkomentar", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}

			val uKey = sanitizeUserKey(u.email, u.uid)
			val p = resolveUserProfile(u.email, u.uid)
			val isAdm = u.email != null && ADMIN_EMAIL.equals(u.email?.trim(), ignoreCase = true)

			val payload = HashMap<String, Any>().apply {
				put("uid", u.uid)
				put("email", u.email ?: "")
				put("text", text)
				put("stickerUrl", stUrl)
				put("timestamp", System.currentTimeMillis())
				put("isAdmin", isAdm)
				if (pId.isNotEmpty()) {
					put("parentId", pId)
				}
			}

			val cleanSlugKey = currentSlug.replace("[.#$\\[\\]/]".toRegex(), "_")
			val entryId = System.currentTimeMillis().toString()
			FirebaseDatabase.getInstance(FIREBASE_DB_URL)
				.getReference("comments")
				.child(cleanSlugKey)
				.child(entryId)
				.setValue(payload)

			addExpToCurrentUser(500L)

			etInput.setText("")
			selectedStickerUrl[0] = ""
			activeReplyParentId[0] = ""
			stickerPreviewBox.visibility = View.GONE
			replyBanner.visibility = View.GONE
			rvMentionList.visibility = View.GONE
			refreshSendVisibility.run()

			if (uKey.isNotEmpty() && !userProfileMap.containsKey(uKey)) {
				userProfileMap[uKey] = p
			}

			val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
			imm?.hideSoftInputFromWindow(etInput.windowToken, 0)
		}

		bottomBar.addView(inputPill)
		bottomBar.addView(btnSendIcon)
		root.addView(bottomBar)

		sheet.setContentView(root)

		val behaviorRef = arrayOfNulls<BottomSheetBehavior<FrameLayout>>(1)
		rvComments.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				behaviorRef[0]?.let { b ->
					val atTop = !recyclerView.canScrollVertically(-1)
					b.isDraggable = atTop
				}
			}
		})

		val dialogDecor = sheet.window?.decorView
		val keyboardLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
			val decor = dialogDecor ?: return@OnGlobalLayoutListener
			val r = Rect()
			decor.getWindowVisibleDisplayFrame(r)
			val fullH = decor.rootView.height
			val keypadHeight = maxOf(0, fullH - r.bottom)

			val availableAboveKeyboard = maxOf((180 * d).toInt(), r.bottom - videoBottomHolder[0])
			val targetH = if (keypadHeight > (100 * d).toInt()) {
				minOf(baseSheetHeight, availableAboveKeyboard)
			} else {
				baseSheetHeight
			}

			val lp = root.layoutParams
			if (lp != null && lp.height != targetH) {
				lp.height = targetH
				root.layoutParams = lp
			}
			bottomBar.setPadding(
				(14 * d).toInt(),
				(8 * d).toInt(),
				(14 * d).toInt(),
				if (keypadHeight > (100 * d).toInt()) (10 * d).toInt() else (12 * d).toInt()
			)

			val bottomSheet = sheet.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
			if (bottomSheet != null) {
				val bLp = bottomSheet.layoutParams
				if (bLp != null && bLp.height != targetH) {
					bLp.height = targetH
					bottomSheet.layoutParams = bLp
				}
				val behavior = BottomSheetBehavior.from(bottomSheet)
				behaviorRef[0] = behavior
				behavior.peekHeight = targetH
				behavior.state = BottomSheetBehavior.STATE_EXPANDED
			}
		}

		dialogDecor?.viewTreeObserver?.addOnGlobalLayoutListener(keyboardLayoutListener)

		sheet.setOnShowListener {
			val bottomSheet = sheet.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
			if (bottomSheet != null) {
				bottomSheet.setBackgroundColor(Color.TRANSPARENT)
				bottomSheet.backgroundTintList = null
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					bottomSheet.elevation = 0f
				}
				val lp = bottomSheet.layoutParams
				lp.height = baseSheetHeight
				bottomSheet.layoutParams = lp
				val behavior = BottomSheetBehavior.from(bottomSheet)
				behaviorRef[0] = behavior
				behavior.peekHeight = baseSheetHeight
				behavior.state = BottomSheetBehavior.STATE_EXPANDED
				behavior.skipCollapsed = true
				behavior.isDraggable = !rvComments.canScrollVertically(-1)
			}
		}
		sheet.setOnDismissListener {
			dialogDecor?.viewTreeObserver?.removeOnGlobalLayoutListener(keyboardLayoutListener)
			activeCommentSheetAdapter = null
			activeCommentSheetHeader = null
		}
		sheet.show()
	}

	private fun openStickerPickerDialog(listener: OnStickerPickedListener) {
		val d = resources.displayMetrics.density
		val picker = BottomSheetDialog(this)

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((18 * d).toInt(), (12 * d).toInt(), (18 * d).toInt(), (22 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#0B0D11"))
				cornerRadii = floatArrayOf(22 * d, 22 * d, 22 * d, 22 * d, 0f, 0f, 0f, 0f)
				setStroke((1 * d).toInt(), Color.parseColor("#1E222B"))
			}
		}

		val handle = View(this).apply {
			layoutParams = LinearLayout.LayoutParams((38 * d).toInt(), (4 * d).toInt()).apply {
				gravity = Gravity.CENTER_HORIZONTAL
				bottomMargin = (12 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#1E222B"))
				cornerRadius = 100f
			}
		}
		root.addView(handle)

		val pHeader = FrameLayout(this).apply {
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (36 * d).toInt()).apply {
				bottomMargin = (10 * d).toInt()
			}
		}

		val title = TextView(this).apply {
			text = "Pilih Stiker"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL
			)
		}

		val btnClose = TextView(this).apply {
			text = "✕"
			setTextColor(Color.parseColor("#CBD5E1"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			gravity = Gravity.CENTER
			layoutParams = FrameLayout.LayoutParams((30 * d).toInt(), (30 * d).toInt(), Gravity.END or Gravity.CENTER_VERTICAL)
			background = GradientDrawable().apply {
				shape = GradientDrawable.OVAL
				setColor(Color.parseColor("#181B22"))
			}
			setOnClickListener { picker.dismiss() }
		}

		pHeader.addView(title)
		pHeader.addView(btnClose)
		root.addView(pHeader)

		val rvStickers = RecyclerView(this).apply {
			layoutManager = GridLayoutManager(this@WatchActivity, 4)
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, (240 * d).toInt()
			)
			adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
				override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
					val cell = FrameLayout(parent.context).apply {
						layoutParams = RecyclerView.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT, (76 * d).toInt()
						).apply {
							setMargins((5 * d).toInt(), (5 * d).toInt(), (5 * d).toInt(), (5 * d).toInt())
						}
						background = GradientDrawable().apply {
							setColor(Color.parseColor("#12141A"))
							cornerRadius = 14 * d
							setStroke((1 * d).toInt(), Color.parseColor("#1E222B"))
						}
					}

					val iv = ImageView(parent.context).apply {
						layoutParams = FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
						)
						setPadding((10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt())
						scaleType = ImageView.ScaleType.FIT_CENTER
						tag = "sticker_img"
					}
					cell.addView(iv)
					return object : RecyclerView.ViewHolder(cell) {}
				}

				override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
					val url = stickerList[pos]
					val iv = holder.itemView.findViewWithTag<ImageView>("sticker_img")
					if (iv != null && !isFinishing && !isDestroyed) {
						Glide.with(this@WatchActivity).load(Uri.parse(url)).into(iv)
					}
					holder.itemView.setOnClickListener {
						picker.dismiss()
						listener.onStickerPicked(url)
					}
				}

				override fun getItemCount(): Int = stickerList.size
			}
		}

		root.addView(rvStickers)
		picker.setContentView(root)
		picker.setOnShowListener {
			val bottomSheet = picker.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
			if (bottomSheet != null) {
				bottomSheet.setBackgroundColor(Color.TRANSPARENT)
				bottomSheet.backgroundTintList = null
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					bottomSheet.elevation = 0f
				}
			}
		}
		picker.show()
	}

	private inner class TikTokCommentAdapter(
		private val mainItems: List<HashMap<String, Any>>,
		private val repliesMap: Map<String, List<HashMap<String, Any>>>,
		private val replyListener: OnReplyClickListener
	) : RecyclerView.Adapter<TikTokCommentAdapter.ViewHolder>() {

		private val expandedParents = HashSet<String>()

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val d = parent.resources.displayMetrics.density

			val card = LinearLayout(parent.context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.TOP
				layoutParams = RecyclerView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					bottomMargin = (14 * d).toInt()
				}
				setPadding((4 * d).toInt(), (4 * d).toInt(), (4 * d).toInt(), (4 * d).toInt())
			}

			val avatar = ImageView(parent.context).apply {
				layoutParams = LinearLayout.LayoutParams((34 * d).toInt(), (34 * d).toInt()).apply {
					rightMargin = (10 * d).toInt()
				}
				scaleType = ImageView.ScaleType.CENTER_CROP
				tag = "avatar"
			}

			val tvInitial = TextView(parent.context).apply {
				layoutParams = LinearLayout.LayoutParams((34 * d).toInt(), (34 * d).toInt()).apply {
					rightMargin = (10 * d).toInt()
				}
				gravity = Gravity.CENTER
				setTextColor(Color.WHITE)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				tag = "initial"
			}

			val col = LinearLayout(parent.context).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
			}

			val nameLine = LinearLayout(parent.context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
			}

			val tvName = TextView(parent.context).apply {
				setTextColor(Color.parseColor("#CBD5E1"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				tag = "name"
			}

			val tvAdminBadge = TextView(parent.context).apply {
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				setPadding((7 * d).toInt(), (2 * d).toInt(), (7 * d).toInt(), (2 * d).toInt())
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					leftMargin = (6 * d).toInt()
				}
				tag = "admin_badge"
			}

			val tvLevelBadge = TextView(parent.context).apply {
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				setPadding((7 * d).toInt(), (2 * d).toInt(), (7 * d).toInt(), (2 * d).toInt())
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					leftMargin = (5 * d).toInt()
				}
				tag = "level_badge"
			}

			nameLine.addView(tvName)
			nameLine.addView(tvAdminBadge)
			nameLine.addView(tvLevelBadge)

			val tvBody = TextView(parent.context).apply {
				setTextColor(Color.WHITE)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				setLineSpacing(2 * d, 1.08f)
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					topMargin = (3 * d).toInt()
				}
				tag = "body"
			}

			val ivSticker = ImageView(parent.context).apply {
				layoutParams = LinearLayout.LayoutParams((110 * d).toInt(), (110 * d).toInt()).apply {
					topMargin = (6 * d).toInt()
				}
				scaleType = ImageView.ScaleType.FIT_CENTER
				tag = "sticker"
			}

			val footerRow = LinearLayout(parent.context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					topMargin = (5 * d).toInt()
				}
			}

			val tvTime = TextView(parent.context).apply {
				setTextColor(Color.parseColor("#94A3B8"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				tag = "time"
			}

			val btnReply = TextView(parent.context).apply {
				text = "Balas"
				setTextColor(Color.parseColor("#CBD5E1"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					leftMargin = (14 * d).toInt()
				}
				tag = "btn_reply"
			}

			val btnDelete = TextView(parent.context).apply {
				text = "Hapus"
				setTextColor(Color.parseColor("#EF4444"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					leftMargin = (14 * d).toInt()
				}
				tag = "btn_delete"
			}

			footerRow.addView(tvTime)
			footerRow.addView(btnReply)
			footerRow.addView(btnDelete)

			val toggleRow = LinearLayout(parent.context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					topMargin = (8 * d).toInt()
				}
				tag = "toggle_box"
			}

			val dash = View(parent.context).apply {
				layoutParams = LinearLayout.LayoutParams((20 * d).toInt(), maxOf(1, (1 * d).toInt())).apply {
					rightMargin = (8 * d).toInt()
				}
				setBackgroundColor(Color.parseColor("#475569"))
			}

			val btnToggleReplies = TextView(parent.context).apply {
				setTextColor(Color.parseColor("#94A3B8"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				tag = "toggle_replies"
			}

			val ivToggleChevron = ImageView(parent.context).apply {
				layoutParams = LinearLayout.LayoutParams((13 * d).toInt(), (13 * d).toInt()).apply {
					leftMargin = (4 * d).toInt()
				}
				setImageResource(R.drawable.ic_chevron_down)
				setColorFilter(Color.parseColor("#94A3B8"), PorterDuff.Mode.SRC_IN)
				tag = "toggle_chevron"
			}

			toggleRow.addView(dash)
			toggleRow.addView(btnToggleReplies)
			toggleRow.addView(ivToggleChevron)

			val repliesWrapper = LinearLayout(parent.context).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					topMargin = (8 * d).toInt()
					leftMargin = (2 * d).toInt()
				}
				tag = "replies_wrapper"
			}

			val verticalLine = View(parent.context).apply {
				layoutParams = LinearLayout.LayoutParams(
					maxOf(2, (1.8f * d).toInt()), ViewGroup.LayoutParams.MATCH_PARENT
				).apply {
					rightMargin = (10 * d).toInt()
				}
				setBackgroundColor(Color.parseColor("#27272A"))
			}

			val repliesContainer = LinearLayout(parent.context).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
				tag = "replies_container"
			}

			repliesWrapper.addView(verticalLine)
			repliesWrapper.addView(repliesContainer)

			col.addView(nameLine)
			col.addView(tvBody)
			col.addView(ivSticker)
			col.addView(footerRow)
			col.addView(toggleRow)
			col.addView(repliesWrapper)

			card.addView(avatar)
			card.addView(tvInitial)
			card.addView(col)
			return ViewHolder(card)
		}

		override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
			val item = mainItems[pos]
			val commentId = item["id"]?.toString() ?: ""
			val email = item["email"]?.toString() ?: ""
			val uid = item["uid"]?.toString() ?: ""
			val text = item["text"]?.toString() ?: ""
			val stickerUrl = item["stickerUrl"]?.toString()?.trim() ?: ""
			val ts = parseLongSafe(item["timestamp"], System.currentTimeMillis())

			val prof = resolveUserProfile(email, uid)
			val displayName = prof["name"] ?: "User"
			val rawUser = prof["username"] ?: ""
			val mentionHandle = if (rawUser.isNotEmpty()) rawUser else displayName
			val photoUrl = prof["photoURL"]
			val exp = parseLongSafe(prof["exp"], 0L)
			val isAdmin = email.isNotEmpty() && ADMIN_EMAIL.equals(email.trim(), ignoreCase = true)
			val level = calculateLevel(exp)

			val d = holder.itemView.resources.displayMetrics.density
			val avatar = holder.itemView.findViewWithTag<ImageView>("avatar")
			val tvInitial = holder.itemView.findViewWithTag<TextView>("initial")
			val tvName = holder.itemView.findViewWithTag<TextView>("name")
			val tvAdminBadge = holder.itemView.findViewWithTag<TextView>("admin_badge")
			val tvLevelBadge = holder.itemView.findViewWithTag<TextView>("level_badge")
			val tvBody = holder.itemView.findViewWithTag<TextView>("body")
			val ivSticker = holder.itemView.findViewWithTag<ImageView>("sticker")
			val tvTime = holder.itemView.findViewWithTag<TextView>("time")
			val btnReply = holder.itemView.findViewWithTag<TextView>("btn_reply")
			val btnDelete = holder.itemView.findViewWithTag<TextView>("btn_delete")
			val toggleBox = holder.itemView.findViewWithTag<View>("toggle_box")
			val btnToggleReplies = holder.itemView.findViewWithTag<TextView>("toggle_replies")
			val ivToggleChevron = holder.itemView.findViewWithTag<ImageView>("toggle_chevron")
			val repliesWrapper = holder.itemView.findViewWithTag<View>("replies_wrapper")
			val repliesContainer = holder.itemView.findViewWithTag<LinearLayout>("replies_container")

			bindAvatarOrInitial(avatar, tvInitial, photoUrl, displayName)

			tvName?.text = displayName
			if (isAdmin) {
				tvAdminBadge?.apply {
					visibility = View.VISIBLE
					background = GradientDrawable().apply {
						cornerRadius = 100f
						setColor(Color.parseColor("#451A03"))
						setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#F59E0B"))
					}
					setTextColor(Color.parseColor("#FBBF24"))
					text = "★ ADMIN"
				}
			} else {
				tvAdminBadge?.visibility = View.GONE
			}

			tvLevelBadge?.apply {
				background = GradientDrawable().apply {
					cornerRadius = 100f
					setColor(Color.parseColor("#0C2438"))
					setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#38BDF8"))
				}
				setTextColor(Color.parseColor("#38BDF8"))
				text = if (isAdmin && exp == 0L) "Lv999+" else "Lv$level"
			}

			tvBody?.apply {
				visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
				this.text = text
			}

			if (ivSticker != null) {
				if (stickerUrl.isNotEmpty() && !isFinishing && !isDestroyed) {
					ivSticker.visibility = View.VISIBLE
					Glide.with(this@WatchActivity).load(Uri.parse(stickerUrl)).into(ivSticker)
				} else {
					ivSticker.visibility = View.GONE
				}
			}

			tvTime?.text = formatRelativeOrDateTime(ts)

			val curUser = FirebaseAuth.getInstance().currentUser
			btnReply?.apply {
				visibility = if (curUser != null) View.VISIBLE else View.GONE
				setOnClickListener { replyListener.onReplyRequested(commentId, mentionHandle) }
			}

			if (btnDelete != null) {
				var canDelete = false
				if (curUser != null) {
					val curEmail = curUser.email?.trim() ?: ""
					canDelete = ADMIN_EMAIL.equals(curEmail, ignoreCase = true) ||
						(email.isNotEmpty() && curEmail.equals(email.trim(), ignoreCase = true)) ||
						(uid.isNotEmpty() && uid == curUser.uid)
				}
				btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
				btnDelete.setOnClickListener { showDeleteCommentConfirmDialog(commentId) }
			}

			val replies = repliesMap[commentId]
			val isExpanded = expandedParents.contains(commentId)
			if (toggleBox != null && btnToggleReplies != null && repliesWrapper != null && repliesContainer != null) {
				if (!replies.isNullOrEmpty()) {
					toggleBox.visibility = View.VISIBLE
					btnToggleReplies.text = if (isExpanded) {
						"Sembunyikan balasan"
					} else {
						"Lihat ${replies.size} balasan"
					}
					ivToggleChevron?.rotation = if (isExpanded) 180f else 0f
					repliesWrapper.visibility = if (isExpanded) View.VISIBLE else View.GONE
					repliesContainer.removeAllViews()

					if (isExpanded) {
						for (rItem in replies) {
							repliesContainer.addView(buildReplyRowView(repliesContainer.context, rItem, d))
						}
					}

					toggleBox.setOnClickListener {
						if (expandedParents.contains(commentId)) {
							expandedParents.remove(commentId)
						} else {
							expandedParents.add(commentId)
						}
						val adapterPos = holder.bindingAdapterPosition
						if (adapterPos != RecyclerView.NO_POSITION) {
							notifyItemChanged(adapterPos)
						}
					}
				} else {
					toggleBox.visibility = View.GONE
					repliesWrapper.visibility = View.GONE
					repliesContainer.removeAllViews()
				}
			}
		}

		private fun buildReplyRowView(ctx: Context, rItem: HashMap<String, Any>, d: Float): View {
			val rEmail = rItem["email"]?.toString() ?: ""
			val rUid = rItem["uid"]?.toString() ?: ""
			val rText = rItem["text"]?.toString() ?: ""
			val rSticker = rItem["stickerUrl"]?.toString()?.trim() ?: ""
			val rTs = parseLongSafe(rItem["timestamp"], System.currentTimeMillis())

			val rProf = resolveUserProfile(rEmail, rUid)
			val rName = rProf["name"] ?: "User"
			val rPhoto = rProf["photoURL"]
			val rExp = parseLongSafe(rProf["exp"], 0L)
			val rAdmin = rEmail.isNotEmpty() && ADMIN_EMAIL.equals(rEmail.trim(), ignoreCase = true)
			val rLevel = calculateLevel(rExp)

			val row = LinearLayout(ctx).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.TOP
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					bottomMargin = (10 * d).toInt()
				}
				setPadding((2 * d).toInt(), (2 * d).toInt(), (2 * d).toInt(), (2 * d).toInt())
			}

			val av = ImageView(ctx).apply {
				layoutParams = LinearLayout.LayoutParams((24 * d).toInt(), (24 * d).toInt()).apply {
					rightMargin = (8 * d).toInt()
				}
				scaleType = ImageView.ScaleType.CENTER_CROP
			}

			val avIni = TextView(ctx).apply {
				layoutParams = LinearLayout.LayoutParams((24 * d).toInt(), (24 * d).toInt()).apply {
					rightMargin = (8 * d).toInt()
				}
				gravity = Gravity.CENTER
				setTextColor(Color.WHITE)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			}

			bindAvatarOrInitial(av, avIni, rPhoto, rName)

			val col = LinearLayout(ctx).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
			}

			val nRow = LinearLayout(ctx).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
			}

			val tvN = TextView(ctx).apply {
				text = rName
				setTextColor(Color.parseColor("#CBD5E1"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			}
			nRow.addView(tvN)

			if (rAdmin) {
				val tvAdm = TextView(ctx).apply {
					text = "★ ADMIN"
					setTextColor(Color.parseColor("#FBBF24"))
					setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
					setTypeface(getOutfitTypeface(), Typeface.NORMAL)
					setPadding((6 * d).toInt(), (1 * d).toInt(), (6 * d).toInt(), (1 * d).toInt())
					layoutParams = LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
					).apply {
						leftMargin = (6 * d).toInt()
					}
					background = GradientDrawable().apply {
						cornerRadius = 100f
						setColor(Color.parseColor("#451A03"))
						setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#F59E0B"))
					}
				}
				nRow.addView(tvAdm)
			}

			val tvLvl = TextView(ctx).apply {
				text = if (rAdmin && rExp == 0L) "Lv999+" else "Lv$rLevel"
				setTextColor(Color.parseColor("#38BDF8"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				setPadding((6 * d).toInt(), (1 * d).toInt(), (6 * d).toInt(), (1 * d).toInt())
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					leftMargin = (5 * d).toInt()
				}
				background = GradientDrawable().apply {
					cornerRadius = 100f
					setColor(Color.parseColor("#0C2438"))
					setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#38BDF8"))
				}
			}
			nRow.addView(tvLvl)

			val tvB = TextView(ctx).apply {
				text = rText
				setTextColor(Color.WHITE)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				visibility = if (rText.isEmpty()) View.GONE else View.VISIBLE
			}

			val ivS = ImageView(ctx).apply {
				layoutParams = LinearLayout.LayoutParams((92 * d).toInt(), (92 * d).toInt()).apply {
					topMargin = (4 * d).toInt()
				}
				scaleType = ImageView.ScaleType.FIT_CENTER
				if (rSticker.isNotEmpty() && !isFinishing && !isDestroyed) {
					visibility = View.VISIBLE
					Glide.with(this@WatchActivity).load(Uri.parse(rSticker)).into(this)
				} else {
					visibility = View.GONE
				}
			}

			val tvT = TextView(ctx).apply {
				text = formatRelativeOrDateTime(rTs)
				setTextColor(Color.parseColor("#94A3B8"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			}

			col.addView(nRow)
			col.addView(tvB)
			col.addView(ivS)
			col.addView(tvT)

			row.addView(av)
			row.addView(avIni)
			row.addView(col)
			return row
		}

		override fun getItemCount(): Int = mainItems.size

		inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
	}

	private fun initialize(_savedInstanceState: Bundle?) {
		reqWatch = RequestNetwork(this)

		playerTextureView = findViewById(R.id.playerTextureView)
		layoutPlayer = findViewById(R.id.layoutPlayer)
		playerControlsOverlay = findViewById(R.id.playerControlsOverlay)
		gradientTop = findViewById(R.id.gradientTop)
		gradientBottom = findViewById(R.id.gradientBottom)
		topBar = findViewById(R.id.topBar)
		bottomControls = findViewById(R.id.bottomControls)
		dividerLineView = findViewById(R.id.dividerContainer)
		miniVideoProgressBar = findViewById(R.id.miniVideoProgressBar)

		animSeekLeft = findViewById(R.id.animSeekLeft)
		animSeekRight = findViewById(R.id.animSeekRight)
		btnUnlockScreen = findViewById(R.id.btnUnlockScreen)
		pillSpeedHold = findViewById(R.id.pillSpeedHold)

		btnBack = findViewById(R.id.btnBack)
		btnCast = findViewById(R.id.btnCast)
		btnLock = findViewById(R.id.btnLock)
		btnSettings = findViewById(R.id.btnSettings)
		btnPrevEpisode = findViewById(R.id.btnPrevEpisode)
		btnRewind = findViewById(R.id.btnRewind)
		btnPlay = findViewById(R.id.btnPlay)
		btnForward = findViewById(R.id.btnForward)
		btnNextEpisode = findViewById(R.id.btnNextEpisode)
		btnFullscreen = findViewById(R.id.btnFullscreen)
		imagePoster = findViewById(R.id.imagePoster)
		iconExpandDetail = findViewById(R.id.iconExpandDetail)

		textPlayerTitle = findViewById(R.id.textPlayerTitle)
		textTime = findViewById(R.id.textTime)
		btnFullscreenEpisodes = findViewById(R.id.btnFullscreenEpisodes)
		btnQuality = findViewById(R.id.btnQuality)
		textJudul = findViewById(R.id.textJudul)
		textRating = findViewById(R.id.textRating)
		textYear = findViewById(R.id.textYear)
		textGenre = findViewById(R.id.textGenre)
		textActionServer = findViewById(R.id.textActionServer)
		textSortEpisodes = findViewById(R.id.textSortEpisodes)

		btnCommentSection = findViewById(R.id.btnCommentSection)
		textCommentPreviewLabel = findViewById(R.id.textCommentPreviewLabel)
		textCommentCount = findViewById(R.id.textCommentCount)
		iconCommentChevron = findViewById(R.id.iconCommentChevron)

		btnFill = findViewById(R.id.btnFill)
		btnOpenDetail = findViewById(R.id.btnOpenDetail)
		btnSortEpisodes = findViewById(R.id.btnSortEpisodes)
		btnActionServer = findViewById(R.id.btnActionServer)
		btnActionDownload = findViewById(R.id.btnActionDownload)
		btnActionShare = findViewById(R.id.btnActionShare)
		btnActionReport = findViewById(R.id.btnActionReport)

		playerLoading = findViewById(R.id.playerLoading)
		playerSeekBar = findViewById(R.id.playerSeekBar)
		recyclerEpisode = findViewById(R.id.recyclerEpisode)
		recyclerRekomendasi = findViewById(R.id.recyclerRekomendasi)

		reqWatchRequestListener = object : RequestNetwork.RequestListener {
			override fun onResponse(_param1: String, _param2: String, _param3: HashMap<String, Any>) {
				if (_param1 == "slug") {
					DongflixPlayerManager.getInstance().cachedEpisodesJson = _param2
					handleEpisodesJson(_param2, false)
				} else {
					DongflixPlayerManager.getInstance().cachedAllCatalogJson = _param2
					handleCatalogJson(_param2)
				}
			}

			override fun onErrorResponse(_param1: String, _param2: String) {
				if (_param1 == "slug") {
					isEpisodesLoading = false
				} else {
					isDetailLoading = false
					isRekomLoading = false
					clearDetailShimmer()
				}
				checkStopShimmerIfDone()
			}
		}
	}

	private fun handleEpisodesJson(_response: String, fromCache: Boolean) {
		isEpisodesLoading = false
		val type = object : TypeToken<ArrayList<HashMap<String, Any>>>() {}.type
		val parsed: ArrayList<HashMap<String, Any>>? = Gson().fromJson(_response, type)
		if (parsed != null && parsed.isNotEmpty()) {
			eps = parsed
			Collections.reverse(eps)
			isNewestFirst = true
			textSortEpisodes?.text = "Terbaru"

			recyclerEpisode?.adapter = RecyclerEpisodeAdapter(eps)
			episodeList = eps

			if (fromCache || (isResumingFromMiniPlayer && mediaPlayer != null)) {
				val mgr = DongflixPlayerManager.getInstance()
				currentEpisodeIndex = mgr.activeEpisodeIndex
				recyclerEpisode?.let { rv ->
					rv.scrollToPosition(currentEpisodeIndex)
					rv.adapter?.notifyDataSetChanged()
				}
				updateEpisodeNavButtons()
			} else {
				tryStartEpisodeFromHistoryIfReady()
			}
		}
		checkStopShimmerIfDone()
	}

	private fun handleCatalogJson(_response: String) {
		isDetailLoading = false
		isRekomLoading = false
		clearDetailShimmer()

		val targetSlug = currentSlug
		if (targetSlug.isNotEmpty()) {
			try {
				val jsonArray = JSONArray(_response)
				for (i in 0 until jsonArray.length()) {
					val obj = jsonArray.getJSONObject(i)
					if (targetSlug == obj.optString("slug")) {
						detailTitle = obj.optString("title")
						detailSynopsis = obj.optString("synopsis")
							.replace("\\[[^\\x20-\\x7E]+\\]".toRegex(), "")
							.replace("[^\\x20-\\x7E\\n]".toRegex(), "")
							.trim()
						detailStatus = obj.optString("status", "Ongoing")
						if (detailStatus.isEmpty()) detailStatus = "Ongoing"

						detailCover = obj.optString("cover").trim()
						if (detailCover.contains("i.ibb.co.com")) {
							detailCover = detailCover.replace("i.ibb.co.com", "i.ibb.co")
						}
						if (detailCover.startsWith("http://")) {
							detailCover = detailCover.replace("http://", "https://")
						}
						val released = obj.optString("released")

						detailFirstGenre = "-"
						currentGenresList.clear()
						val allGenresSb = StringBuilder()
						val genresArray = obj.optJSONArray("genres")
						if (genresArray != null && genresArray.length() > 0) {
							detailFirstGenre = genresArray.optString(0)
							for (g in 0 until genresArray.length()) {
								val gStr = genresArray.optString(g).trim()
								if (gStr.isNotEmpty()) {
									currentGenresList.add(gStr.lowercase(Locale.getDefault()))
									if (allGenresSb.isNotEmpty()) allGenresSb.append(", ")
									allGenresSb.append(gStr)
								}
							}
						}
						detailAllGenres = if (allGenresSb.isNotEmpty()) allGenresSb.toString() else detailFirstGenre

						if (released.length >= 4) {
							detailYear = released.substring(released.length - 4)
						}

						textJudul?.text = detailTitle
						textRating?.text = detailStatus
						textYear?.text = detailYear
						textGenre?.text = detailFirstGenre

						if (imagePoster != null && detailCover.isNotEmpty()) {
							Glide.with(applicationContext).load(Uri.parse(detailCover)).into(imagePoster!!)
						}

						val mgr = DongflixPlayerManager.getInstance()
						mgr.activeTitle = detailTitle
						mgr.activeCoverUrl = detailCover
						break
					}
				}

				val type = object : TypeToken<ArrayList<HashMap<String, Any>>>() {}.type
				val allList: ArrayList<HashMap<String, Any>>? = Gson().fromJson(_response, type)
				if (allList != null && recyclerRekomendasi != null) {
					val smartRekomList = getSmartRecommendations(allList, targetSlug, 6)
					recyclerRekomendasi?.adapter = RecyclerRekomendasiAdapter(smartRekomList)
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		applyOutfitToAllViews()
		checkStopShimmerIfDone()
	}

	private fun extractKeywords(text: String?): Set<String> {
		val keywords = HashSet<String>()
		if (text.isNullOrEmpty()) return keywords

		val tokens = text.lowercase(Locale.getDefault())
			.replace("[^a-z0-9\\s]".toRegex(), " ")
			.split("\\s+".toRegex())
		for (t in tokens) {
			if (t.length >= 3 && !STOP_WORDS.contains(t)) {
				keywords.add(t)
			}
		}
		return keywords
	}

	private fun getSmartRecommendations(
		allList: ArrayList<HashMap<String, Any>>,
		currentSlug: String,
		maxCount: Int
	): ArrayList<HashMap<String, Any>> {
		val baseTitleWords = extractKeywords(detailTitle)
		val baseSynopsisWords = extractKeywords(detailSynopsis)
		val baseGenres = HashSet(currentGenresList)

		val scoredItems = ArrayList<ScoredAnimeItem>()

		for (item in allList) {
			val slug = item["slug"]?.toString() ?: ""
			if (slug.isEmpty() || slug == currentSlug) continue

			var score = 0

			val rawGenres = item["genres"]
			if (rawGenres is List<*>) {
				for (gObj in rawGenres) {
					if (gObj != null && baseGenres.contains(gObj.toString().trim().lowercase(Locale.getDefault()))) {
						score += 12
					}
				}
			} else if (rawGenres != null) {
				val gStr = rawGenres.toString().lowercase(Locale.getDefault())
				for (bg in baseGenres) {
					if (gStr.contains(bg)) score += 12
				}
			}

			val itemTitle = item["title"]?.toString() ?: ""
			val itemTitleWords = extractKeywords(itemTitle)
			for (w in itemTitleWords) {
				if (baseTitleWords.contains(w)) score += 15
			}

			val itemSynopsis = item["synopsis"]?.toString() ?: ""
			val itemSynopsisWords = extractKeywords(itemSynopsis)
			for (sw in itemSynopsisWords) {
				if (baseSynopsisWords.contains(sw)) score += 4
			}

			val itemStatus = item["status"]?.toString() ?: ""
			if (detailStatus.isNotEmpty() && detailStatus.equals(itemStatus, ignoreCase = true)) {
				score += 2
			}

			scoredItems.add(ScoredAnimeItem(item, score))
		}

		scoredItems.sortWith { a, b -> b.score.compareTo(a.score) }

		val result = ArrayList<HashMap<String, Any>>()
		for (i in 0 until minOf(maxCount, scoredItems.size)) {
			result.add(scoredItems[i].item)
		}
		return result
	}

	private fun createShimmerDrawable(cornerRadiusPx: Float): Drawable {
		return object : Drawable() {
			private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
				color = Color.parseColor("#12141A")
			}
			private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
			private val shaderMatrix = Matrix()
			private var gradient: LinearGradient? = null
			private var lastWidth = -1

			override fun draw(canvas: Canvas) {
				val b = bounds
				val w = b.width()
				val h = b.height()
				if (w <= 0 || h <= 0) return

				val rect = RectF(b)
				canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, basePaint)

				if (gradient == null || lastWidth != w) {
					lastWidth = w
					gradient = LinearGradient(
						0f, 0f, w.toFloat(), 0f,
						intArrayOf(
							Color.TRANSPARENT,
							Color.parseColor("#1AFFFFFF"),
							Color.parseColor("#33FFFFFF"),
							Color.parseColor("#1AFFFFFF"),
							Color.TRANSPARENT
						),
						floatArrayOf(0f, 0.35f, 0.5f, 0.65f, 1f),
						Shader.TileMode.CLAMP
					)
					shimmerPaint.shader = gradient
				}

				val translateX = -w + (2f * w * shimmerPhase)
				shaderMatrix.setTranslate(translateX, 0f)
				gradient?.setLocalMatrix(shaderMatrix)
				canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shimmerPaint)
			}

			override fun setAlpha(alpha: Int) {}
			override fun setColorFilter(colorFilter: ColorFilter?) {}
			@Deprecated("Deprecated in Java")
			override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
		}
	}

	private fun startLoadingShimmer() {
		val d = resources.displayMetrics.density

		imagePoster?.setImageDrawable(createShimmerDrawable(32 * d))
		textJudul?.apply {
			text = "                    "
			background = createShimmerDrawable(6 * d)
		}
		textRating?.apply {
			text = "        "
			background = createShimmerDrawable(4 * d)
		}
		textYear?.apply {
			text = "      "
			background = createShimmerDrawable(4 * d)
		}
		textGenre?.apply {
			text = "        "
			background = createShimmerDrawable(4 * d)
		}

		recyclerEpisode?.adapter = ShimmerEpisodeAdapter(6)
		recyclerRekomendasi?.adapter = ShimmerRekomendasiAdapter(6)

		shimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = 1250L
			repeatCount = ValueAnimator.INFINITE
			interpolator = LinearInterpolator()
			addUpdateListener { animation ->
				shimmerPhase = animation.animatedValue as Float
				if (isDetailLoading) {
					imagePoster?.invalidate()
					textJudul?.invalidate()
					textRating?.invalidate()
					textYear?.invalidate()
					textGenre?.invalidate()
				}
				val epRv = recyclerEpisode
				if (isEpisodesLoading && epRv != null) {
					for (i in 0 until epRv.childCount) {
						epRv.getChildAt(i).invalidate()
					}
				}
				val rekRv = recyclerRekomendasi
				if (isRekomLoading && rekRv != null) {
					for (i in 0 until rekRv.childCount) {
						val child = rekRv.getChildAt(i)
						child.invalidate()
						if (child is ViewGroup) {
							for (j in 0 until child.childCount) {
								child.getChildAt(j).invalidate()
							}
						}
					}
				}
			}
			start()
		}
	}

	private fun clearDetailShimmer() {
		textJudul?.background = null
		textRating?.background = null
		textYear?.background = null
		textGenre?.background = null
	}

	private fun checkStopShimmerIfDone() {
		if (!isDetailLoading && !isEpisodesLoading && !isRekomLoading) {
			shimmerAnimator?.cancel()
			shimmerAnimator = null
		}
	}

	private fun initializeLogic() {
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			window.apply {
				clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
				addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
				statusBarColor = 0xFF000000.toInt()
			}
		}

		findViewById<View>(R.id.rootWatchLayout)?.setPadding(0, 0, 0, 0)
		btnLock?.visibility = View.GONE

		currentSlug = intent.getStringExtra("slug")?.trim() ?: ""

		_WatchUI()
		setupCommentSection()
		updateBottomDividerState()
		loadUsersAndStickersFromFirebase()

		recyclerEpisode?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
		recyclerRekomendasi?.apply {
			layoutManager = GridLayoutManager(this@WatchActivity, 3)
			isNestedScrollingEnabled = false
		}

		val mgr = DongflixPlayerManager.getInstance()
		val resumeFlag = intent.getBooleanExtra("resume_mini_player", false)

		if ((resumeFlag || (mgr.isMiniPlayerActive && currentSlug == mgr.activeSlug)) && mgr.sharedMediaPlayer != null) {
			isResumingFromMiniPlayer = true
			initialEpisodeStarted = true
			mediaPlayer = mgr.sharedMediaPlayer
			mgr.isMiniPlayerActive = false

			currentPlayingUrl = mgr.activeStreamUrl
			currentEpisodeIndex = mgr.activeEpisodeIndex
			selectedServerIndex = mgr.activeSelectedServerIndex
			selectedQualityIndex = mgr.activeSelectedQualityIndex
			isAutoQualityMode = mgr.activeIsAutoQuality

			if (mgr.activeEpisodeList.isNotEmpty()) {
				episodeList = ArrayList(mgr.activeEpisodeList)
			}
			if (mgr.activeServerNames.isNotEmpty()) {
				currentEpisodeServerNames = ArrayList(mgr.activeServerNames)
				currentEpisodeServerUrls = ArrayList(mgr.activeServerUrls)
			}
			if (mgr.activeQualityNames.isNotEmpty()) {
				qualityNames = ArrayList(mgr.activeQualityNames)
				qualityUrls = ArrayList(mgr.activeQualityUrls)
				updateQualityBadgeText()
			}
			if (mgr.activeServerName.isNotEmpty()) {
				textActionServer?.text = mgr.activeServerName
			}

			if (mgr.cachedAllCatalogJson.isNotEmpty()) {
				handleCatalogJson(mgr.cachedAllCatalogJson)
			} else {
				reqWatch.startRequestNetwork("GET", CATALOG_URL, "", reqWatchRequestListener)
			}

			if (mgr.cachedEpisodesJson.isNotEmpty()) {
				handleEpisodesJson(mgr.cachedEpisodesJson, true)
			} else {
				reqWatch.startRequestNetwork("GET", "$CATALOG_URL?slug=$currentSlug", "slug", reqWatchRequestListener)
			}
		} else {
			mgr.stopAndCloseEverything(this)
			startLoadingShimmer()
			loadWatchHistoryFromFirebase()
			reqWatch.startRequestNetwork("GET", CATALOG_URL, "", reqWatchRequestListener)
			reqWatch.startRequestNetwork("GET", "$CATALOG_URL?slug=$currentSlug", "slug", reqWatchRequestListener)
		}

		_initPlayerViews()
		applyOutfitToAllViews()

		val resumedMp = mediaPlayer
		if (isResumingFromMiniPlayer && resumedMp != null) {
			try {
				val dur = resumedMp.duration
				if (dur > 0) playerSeekBar?.max = dur
				btnPlay?.apply {
					isEnabled = true
					alpha = 1.0f
					setImageResource(if (resumedMp.isPlaying) R.drawable.ic_pause else R.drawable.ic_play1)
				}
				val vw = resumedMp.videoWidth
				val vh = resumedMp.videoHeight
				if (vw > 0 && vh > 0) {
					currentVideoWidth = vw
					currentVideoHeight = vh
					adjustVideoAspectRatio()
				}
				progressLoop?.let { loop ->
					playerHandler?.removeCallbacks(loop)
					playerHandler?.post(loop)
				}
			} catch (ignored: Exception) {
			}
		}
	}

	fun _WatchUI() {
		gradientTop?.background = GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			intArrayOf(0xCC000000.toInt(), 0x00000000)
		)
		gradientBottom?.background = GradientDrawable(
			GradientDrawable.Orientation.BOTTOM_TOP,
			intArrayOf(0xCC000000.toInt(), 0x00000000)
		)

		val seekPills = arrayOf(animSeekLeft, animSeekRight, pillSpeedHold)
		for (pill in seekPills) {
			pill?.background = GradientDrawable().apply {
				cornerRadius = 100f
				setColor(0xB3000000.toInt())
			}
		}

		btnUnlockScreen?.background = GradientDrawable().apply {
			cornerRadius = 100f
			setColor(0xCC0B0D11.toInt())
		}

		btnFullscreenEpisodes?.setBackgroundColor(Color.TRANSPARENT)

		iconExpandDetail?.background = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(Color.parseColor("#12141A"))
		}

		btnSortEpisodes?.background = GradientDrawable().apply {
			cornerRadius = 100f
			setColor(Color.parseColor("#12141A"))
		}

		val actionBtns = arrayOf(btnActionServer, btnActionDownload, btnActionShare, btnActionReport)
		for (btn in actionBtns) {
			btn?.background = GradientDrawable().apply {
				cornerRadius = 100f
				setColor(Color.parseColor("#12141A"))
			}
		}
	}

	private fun updateBottomDividerState() {
		val div = dividerLineView ?: return
		val miniBar = miniVideoProgressBar ?: return

		if (isFullscreen) {
			div.visibility = View.GONE
		} else {
			div.visibility = View.VISIBLE
			miniBar.visibility = if (areControlsVisible) View.INVISIBLE else View.VISIBLE
		}
	}

	private fun applyPlaybackSpeedValue(speed: Float) {
		val mp = mediaPlayer ?: return
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			try {
				val wasPlaying = mp.isPlaying
				val params = mp.playbackParams ?: PlaybackParams()
				params.speed = speed
				mp.playbackParams = params
				if (!wasPlaying) mp.pause()
			} catch (ignored: Exception) {
			}
		}
	}

	private fun startHold2xSpeed() {
		val mp = mediaPlayer ?: return
		if (isScreenLocked || !mp.isPlaying) return
		isHolding2xSpeed = true
		applyPlaybackSpeedValue(2.0f)
		pillSpeedHold?.apply {
			visibility = View.VISIBLE
			bringToFront()
		}
	}

	private fun stopHold2xSpeed() {
		if (!isHolding2xSpeed) return
		isHolding2xSpeed = false
		applyPlaybackSpeedValue(currentPlaybackSpeed)
		pillSpeedHold?.visibility = View.GONE
	}

	fun adjustVideoAspectRatio() {
		val texView = playerTextureView ?: return
		val container = layoutPlayer ?: return
		if (currentVideoWidth <= 0 || currentVideoHeight <= 0) return

		container.post {
			val containerWidth = container.width
			val containerHeight = container.height
			if (containerWidth <= 0 || containerHeight <= 0) return@post

			val videoAspect = currentVideoWidth.toFloat() / currentVideoHeight.toFloat()
			val containerAspect = containerWidth.toFloat() / containerHeight.toFloat()

			val targetWidth: Int
			val targetHeight: Int

			if (isFillModeActive) {
				if (videoAspect > containerAspect) {
					targetHeight = containerHeight
					targetWidth = (containerHeight * videoAspect).roundToInt()
				} else {
					targetWidth = containerWidth
					targetHeight = (containerWidth / videoAspect).roundToInt()
				}
			} else {
				if (videoAspect > containerAspect) {
					targetWidth = containerWidth
					targetHeight = (containerWidth / videoAspect).roundToInt()
				} else {
					targetHeight = containerHeight
					targetWidth = (containerHeight * videoAspect).roundToInt()
				}
			}

			val lp = FrameLayout.LayoutParams(targetWidth, targetHeight).apply {
				gravity = Gravity.CENTER
			}
			texView.layoutParams = lp
			texView.scaleX = 1.0f
			texView.scaleY = 1.0f
		}
	}

	private fun showSeekAnimation(animView: View?) {
		if (animView == null) return
		animView.animate().cancel()
		animView.visibility = View.VISIBLE
		animView.alpha = 1f
		animView.animate()
			.alpha(0f)
			.setStartDelay(450L)
			.setDuration(250L)
			.withEndAction { animView.visibility = View.GONE }
			.start()
	}

	private fun lockScreenControls() {
		isScreenLocked = true
		hideControls()
		showUnlockPillTemporarily()
		Toast.makeText(this, "Layar dikunci", Toast.LENGTH_SHORT).show()
	}

	private fun unlockScreenControls() {
		isScreenLocked = false
		btnUnlockScreen?.visibility = View.GONE
		hideUnlockBtnRunnable?.let { playerHandler?.removeCallbacks(it) }
		showControls()
		Toast.makeText(this, "Kunci layar dibuka", Toast.LENGTH_SHORT).show()
	}

	private fun showUnlockPillTemporarily() {
		val unlockBtn = btnUnlockScreen ?: return
		unlockBtn.visibility = View.VISIBLE
		unlockBtn.bringToFront()
		hideUnlockBtnRunnable?.let { cb ->
			playerHandler?.removeCallbacks(cb)
			playerHandler?.postDelayed(cb, 2500L)
		}
	}

	fun toggleSortEpisodes() {
		if (episodeList.size <= 1) return

		val currentlyPlayingItem = episodeList[currentEpisodeIndex]
		Collections.reverse(episodeList)
		isNewestFirst = !isNewestFirst

		for (i in episodeList.indices) {
			if (episodeList[i] === currentlyPlayingItem) {
				currentEpisodeIndex = i
				break
			}
		}

		textSortEpisodes?.text = if (isNewestFirst) "Terbaru" else "Terlama"

		recyclerEpisode?.adapter?.let { adp ->
			adp.notifyDataSetChanged()
			recyclerEpisode?.scrollToPosition(currentEpisodeIndex)
		}
		updateEpisodeNavButtons()
	}

	private fun isLandscapeOrFullscreen(): Boolean {
		return isFullscreen || resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
	}

	private fun createAdaptiveSheetDialog(contentView: View, allowVideoInteraction: Boolean = false): Dialog {
		val d = resources.displayMetrics.density

		if (isLandscapeOrFullscreen()) {
			val sideDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen).apply {
				requestWindowFeature(Window.FEATURE_NO_TITLE)
			}

			contentView.background = GradientDrawable().apply {
				setColor(Color.parseColor("#0B0D11"))
				cornerRadii = floatArrayOf(18 * d, 18 * d, 0f, 0f, 0f, 0f, 18 * d, 18 * d)
			}

			if (contentView is ViewGroup && contentView.childCount > 0) {
				val firstChild = contentView.getChildAt(0)
				if (firstChild !is LinearLayout) {
					firstChild.visibility = View.GONE
				}
			}
			contentView.setPadding((22 * d).toInt(), (20 * d).toInt(), (22 * d).toInt(), (20 * d).toInt())

			sideDialog.setContentView(contentView)
			sideDialog.setCanceledOnTouchOutside(true)

			sideDialog.window?.apply {
				setLayout((340 * d).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
				setGravity(Gravity.RIGHT or Gravity.CENTER_VERTICAL)
				addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
				setDimAmount(0.45f)
				@Suppress("DEPRECATION")
				decorView.systemUiVisibility =
					View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
						View.SYSTEM_UI_FLAG_FULLSCREEN or
						View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			}

			sideDialog.setOnDismissListener {
				if (isFullscreen) {
					@Suppress("DEPRECATION")
					window.decorView.systemUiVisibility =
						View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
							View.SYSTEM_UI_FLAG_FULLSCREEN or
							View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				}
			}

			sideDialog.show()
			return sideDialog
		} else {
			val sheetDialog = BottomSheetDialog(this)
			val sheetBehaviorHolder = arrayOfNulls<BottomSheetBehavior<FrameLayout>>(1)
			val scrollableChildHolder = arrayOfNulls<ScrollView>(1)

			if (contentView is ViewGroup) {
				for (i in 0 until contentView.childCount) {
					val child = contentView.getChildAt(i)
					if (child is ScrollView) {
						scrollableChildHolder[0] = child
						child.isNestedScrollingEnabled = true
						child.viewTreeObserver.addOnScrollChangedListener {
							sheetBehaviorHolder[0]?.isDraggable = (child.scrollY <= 0)
						}
					}
				}
			}

			sheetDialog.setContentView(contentView)

			if (allowVideoInteraction) {
				sheetDialog.setCanceledOnTouchOutside(false)
				sheetDialog.window?.apply {
					clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
					setDimAmount(0f)
					@Suppress("DEPRECATION")
					addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
					@Suppress("DEPRECATION")
					addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
				}
			}

			sheetDialog.setOnShowListener { dialog ->
				val dialogObj = dialog as BottomSheetDialog
				val bottomSheet = dialogObj.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
				if (bottomSheet != null) {
					bottomSheet.setBackgroundColor(Color.TRANSPARENT)
					bottomSheet.backgroundTintList = null
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						bottomSheet.elevation = 0f
					}

					val visibleFrame = Rect()
					window.decorView.getWindowVisibleDisplayFrame(visibleFrame)

					var playerBottom = 0
					val pView = layoutPlayer
					if (pView != null) {
						val loc = IntArray(2)
						pView.getLocationOnScreen(loc)
						playerBottom = loc[1] + pView.height
					}

					val targetSheetHeight = visibleFrame.bottom - playerBottom
					if (targetSheetHeight > (200 * resources.displayMetrics.density).toInt()) {
						val sheetLp = bottomSheet.layoutParams
						sheetLp.height = targetSheetHeight
						bottomSheet.layoutParams = sheetLp
					}

					val behavior = BottomSheetBehavior.from(bottomSheet)
					sheetBehaviorHolder[0] = behavior
					behavior.peekHeight = targetSheetHeight
					behavior.skipCollapsed = true
					behavior.state = BottomSheetBehavior.STATE_EXPANDED

					val isAtTop = scrollableChildHolder[0] == null || scrollableChildHolder[0]!!.scrollY <= 0
					behavior.isDraggable = isAtTop
				}
			}
			sheetDialog.show()
			return sheetDialog
		}
	}

	private fun buildAdaptiveHeader(titleText: String, holderDialog: Array<Dialog?>): LinearLayout {
		val d = resources.displayMetrics.density

		val header = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding(0, 0, 0, (16 * d).toInt())
		}

		val tvTitle = TextView(this).apply {
			text = titleText
			setTextColor(Color.WHITE)
			textSize = 19f
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}
		header.addView(tvTitle)

		val btnClose = TextView(this).apply {
			layoutParams = LinearLayout.LayoutParams((32 * d).toInt(), (32 * d).toInt())
			gravity = Gravity.CENTER
			text = "✕"
			setTextColor(Color.parseColor("#CBD5E1"))
			textSize = 14f
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			background = GradientDrawable().apply {
				shape = GradientDrawable.OVAL
				setColor(Color.parseColor("#181B22"))
			}
			setOnClickListener { holderDialog[0]?.dismiss() }
		}
		header.addView(btnClose)

		return header
	}

	fun showSettingsBottomSheet() {
		val dialogHolder = arrayOfNulls<Dialog>(1)
		val d = resources.displayMetrics.density

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((20 * d).toInt(), (12 * d).toInt(), (20 * d).toInt(), (24 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#0B0D11"))
				cornerRadii = floatArrayOf(22 * d, 22 * d, 22 * d, 22 * d, 0f, 0f, 0f, 0f)
			}
		}

		val handle = View(this).apply {
			layoutParams = LinearLayout.LayoutParams((38 * d).toInt(), (4 * d).toInt()).apply {
				gravity = Gravity.CENTER_HORIZONTAL
				bottomMargin = (12 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#1E222B"))
				cornerRadius = 100f
			}
		}
		root.addView(handle)

		root.addView(buildAdaptiveHeader("Pengaturan Pemutaran", dialogHolder))

		val currentQualityLabel = btnQuality?.text?.toString() ?: "Auto"
		val currentSpeedLabel = if (abs(currentPlaybackSpeed - 1.0f) < 0.01f) "Normal" else "${currentPlaybackSpeed}x"
		val currentServerLabel = textActionServer?.text?.toString() ?: "Default"

		val menuTitles = arrayOf("Kualitas", "Kecepatan Putar", "Server Streaming")
		val menuValues = arrayOf(currentQualityLabel, currentSpeedLabel, currentServerLabel)

		for (i in menuTitles.indices) {
			val row = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				setPadding((4 * d).toInt(), (16 * d).toInt(), (4 * d).toInt(), (16 * d).toInt())
			}

			val tvLeft = TextView(this).apply {
				text = menuTitles[i]
				setTextColor(Color.WHITE)
				textSize = 15f
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
			}
			row.addView(tvLeft)

			val tvRight = TextView(this).apply {
				text = "${menuValues[i]}  >"
				setTextColor(Color.parseColor("#D1D5DB"))
				textSize = 14f
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			}
			row.addView(tvRight)

			row.setOnClickListener {
				dialogHolder[0]?.dismiss()
				when (i) {
					0 -> showQualityDialog()
					1 -> showSpeedDialog()
					else -> showServerDialog()
				}
			}

			root.addView(row)
		}

		dialogHolder[0] = createAdaptiveSheetDialog(root)
	}

	fun showDetailBottomSheet() {
		val dialogHolder = arrayOfNulls<Dialog>(1)
		val d = resources.displayMetrics.density

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((20 * d).toInt(), (12 * d).toInt(), (20 * d).toInt(), (28 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#0B0D11"))
				cornerRadii = floatArrayOf(22 * d, 22 * d, 22 * d, 22 * d, 0f, 0f, 0f, 0f)
			}
		}

		val handle = View(this).apply {
			layoutParams = LinearLayout.LayoutParams((38 * d).toInt(), (4 * d).toInt()).apply {
				gravity = Gravity.CENTER_HORIZONTAL
				bottomMargin = (12 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#1E222B"))
				cornerRadius = 100f
			}
		}
		root.addView(handle)

		root.addView(buildAdaptiveHeader("Sinopsis", dialogHolder))

		val scrollView = ScrollView(this).apply {
			isVerticalScrollBarEnabled = false
		}

		val content = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
		}
		scrollView.addView(content)

		val infoHeaderRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.TOP
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				bottomMargin = (18 * d).toInt()
			}
		}

		val posterCard = CardView(this).apply {
			layoutParams = LinearLayout.LayoutParams((96 * d).toInt(), (134 * d).toInt()).apply {
				rightMargin = (16 * d).toInt()
			}
			radius = 8 * d
			cardElevation = 0f
			setCardBackgroundColor(Color.parseColor("#12141A"))
		}

		val ivSheetPoster = ImageView(this).apply {
			layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			scaleType = ImageView.ScaleType.CENTER_CROP
			if (detailCover.isNotEmpty()) {
				Glide.with(applicationContext).load(Uri.parse(detailCover)).into(this)
			}
		}
		posterCard.addView(ivSheetPoster)
		infoHeaderRow.addView(posterCard)

		val rightCol = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}

		val tvSheetTitle = TextView(this).apply {
			text = if (detailTitle.isEmpty()) (textJudul?.text?.toString() ?: "") else detailTitle
			setTextColor(Color.WHITE)
			textSize = 18f
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			maxLines = 2
			ellipsize = TextUtils.TruncateAt.END
		}
		rightCol.addView(tvSheetTitle)

		val tvSubGenres = TextView(this).apply {
			text = detailAllGenres
			setTextColor(Color.parseColor("#9CA3AF"))
			textSize = 13f
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			maxLines = 1
			ellipsize = TextUtils.TruncateAt.END
			setPadding(0, (6 * d).toInt(), 0, (10 * d).toInt())
		}
		rightCol.addView(tvSubGenres)

		val badgeRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				bottomMargin = (12 * d).toInt()
			}
		}

		val badgeItems = arrayOf(detailStatus, detailYear, detailFirstGenre)
		for (b in badgeItems.indices) {
			val bView = TextView(this).apply {
				text = badgeItems[b]
				textSize = 12f
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				setTextColor(Color.parseColor("#CBD5E1"))
				setPadding((8 * d).toInt(), (4 * d).toInt(), (8 * d).toInt(), (4 * d).toInt())
				background = GradientDrawable().apply {
					cornerRadius = 5 * d
					setColor(Color.parseColor("#12141A"))
				}
				layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
					if (b < badgeItems.size - 1) rightMargin = (8 * d).toInt()
				}
			}
			badgeRow.addView(bView)
		}
		rightCol.addView(badgeRow)

		val studioRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
		}

		val miniThumb = CardView(this).apply {
			layoutParams = LinearLayout.LayoutParams((22 * d).toInt(), (22 * d).toInt()).apply {
				rightMargin = (8 * d).toInt()
			}
			radius = 11 * d
			cardElevation = 0f
			setCardBackgroundColor(Color.parseColor("#12141A"))
		}

		val ivMini = ImageView(this).apply {
			layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			scaleType = ImageView.ScaleType.CENTER_CROP
			if (detailCover.isNotEmpty()) {
				Glide.with(applicationContext).load(Uri.parse(detailCover)).into(this)
			}
		}
		miniThumb.addView(ivMini)
		studioRow.addView(miniThumb)

		val tvEpSummary = TextView(this).apply {
			text = "Dongflix • ${episodeList.size} Episode"
			setTextColor(Color.parseColor("#E5E7EB"))
			textSize = 13f
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}
		studioRow.addView(tvEpSummary)

		rightCol.addView(studioRow)
		infoHeaderRow.addView(rightCol)
		content.addView(infoHeaderRow)

		val tvSinopsisBody = TextView(this).apply {
			text = if (detailSynopsis.isEmpty()) "Sinopsis belum tersedia untuk donghua ini." else detailSynopsis
			setTextColor(Color.parseColor("#9CA3AF"))
			textSize = 13.5f
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			setLineSpacing(5 * d, 1f)
		}
		content.addView(tvSinopsisBody)

		root.addView(scrollView)
		dialogHolder[0] = createAdaptiveSheetDialog(root, true)
	}

	private fun showCleanOptionBottomSheet(
		title: String,
		optionTitles: ArrayList<String>,
		activeIdx: Int,
		listener: OnSheetItemClickListener
	) {
		val dialogHolder = arrayOfNulls<Dialog>(1)
		val d = resources.displayMetrics.density

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((20 * d).toInt(), (12 * d).toInt(), (20 * d).toInt(), (24 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#0B0D11"))
				cornerRadii = floatArrayOf(22 * d, 22 * d, 22 * d, 22 * d, 0f, 0f, 0f, 0f)
			}
		}

		val handle = View(this).apply {
			layoutParams = LinearLayout.LayoutParams((38 * d).toInt(), (4 * d).toInt()).apply {
				gravity = Gravity.CENTER_HORIZONTAL
				bottomMargin = (12 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#1E222B"))
				cornerRadius = 100f
			}
		}
		root.addView(handle)

		root.addView(buildAdaptiveHeader(title, dialogHolder))

		val scrollView = ScrollView(this).apply {
			isVerticalScrollBarEnabled = false
		}
		val listContainer = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
		}
		scrollView.addView(listContainer)

		for (i in optionTitles.indices) {
			val isSelected = (i == activeIdx)

			val itemRow = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				setPadding((16 * d).toInt(), (14 * d).toInt(), (16 * d).toInt(), (14 * d).toInt())
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					bottomMargin = (4 * d).toInt()
				}
				if (isSelected) {
					background = GradientDrawable().apply {
						cornerRadius = 12 * d
						setColor(Color.parseColor("#1E3A8A"))
					}
				} else {
					setBackgroundColor(Color.TRANSPARENT)
				}
			}

			val tvName = TextView(this).apply {
				text = optionTitles[i]
				setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#E5E7EB"))
				textSize = 15f
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
			}
			itemRow.addView(tvName)

			if (isSelected) {
				val checkCircle = TextView(this).apply {
					layoutParams = LinearLayout.LayoutParams((20 * d).toInt(), (20 * d).toInt())
					gravity = Gravity.CENTER
					text = "✓"
					setTextColor(Color.parseColor("#0B0D11"))
					textSize = 11f
					setTypeface(getOutfitTypeface(), Typeface.NORMAL)
					background = GradientDrawable().apply {
						shape = GradientDrawable.OVAL
						setColor(Color.parseColor("#38BDF8"))
					}
				}
				itemRow.addView(checkCircle)
			}

			itemRow.setOnClickListener {
				dialogHolder[0]?.dismiss()
				listener.onItemClick(i)
			}

			listContainer.addView(itemRow)
		}

		root.addView(scrollView)
		dialogHolder[0] = createAdaptiveSheetDialog(root)
	}

	fun showAllEpisodesBottomSheet() {
		if (episodeList.isEmpty()) {
			Toast.makeText(this@WatchActivity, "Daftar episode belum dimuat", Toast.LENGTH_SHORT).show()
			return
		}

		val dialogHolder = arrayOfNulls<Dialog>(1)
		val d = resources.displayMetrics.density

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((20 * d).toInt(), (12 * d).toInt(), (20 * d).toInt(), (24 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#0B0D11"))
				cornerRadii = floatArrayOf(22 * d, 22 * d, 22 * d, 22 * d, 0f, 0f, 0f, 0f)
			}
		}

		val handle = View(this).apply {
			layoutParams = LinearLayout.LayoutParams((38 * d).toInt(), (4 * d).toInt()).apply {
				gravity = Gravity.CENTER_HORIZONTAL
				bottomMargin = (12 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#1E222B"))
				cornerRadius = 100f
			}
		}
		root.addView(handle)

		root.addView(buildAdaptiveHeader("Semua Episode (${episodeList.size})", dialogHolder))

		val scrollView = ScrollView(this).apply {
			isVerticalScrollBarEnabled = false
		}

		val gridContainer = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
		}
		scrollView.addView(gridContainer)

		val columns = if (isLandscapeOrFullscreen()) 4 else 5
		val total = episodeList.size
		val rows = ceil(total.toDouble() / columns).toInt()

		for (r in 0 until rows) {
			val rowLayout = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					bottomMargin = (10 * d).toInt()
				}
			}

			for (c in 0 until columns) {
				val epIdx = (r * columns) + c
				val cellLp = LinearLayout.LayoutParams(0, (46 * d).toInt(), 1f).apply {
					if (c < columns - 1) rightMargin = (8 * d).toInt()
				}

				if (epIdx < total) {
					val isActive = (epIdx == currentEpisodeIndex)
					val itemData = episodeList[epIdx]

					var epLabel = (epIdx + 1).toString()
					if (itemData["episode"] != null) {
						epLabel = itemData["episode"].toString().replace("Ep ", "").replace("Eps ", "").trim()
					} else if (itemData["title"] != null) {
						val parsedNum = itemData["title"].toString().replace("[^0-9]".toRegex(), "").trim()
						if (parsedNum.isNotEmpty()) epLabel = parsedNum
					}

					val cellBtn = TextView(this).apply {
						layoutParams = cellLp
						gravity = Gravity.CENTER
						text = epLabel
						textSize = 14f
						setTypeface(getOutfitTypeface(), Typeface.NORMAL)
						setTextColor(if (isActive) Color.WHITE else Color.parseColor("#CBD5E1"))
						background = GradientDrawable().apply {
							cornerRadius = 10 * d
							setColor(if (isActive) Color.parseColor("#3B82F6") else Color.parseColor("#12141A"))
						}
						setOnClickListener {
							dialogHolder[0]?.dismiss()
							if (currentEpisodeIndex != epIdx || mediaPlayer?.isPlaying != true) {
								playEpisodeByIndex(epIdx, 0)
							}
						}
					}

					rowLayout.addView(cellBtn)
				} else {
					val spacer = View(this).apply { layoutParams = cellLp }
					rowLayout.addView(spacer)
				}
			}
			gridContainer.addView(rowLayout)
		}

		root.addView(scrollView)
		dialogHolder[0] = createAdaptiveSheetDialog(root)
	}

	fun showServerDialog() {
		if (currentEpisodeServerNames.isEmpty()) {
			Toast.makeText(this@WatchActivity, "Daftar server belum siap", Toast.LENGTH_SHORT).show()
			return
		}

		showCleanOptionBottomSheet(
			"Server",
			currentEpisodeServerNames,
			selectedServerIndex
		) { which ->
			selectedServerIndex = which
			val chosenName = currentEpisodeServerNames[which]
			val chosenUrl = currentEpisodeServerUrls[which]

			textActionServer?.text = chosenName
			var curPos = 0
			mediaPlayer?.let { mp ->
				try {
					curPos = mp.currentPosition
				} catch (ignored: Exception) {
				}
			}
			extractStream(chosenName, chosenUrl, curPos)
		}
	}

	fun showQualityDialog() {
		if (qualityNames.isEmpty()) {
			Toast.makeText(this@WatchActivity, "Pilihan resolusi belum siap", Toast.LENGTH_SHORT).show()
			return
		}

		val titles = ArrayList<String>()
		val currentActiveRes = qualityNames[selectedQualityIndex]

		if (currentActiveRes.equals("Auto", ignoreCase = true) || currentActiveRes.equals("Default", ignoreCase = true)) {
			titles.add("Auto")
		} else {
			titles.add("Auto (${currentActiveRes.lowercase(Locale.getDefault())})")
		}

		for (q in qualityNames) {
			if (!q.equals("Auto", ignoreCase = true)) {
				titles.add(q.lowercase(Locale.getDefault()))
			}
		}

		var activeSheetIdx = 0
		if (!isAutoQualityMode) {
			val chosenLabel = qualityNames[selectedQualityIndex].lowercase(Locale.getDefault())
			for (i in 1 until titles.size) {
				if (titles[i].equals(chosenLabel, ignoreCase = true)) {
					activeSheetIdx = i
					break
				}
			}
		}

		showCleanOptionBottomSheet("Kualitas", titles, activeSheetIdx) { which ->
			var currentPos = 0
			mediaPlayer?.let { mp ->
				try {
					currentPos = mp.currentPosition
				} catch (ignored: Exception) {
				}
			}

			if (which == 0) {
				isAutoQualityMode = true
				val recIdx = getRecommendedQualityIndex()
				val needReload = (recIdx != selectedQualityIndex)
				selectedQualityIndex = recIdx
				updateQualityBadgeText()

				if (needReload) {
					playVideoUrl(qualityUrls[recIdx], currentPos)
				}
			} else {
				isAutoQualityMode = false
				val targetRes = titles[which]
				var realIdx = 0
				for (k in qualityNames.indices) {
					if (qualityNames[k].equals(targetRes, ignoreCase = true)) {
						realIdx = k
						break
					}
				}
				selectedQualityIndex = realIdx
				updateQualityBadgeText()
				playVideoUrl(qualityUrls[realIdx], currentPos)
			}
		}
	}

	fun showSpeedDialog() {
		val speedLabels = arrayListOf("0.5x", "0.75x", "Normal (1x)", "1.25x", "1.5x", "1.75x", "2x")
		val speedVals = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
		var activeIdx = 2
		for (i in speedVals.indices) {
			if (abs(speedVals[i] - currentPlaybackSpeed) < 0.01f) {
				activeIdx = i
				break
			}
		}

		showCleanOptionBottomSheet("Kecepatan Putar", speedLabels, activeIdx) { which ->
			val selectedSpeed = speedVals[which]
			currentPlaybackSpeed = selectedSpeed
			applyPlaybackSpeedValue(selectedSpeed)
		}
	}

	private fun minimizeToMiniPlayer() {
		mediaPlayer?.let { mp ->
			try {
				saveWatchHistoryToFirebase(mp.currentPosition, mp.duration, true)
			} catch (ignored: Exception) {
			}
		}

		val mp = mediaPlayer
		if (mp != null && currentPlayingUrl.isNotEmpty()) {
			val mgr = DongflixPlayerManager.getInstance()
			mgr.sharedMediaPlayer = mp
			mgr.isMiniPlayerActive = true
			mgr.activeSlug = currentSlug
			mgr.activeTitle = when {
				detailTitle.isNotEmpty() -> detailTitle
				!textJudul?.text.isNullOrEmpty() -> textJudul!!.text.toString()
				else -> "Dongflix"
			}
			mgr.activeCoverUrl = detailCover
			mgr.activeStreamUrl = currentPlayingUrl
			mgr.activeEpisodeIndex = currentEpisodeIndex
			mgr.activeEpisodeList = ArrayList(episodeList)

			mgr.activeServerName = textActionServer?.text?.toString() ?: ""
			mgr.activeSelectedServerIndex = selectedServerIndex
			mgr.activeServerNames = ArrayList(currentEpisodeServerNames)
			mgr.activeServerUrls = ArrayList(currentEpisodeServerUrls)
			mgr.activeQualityNames = ArrayList(qualityNames)
			mgr.activeQualityUrls = ArrayList(qualityUrls)
			mgr.activeSelectedQualityIndex = selectedQualityIndex
			mgr.activeIsAutoQuality = isAutoQualityMode

			if (currentEpisodeIndex in 0 until episodeList.size) {
				val epObj = episodeList[currentEpisodeIndex]["episode"]
				mgr.activeEpisodeLabel = epObj?.toString()?.replace("Ep ", "")?.replace("Eps ", "")?.trim()
					?: (currentEpisodeIndex + 1).toString()
			}

			mgr.updatePlaybackNotification(this)
			mediaPlayer = null
		}
		finish()
		overridePendingTransition(R.anim.no_anim, R.anim.slide_down_exit)
	}

	override fun onDestroy() {
		detachCommentsListener()
		shimmerAnimator?.cancel()
		shimmerAnimator = null

		playerHandler?.let { h ->
			progressLoop?.let { h.removeCallbacks(it) }
			hideControlsRunnable?.let { h.removeCallbacks(it) }
			autoDowngradeRunnable?.let { h.removeCallbacks(it) }
			hideUnlockBtnRunnable?.let { h.removeCallbacks(it) }
			h.removeCallbacksAndMessages(null)
		}

		val mp = mediaPlayer
		if (mp != null && !DongflixPlayerManager.getInstance().isMiniPlayerActive) {
			try {
				if (mp.isPlaying) mp.stop()
				mp.reset()
				mp.release()
			} catch (ignored: Exception) {
			}
			mediaPlayer = null
		}

		fallbackWebView?.let { wv ->
			try {
				wv.stopLoading()
				wv.destroy()
			} catch (ignored: Exception) {
			}
		}
		fallbackWebView = null

		playerSurface?.release()
		playerSurface = null

		window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		super.onDestroy()
	}

	override fun onPause() {
		super.onPause()
		stopHold2xSpeed()
		val mp = mediaPlayer
		if (mp != null) {
			try {
				saveWatchHistoryToFirebase(mp.currentPosition, mp.duration, true)
			} catch (ignored: Exception) {
			}
			if (mp.isPlaying && !DongflixPlayerManager.getInstance().isMiniPlayerActive) {
				mp.pause()
				btnPlay?.setImageResource(R.drawable.ic_play1)
			}
		}
		if (fallbackWebView != null && isWebViewFallbackActive) {
			fallbackWebView?.onPause()
		}
	}

	override fun onResume() {
		super.onResume()
		if (fallbackWebView != null && isWebViewFallbackActive) {
			fallbackWebView?.onResume()
		}
	}

	@Deprecated("Deprecated in Java")
	override fun onBackPressed() {
		if (isScreenLocked) {
			showUnlockPillTemporarily()
			return
		}
		if (isFullscreen) {
			toggleFullscreen()
		} else if (!isFinishing) {
			minimizeToMiniPlayer()
		}
	}

	fun getRecommendedQualityIndex(): Int {
		if (qualityNames.isEmpty()) return 0

		var targetHeight = 480
		try {
			val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
			if (cm != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					val caps = cm.getNetworkCapabilities(cm.activeNetwork)
					if (caps != null) {
						when {
							caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
								val downMbps = caps.linkDownstreamBandwidthKbps / 1000
								targetHeight = when {
									downMbps >= 15 -> 1080
									downMbps >= 5 -> 720
									else -> 480
								}
							}
							caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
								val downMbps = caps.linkDownstreamBandwidthKbps / 1000
								targetHeight = when {
									downMbps >= 10 -> 720
									downMbps >= 2 -> 480
									else -> 360
								}
							}
						}
					}
				} else {
					@Suppress("DEPRECATION")
					val info = cm.activeNetworkInfo
					@Suppress("DEPRECATION")
					if (info != null && info.isConnected) {
						@Suppress("DEPRECATION")
						targetHeight = if (info.type == ConnectivityManager.TYPE_WIFI) 720 else 480
					}
				}
			}
		} catch (ignored: Exception) {
		}

		for (i in qualityNames.indices) {
			if (qualityNames[i].equals("Auto", ignoreCase = true)) return i
		}

		var bestIdx = 0
		var smallestDiff = Int.MAX_VALUE
		for (i in qualityNames.indices) {
			val numStr = qualityNames[i].replace("[^0-9]".toRegex(), "")
			if (numStr.isNotEmpty()) {
				val resVal = numStr.toInt()
				val diff = abs(resVal - targetHeight)
				if (diff < smallestDiff) {
					smallestDiff = diff
					bestIdx = i
				}
			}
		}
		return bestIdx
	}

	fun triggerAutoDowngrade() {
		if (!isAutoQualityMode || qualityNames.size <= 1) return

		var currentRes = 9999
		val curNum = qualityNames[selectedQualityIndex].replace("[^0-9]".toRegex(), "")
		if (curNum.isNotEmpty()) currentRes = curNum.toInt()

		var lowerIdx = -1
		var closestLowerRes = -1

		for (i in qualityNames.indices) {
			if (i == selectedQualityIndex) continue
			val numStr = qualityNames[i].replace("[^0-9]".toRegex(), "")
			if (numStr.isNotEmpty()) {
				val resVal = numStr.toInt()
				if (resVal < currentRes && resVal > closestLowerRes) {
					closestLowerRes = resVal
					lowerIdx = i
				}
			}
		}

		if (lowerIdx != -1) {
			selectedQualityIndex = lowerIdx
			var currentPos = 0
			mediaPlayer?.let { mp ->
				try {
					currentPos = mp.currentPosition
				} catch (ignored: Exception) {
				}
			}
			updateQualityBadgeText()
			Toast.makeText(this@WatchActivity, "Jaringan lambat, otomatis ke ${qualityNames[lowerIdx]}", Toast.LENGTH_SHORT).show()
			playVideoUrl(qualityUrls[lowerIdx], currentPos)
		}
	}

	fun updateQualityBadgeText() {
		val badge = btnQuality ?: return
		if (qualityNames.isEmpty()) return
		if (isAutoQualityMode) {
			val activeRes = qualityNames[selectedQualityIndex]
			if (activeRes.equals("Auto", ignoreCase = true) || activeRes.equals("Default", ignoreCase = true)) {
				badge.text = "Auto"
			} else {
				badge.text = "Auto ($activeRes)"
			}
		} else {
			badge.text = qualityNames[selectedQualityIndex]
		}
	}

	fun resetControlsTimer() {
		hideControlsRunnable?.let { cb ->
			playerHandler?.removeCallbacks(cb)
			playerHandler?.postDelayed(cb, 3500L)
		}
	}

	fun showControls() {
		if (isScreenLocked) {
			showUnlockPillTemporarily()
			return
		}
		playerControlsOverlay?.apply {
			visibility = View.VISIBLE
			bringToFront()
		}
		areControlsVisible = true
		updateBottomDividerState()
		resetControlsTimer()
	}

	fun hideControls() {
		playerControlsOverlay?.visibility = View.GONE
		btnUnlockScreen?.visibility = View.GONE
		areControlsVisible = false
		updateBottomDividerState()
		hideControlsRunnable?.let { cb -> playerHandler?.removeCallbacks(cb) }
	}

	fun toggleControls() {
		if (isScreenLocked) {
			if (btnUnlockScreen?.visibility == View.VISIBLE) {
				btnUnlockScreen?.visibility = View.GONE
			} else {
				showUnlockPillTemporarily()
			}
			return
		}
		if (areControlsVisible) hideControls() else showControls()
	}

	fun updateEpisodeNavButtons() {
		val totalItems = recyclerEpisode?.adapter?.itemCount ?: episodeList.size

		val hasPrev = currentEpisodeIndex > 0
		btnPrevEpisode?.apply {
			isEnabled = hasPrev
			alpha = if (hasPrev) 1.0f else 0.35f
		}

		val hasNext = currentEpisodeIndex < (totalItems - 1) && totalItems > 1
		btnNextEpisode?.apply {
			isEnabled = hasNext
			alpha = if (hasNext) 1.0f else 0.35f
		}
	}

	fun playEpisodeByIndex(targetIndex: Int) {
		playEpisodeByIndex(targetIndex, 0)
	}

	fun playEpisodeByIndex(targetIndex: Int, initialSeekMs: Int) {
		if (episodeList.isEmpty()) return
		if (targetIndex !in episodeList.indices) return

		currentEpisodeIndex = targetIndex
		savedSeekPosition = maxOf(0, initialSeekMs)
		updateEpisodeNavButtons()

		runOnUiThread {
			recyclerEpisode?.let { rv ->
				rv.scrollToPosition(targetIndex)
				rv.adapter?.notifyDataSetChanged()
			}

			playerLoading?.visibility = View.VISIBLE
			btnPlay?.apply {
				isEnabled = false
				alpha = 0.5f
			}

			val item = episodeList[targetIndex]
			if (!item.containsKey("servers")) {
				playerLoading?.visibility = View.GONE
				btnPlay?.apply {
					isEnabled = true
					alpha = 1.0f
				}
				return@runOnUiThread
			}

			currentEpisodeServerNames.clear()
			currentEpisodeServerUrls.clear()

			try {
				val rawServers = item["servers"]
				val sArray = when (rawServers) {
					is JSONArray -> rawServers
					is ArrayList<*> -> JSONArray(rawServers)
					else -> JSONArray(rawServers.toString())
				}

				for (i in 0 until sArray.length()) {
					val itemObj = sArray.get(i)
					val sObj = when (itemObj) {
						is HashMap<*, *> -> JSONObject(itemObj)
						else -> sArray.getJSONObject(i)
					}

					val sName = sObj.optString("name", "Server ${i + 1}")
					val sLink = sObj.optString("link")
					if (sLink.isNotEmpty()) {
						currentEpisodeServerNames.add(sName)
						currentEpisodeServerUrls.add(sLink)
					}
				}
			} catch (ignored: Exception) {
				playerLoading?.visibility = View.GONE
				btnPlay?.apply {
					isEnabled = true
					alpha = 1.0f
				}
				return@runOnUiThread
			}

			if (currentEpisodeServerUrls.isEmpty()) {
				playerLoading?.visibility = View.GONE
				btnPlay?.apply {
					isEnabled = true
					alpha = 1.0f
				}
				return@runOnUiThread
			}

			var targetServerIdx = 0
			for (i in currentEpisodeServerNames.indices) {
				val name = currentEpisodeServerNames[i].lowercase(Locale.getDefault())
				if (name.contains("ok") || name.contains("odnoklassniki")) {
					targetServerIdx = i
					break
				}
			}

			selectedServerIndex = targetServerIdx
			val chosenServerName = currentEpisodeServerNames[targetServerIdx]
			val chosenServerUrl = currentEpisodeServerUrls[targetServerIdx]

			textActionServer?.text = chosenServerName

			saveWatchHistoryToFirebase(savedSeekPosition, 0, true)
			extractStream(chosenServerName, chosenServerUrl, savedSeekPosition)
		}
	}

	private fun hideFallbackWebView() {
		isWebViewFallbackActive = false
		fallbackWebView?.let { wv ->
			try {
				wv.stopLoading()
				wv.loadUrl("about:blank")
				wv.visibility = View.GONE
			} catch (ignored: Exception) {
			}
		}
		playerTextureView?.visibility = View.VISIBLE
	}

	private fun playViaWebViewInterceptor(embedUrl: String) {
		runOnUiThread {
			try {
				mediaPlayer?.let { mp ->
					try {
						mp.stop()
						mp.reset()
						mp.release()
					} catch (ignored: Exception) {
					}
				}
				mediaPlayer = null

				val container = layoutPlayer
				if (fallbackWebView == null && container != null) {
					fallbackWebView = WebView(this@WatchActivity).apply {
						layoutParams = FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.MATCH_PARENT
						)
						setBackgroundColor(Color.BLACK)
					}
					container.addView(fallbackWebView, 0)
				}

				fallbackWebView?.let { wv ->
					@Suppress("SetJavaScriptEnabled")
					wv.settings.apply {
						javaScriptEnabled = true
						domStorageEnabled = true
						mediaPlaybackRequiresUserGesture = false
						allowFileAccess = true
						userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
					}

					wv.webChromeClient = WebChromeClient()
					wv.webViewClient = object : WebViewClient() {
						override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request?.url != null) {
								val reqUrl = request.url.toString()
								val low = reqUrl.lowercase(Locale.getDefault())
								if ((low.contains(".m3u8") || low.contains(".mp4")) &&
									!low.contains("ads") && !low.contains("analytics") && !low.contains("tracker")
								) {
									runOnUiThread {
										if (isWebViewFallbackActive) {
											hideFallbackWebView()
											qualityNames.clear()
											qualityUrls.clear()
											qualityNames.add("Auto")
											qualityUrls.add(reqUrl)
											isAutoQualityMode = true
											selectedQualityIndex = 0
											updateQualityBadgeText()
											playVideoUrl(reqUrl, savedSeekPosition)
										}
									}
								}
							}
							return super.shouldInterceptRequest(view, request)
						}

						override fun onPageFinished(view: WebView?, url: String?) {
							playerLoading?.visibility = View.GONE
							btnPlay?.apply {
								isEnabled = true
								alpha = 1.0f
							}
						}
					}

					isWebViewFallbackActive = true
					playerTextureView?.visibility = View.GONE
					wv.visibility = View.VISIBLE
					playerControlsOverlay?.bringToFront()

					val extraHeaders = mapOf("Referer" to "https://anichin.cafe/")
					wv.loadUrl(embedUrl, extraHeaders)
				}
			} catch (e: Exception) {
				playerLoading?.visibility = View.GONE
			}
		}
	}

	fun playVideoUrl(rawUrl: String?, targetSeek: Int) {
		if (rawUrl.isNullOrBlank()) return

		hideFallbackWebView()

		var normalized = rawUrl.trim()
		if (normalized.startsWith("Https://")) {
			normalized = "https://${normalized.substring(8)}"
		} else if (normalized.startsWith("Http://")) {
			normalized = "http://${normalized.substring(7)}"
		}
		val cleanUrl = normalized
		currentPlayingUrl = cleanUrl
		savedSeekPosition = targetSeek

		runOnUiThread {
			playerLoading?.visibility = View.VISIBLE
			btnPlay?.apply {
				isEnabled = false
				alpha = 0.5f
			}

			autoDowngradeRunnable?.let { cb ->
				playerHandler?.removeCallbacks(cb)
				if (isAutoQualityMode) {
					playerHandler?.postDelayed(cb, 5000L)
				}
			}

			try {
				mediaPlayer?.let { oldMp ->
					try {
						oldMp.stop()
					} catch (ignored: Exception) {
					}
					try {
						oldMp.reset()
						oldMp.release()
					} catch (ignored: Exception) {
					}
				}

				val mp = MediaPlayer()
				mediaPlayer = mp

				val texView = playerTextureView
				if (texView != null && texView.isAvailable) {
					val st = texView.surfaceTexture
					if (st != null) {
						playerSurface?.release()
						playerSurface = Surface(st)
						mp.setSurface(playerSurface)
					}
				} else if (playerSurface != null && playerSurface?.isValid == true) {
					mp.setSurface(playerSurface)
				}

				val headers = HashMap<String, String>().apply {
					put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
					when {
						cleanUrl.contains("ok.ru") || cleanUrl.contains("odnoklassniki") -> {
							put("Referer", "https://ok.ru/")
						}
						cleanUrl.contains("dailymotion") || cleanUrl.contains("dmcdn") -> {
							put("Referer", "https://www.dailymotion.com/")
							put("Origin", "https://www.dailymotion.com")
							if (dmSessionCookie.isNotEmpty()) {
								put("Cookie", dmSessionCookie)
							}
						}
						cleanUrl.contains("rumble") -> {
							put("Referer", "https://rumble.com/")
						}
						cleanUrl.contains("odysee") || cleanUrl.contains("odycdn") -> {
							put("Referer", "https://odysee.com/")
						}
						currentStreamReferer.isNotEmpty() -> {
							put("Referer", currentStreamReferer)
						}
					}
					put("Accept", "*/*")
				}

				mp.setDataSource(this@WatchActivity, Uri.parse(cleanUrl), headers)
				@Suppress("DEPRECATION")
				mp.setAudioStreamType(AudioManager.STREAM_MUSIC)

				mp.setOnVideoSizeChangedListener { _, width, height ->
					if (width > 0 && height > 0) {
						currentVideoWidth = width
						currentVideoHeight = height
						adjustVideoAspectRatio()
					}
				}

				mp.setOnBufferingUpdateListener { mediaP, percent ->
					try {
						val duration = mediaP.duration
						if (duration > 0 && playerSeekBar != null) {
							val cacheMillis = ((duration.toLong() * percent.toLong()) / 100L).toInt()
							playerSeekBar?.secondaryProgress = cacheMillis
							playerSeekBar?.invalidate()
						}
						miniVideoProgressBar?.let { bar ->
							bar.secondaryProgress = percent * 10
							bar.invalidate()
						}
					} catch (ignored: Exception) {
					}
				}

				mp.setOnInfoListener { _, what, _ ->
					if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
						playerLoading?.visibility = View.VISIBLE
						btnPlay?.apply {
							isEnabled = false
							alpha = 0.5f
						}
						autoDowngradeRunnable?.let { cb ->
							if (isAutoQualityMode) {
								playerHandler?.removeCallbacks(cb)
								playerHandler?.postDelayed(cb, 2500L)
							}
						}
					} else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END || what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
						autoDowngradeRunnable?.let { cb -> playerHandler?.removeCallbacks(cb) }
						playerLoading?.visibility = View.GONE
						btnPlay?.apply {
							isEnabled = true
							alpha = 1.0f
						}
					}
					false
				}

				mp.setOnPreparedListener { preparedMp ->
					autoDowngradeRunnable?.let { cb -> playerHandler?.removeCallbacks(cb) }
					playerLoading?.visibility = View.GONE
					btnPlay?.apply {
						isEnabled = true
						alpha = 1.0f
						setImageResource(R.drawable.ic_pause)
					}

					val tv = playerTextureView
					if (tv != null && tv.isAvailable && playerSurface == null) {
						playerSurface = Surface(tv.surfaceTexture)
						preparedMp.setSurface(playerSurface)
					}

					val vw = preparedMp.videoWidth
					val vh = preparedMp.videoHeight
					if (vw > 0 && vh > 0) {
						currentVideoWidth = vw
						currentVideoHeight = vh
						adjustVideoAspectRatio()
					}

					val duration = preparedMp.duration
					if (duration > 0) playerSeekBar?.max = duration

					if (savedSeekPosition > 0 && savedSeekPosition < duration) {
						preparedMp.seekTo(savedSeekPosition)
						savedSeekPosition = 0
					}
					preparedMp.start()

					if (!hasRewardedWatchExp) {
						hasRewardedWatchExp = true
						addExpToCurrentUser(100L)
					}

					val targetSpeed = if (isHolding2xSpeed) 2.0f else currentPlaybackSpeed
					if (targetSpeed != 1.0f) {
						applyPlaybackSpeedValue(targetSpeed)
					}

					progressLoop?.let { loop ->
						playerHandler?.removeCallbacks(loop)
						playerHandler?.post(loop)
					}
					updateEpisodeNavButtons()
					resetControlsTimer()
				}

				mp.setOnErrorListener { _, _, _ ->
					autoDowngradeRunnable?.let { cb -> playerHandler?.removeCallbacks(cb) }
					if (cleanUrl.contains("ipfs.io/ipfs/")) {
						val altIpfs = cleanUrl.replace("https://ipfs.io/ipfs/", "https://dweb.link/ipfs/")
						playVideoUrl(altIpfs, savedSeekPosition)
						return@setOnErrorListener true
					}
					playerLoading?.visibility = View.GONE
					btnPlay?.apply {
						isEnabled = true
						alpha = 1.0f
						setImageResource(R.drawable.ic_play1)
					}
					Toast.makeText(this@WatchActivity, "Gagal memutar stream, coba server lain", Toast.LENGTH_SHORT).show()
					true
				}

				mp.prepareAsync()
			} catch (e: Exception) {
				playerLoading?.visibility = View.GONE
				btnPlay?.apply {
					isEnabled = true
					alpha = 1.0f
				}
			}
		}
	}

	fun toggleFullscreen() {
		val decorView = window.decorView
		val rootWatch = findViewById<View>(R.id.rootWatchLayout)
		val scrollContent = findViewById<View>(R.id.vscroll16)
		val playContainer = findViewById<FrameLayout>(R.id.frame_layout5)
		val density = resources.displayMetrics.density

		if (!isFullscreen) {
			requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
			rootWatch?.setPadding(0, 0, 0, 0)

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				val lp = window.attributes
				lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
				window.attributes = lp
			}

			@Suppress("DEPRECATION")
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

			@Suppress("DEPRECATION")
			decorView.systemUiVisibility =
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
					View.SYSTEM_UI_FLAG_FULLSCREEN or
					View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

			scrollContent?.visibility = View.GONE

			layoutPlayer?.let { p ->
				val lp = p.layoutParams
				lp.width = ViewGroup.LayoutParams.MATCH_PARENT
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT
				p.layoutParams = lp
			}

			textPlayerTitle?.apply {
				text = textJudul?.text?.toString() ?: ""
				textSize = 16f
				visibility = View.VISIBLE
			}

			btnLock?.visibility = View.VISIBLE

			btnFullscreenEpisodes?.apply {
				setBackgroundColor(Color.TRANSPARENT)
				visibility = View.VISIBLE
			}

			val sideMarginFull = (28 * density).toInt()
			btnPrevEpisode?.let { prev ->
				val lp = prev.layoutParams as RelativeLayout.LayoutParams
				lp.width = (38 * density).toInt()
				lp.height = (38 * density).toInt()
				lp.setMargins(sideMarginFull, 0, 0, 0)
				prev.layoutParams = lp
			}

			btnNextEpisode?.let { next ->
				val lp = next.layoutParams as RelativeLayout.LayoutParams
				lp.width = (38 * density).toInt()
				lp.height = (38 * density).toInt()
				lp.setMargins(0, 0, sideMarginFull, 0)
				next.layoutParams = lp
			}

			val gapCenterFull = (36 * density).toInt()
			btnRewind?.let { rew ->
				val lp = rew.layoutParams as LinearLayout.LayoutParams
				lp.width = (42 * density).toInt()
				lp.height = (42 * density).toInt()
				rew.layoutParams = lp
			}

			playContainer?.let { pc ->
				val lp = pc.layoutParams as LinearLayout.LayoutParams
				lp.width = (74 * density).toInt()
				lp.height = (74 * density).toInt()
				lp.setMargins(gapCenterFull, 0, gapCenterFull, 0)
				pc.layoutParams = lp
			}

			playerLoading?.let { loading ->
				val lp = loading.layoutParams
				lp.width = (74 * density).toInt()
				lp.height = (74 * density).toInt()
				loading.layoutParams = lp
			}

			btnPlay?.let { play ->
				val lp = play.layoutParams
				lp.width = (60 * density).toInt()
				lp.height = (60 * density).toInt()
				play.layoutParams = lp
				val padPlay = (15 * density).toInt()
				play.setPadding(padPlay, padPlay, padPlay, padPlay)
			}

			btnForward?.let { fwd ->
				val lp = fwd.layoutParams as LinearLayout.LayoutParams
				lp.width = (42 * density).toInt()
				lp.height = (42 * density).toInt()
				fwd.layoutParams = lp
			}

			btnBack?.let { back ->
				val lp = back.layoutParams
				lp.width = (28 * density).toInt()
				lp.height = (28 * density).toInt()
				back.layoutParams = lp
			}
			btnFullscreen?.let { fs ->
				fs.setImageResource(R.drawable.ic_fullscreen_exit)
				val lp = fs.layoutParams
				lp.width = (26 * density).toInt()
				lp.height = (26 * density).toInt()
				fs.layoutParams = lp
			}
			btnQuality?.textSize = 14f
			textTime?.textSize = 13.5f

			val padHoriz = (28 * density).toInt()
			val padTop = (18 * density).toInt()
			val padBottom = (12 * density).toInt()
			topBar?.setPadding(padHoriz, padTop, padHoriz, (6 * density).toInt())
			bottomControls?.setPadding(padHoriz, 0, padHoriz, padBottom)

			isFullscreen = true
		} else {
			requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
			rootWatch?.setPadding(0, 0, 0, 0)

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				val lp = window.attributes
				lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
				window.attributes = lp
			}

			@Suppress("DEPRECATION")
			window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
			@Suppress("DEPRECATION")
			decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

			val heightPx = (220 * density).toInt()
			layoutPlayer?.let { p ->
				val lp = p.layoutParams
				lp.width = ViewGroup.LayoutParams.MATCH_PARENT
				lp.height = heightPx
				p.layoutParams = lp
			}

			scrollContent?.visibility = View.VISIBLE
			textPlayerTitle?.visibility = View.GONE
			btnLock?.visibility = View.GONE
			btnFullscreenEpisodes?.visibility = View.GONE

			val sideMarginNormal = (12 * density).toInt()
			btnPrevEpisode?.let { prev ->
				val lp = prev.layoutParams as RelativeLayout.LayoutParams
				lp.width = (32 * density).toInt()
				lp.height = (32 * density).toInt()
				lp.setMargins(sideMarginNormal, 0, 0, 0)
				prev.layoutParams = lp
			}

			btnNextEpisode?.let { next ->
				val lp = next.layoutParams as RelativeLayout.LayoutParams
				lp.width = (32 * density).toInt()
				lp.height = (32 * density).toInt()
				lp.setMargins(0, 0, sideMarginNormal, 0)
				next.layoutParams = lp
			}

			val gapCenterNormal = (22 * density).toInt()
			btnRewind?.let { rew ->
				val lp = rew.layoutParams as LinearLayout.LayoutParams
				lp.width = (36 * density).toInt()
				lp.height = (36 * density).toInt()
				rew.layoutParams = lp
			}

			playContainer?.let { pc ->
				val lp = pc.layoutParams as LinearLayout.LayoutParams
				lp.width = (64 * density).toInt()
				lp.height = (64 * density).toInt()
				lp.setMargins(gapCenterNormal, 0, gapCenterNormal, 0)
				pc.layoutParams = lp
			}

			playerLoading?.let { loading ->
				val lp = loading.layoutParams
				lp.width = (64 * density).toInt()
				lp.height = (64 * density).toInt()
				loading.layoutParams = lp
			}

			btnPlay?.let { play ->
				val lp = play.layoutParams
				lp.width = (54 * density).toInt()
				lp.height = (54 * density).toInt()
				play.layoutParams = lp
				val padPlay = (14 * density).toInt()
				play.setPadding(padPlay, padPlay, padPlay, padPlay)
			}

			btnForward?.let { fwd ->
				val lp = fwd.layoutParams as LinearLayout.LayoutParams
				lp.width = (36 * density).toInt()
				lp.height = (36 * density).toInt()
				fwd.layoutParams = lp
			}

			btnBack?.let { back ->
				val lp = back.layoutParams
				lp.width = (28 * density).toInt()
				lp.height = (28 * density).toInt()
				back.layoutParams = lp
			}
			btnFullscreen?.let { fs ->
				fs.setImageResource(R.drawable.ic_fullscreen)
				val lp = fs.layoutParams
				lp.width = (24 * density).toInt()
				lp.height = (24 * density).toInt()
				fs.layoutParams = lp
			}
			btnQuality?.textSize = 13f
			textTime?.textSize = 13f

			val defPadX = (16 * density).toInt()
			val defPadTop = (14 * density).toInt()
			val defPadBottom = (6 * density).toInt()
			topBar?.setPadding(defPadX, defPadTop, defPadX, (6 * density).toInt())
			bottomControls?.setPadding(0, 0, 0, defPadBottom)

			isFullscreen = false
		}
		updateBottomDividerState()
		adjustVideoAspectRatio()
		resetControlsTimer()
	}

	fun _initPlayerViews() {
		val isUserTrackingSeekBar = booleanArrayOf(false)
		playerHandler = Handler(Looper.getMainLooper())

		autoDowngradeRunnable = Runnable { triggerAutoDowngrade() }
		hideUnlockBtnRunnable = Runnable { btnUnlockScreen?.visibility = View.GONE }

		btnPlay?.background = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(0xFFFFFFFF.toInt())
		}

		val density = resources.displayMetrics.density
		val barHeight = (4 * density).toInt()
		val cornerRadius = (2 * density)
		val gapSize = (4 * density)

		val customTrackDrawable = object : Drawable() {
			private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF.toInt() }
			private val paintCache = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x80CCCCCC.toInt() }
			private val paintProgress = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3B82F6.toInt() }

			override fun draw(canvas: Canvas) {
				val seekBar = playerSeekBar ?: return
				val b = bounds
				val width = b.width().toFloat()
				val top = b.centerY() - (barHeight / 2f)
				val bottom = b.centerY() + (barHeight / 2f)

				val max = if (seekBar.max <= 0) 1 else seekBar.max
				val progressRatio = seekBar.progress.toFloat() / max.toFloat()
				val cacheRatio = seekBar.secondaryProgress.toFloat() / max.toFloat()

				val progressRight = b.left + (width * progressRatio)
				val cacheRight = b.left + (width * cacheRatio)

				if (cacheRight > b.left) {
					canvas.drawRoundRect(RectF(b.left.toFloat(), top, cacheRight, bottom), cornerRadius, cornerRadius, paintCache)
				}
				if (progressRight > b.left) {
					canvas.drawRoundRect(RectF(b.left.toFloat(), top, progressRight, bottom), cornerRadius, cornerRadius, paintProgress)
				}
				val bgStart = maxOf(cacheRight, progressRight) + gapSize
				if (bgStart < b.right) {
					canvas.drawRoundRect(RectF(bgStart, top, b.right.toFloat(), bottom), cornerRadius, cornerRadius, paintBg)
				}
			}

			override fun setAlpha(alpha: Int) {}
			override fun setColorFilter(colorFilter: ColorFilter?) {}
			@Deprecated("Deprecated in Java")
			override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
		}

		val miniTrackDrawable = object : Drawable() {
			private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF.toInt() }
			private val paintCache = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x80CCCCCC.toInt() }
			private val paintProgress = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3B82F6.toInt() }

			override fun draw(canvas: Canvas) {
				val miniBar = miniVideoProgressBar ?: return
				val b = bounds
				val width = b.width().toFloat()
				val top = b.top.toFloat()
				val bottom = b.bottom.toFloat()

				val max = if (miniBar.max <= 0) 1000 else miniBar.max
				val progressRatio = miniBar.progress.toFloat() / max.toFloat()
				val cacheRatio = miniBar.secondaryProgress.toFloat() / max.toFloat()

				val progressRight = b.left + (width * progressRatio)
				val cacheRight = b.left + (width * cacheRatio)

				if (cacheRight > b.left) {
					canvas.drawRect(b.left.toFloat(), top, cacheRight, bottom, paintCache)
				}
				if (progressRight > b.left) {
					canvas.drawRect(b.left.toFloat(), top, progressRight, bottom, paintProgress)
				}
				val bgStart = maxOf(cacheRight, progressRight) + gapSize
				if (bgStart < b.right) {
					canvas.drawRect(bgStart, top, b.right.toFloat(), bottom, paintBg)
				}
			}

			override fun setAlpha(alpha: Int) {}
			override fun setColorFilter(colorFilter: ColorFilter?) {}
			@Deprecated("Deprecated in Java")
			override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
		}

		miniVideoProgressBar?.progressDrawable = miniTrackDrawable

		playerSeekBar?.let { seekBar ->
			seekBar.progressDrawable = customTrackDrawable

			val normalThumbSize = (12 * density).toInt()
			val activeThumbSize = (16 * density).toInt()

			val customThumb = GradientDrawable().apply {
				shape = GradientDrawable.OVAL
				setColor(0xFF3B82F6.toInt())
				setSize(normalThumbSize, normalThumbSize)
			}

			seekBar.thumb = customThumb
			seekBar.thumbOffset = normalThumbSize / 2

			seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
					sb.invalidate()
					if (fromUser && textTime != null) {
						val curSec = (progress / 1000) % 60
						val curMin = (progress / (1000 * 60)) % 60
						val total = mediaPlayer?.duration ?: 0
						val totSec = (total / 1000) % 60
						val totMin = (total / (1000 * 60)) % 60
						textTime?.text = String.format(Locale.US, "%02d:%02d / %02d:%02d", curMin, curSec, totMin, totSec)
					}
				}

				override fun onStartTrackingTouch(sb: SeekBar) {
					isUserTrackingSeekBar[0] = true
					customThumb.setSize(activeThumbSize, activeThumbSize)
					sb.thumb = customThumb
					sb.thumbOffset = activeThumbSize / 2
					hideControlsRunnable?.let { cb -> playerHandler?.removeCallbacks(cb) }
				}

				override fun onStopTrackingTouch(sb: SeekBar) {
					mediaPlayer?.let { mp ->
						mp.seekTo(sb.progress)
						saveWatchHistoryToFirebase(sb.progress, mp.duration, true)
					}
					customThumb.setSize(normalThumbSize, normalThumbSize)
					sb.thumb = customThumb
					sb.thumbOffset = normalThumbSize / 2

					isUserTrackingSeekBar[0] = false
					resetControlsTimer()
				}
			})
		}

		hideControlsRunnable = Runnable { hideControls() }

		val playerGestureDetector = GestureDetector(this@WatchActivity, object : GestureDetector.SimpleOnGestureListener() {
			override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
				toggleControls()
				return true
			}

			override fun onLongPress(e: MotionEvent) {
				startHold2xSpeed()
			}

			override fun onDoubleTap(e: MotionEvent): Boolean {
				if (isScreenLocked) {
					showUnlockPillTemporarily()
					return true
				}
				val mp = mediaPlayer ?: return false
				val container = layoutPlayer ?: return false

				val touchX = e.x
				val viewWidth = container.width

				if (touchX < (viewWidth / 2f)) {
					val targetPos = maxOf(0, mp.currentPosition - 10000)
					mp.seekTo(targetPos)
					playerSeekBar?.let { sb ->
						sb.progress = targetPos
						sb.invalidate()
					}
					showSeekAnimation(animSeekLeft)
				} else {
					val targetPos = minOf(mp.duration, mp.currentPosition + 10000)
					mp.seekTo(targetPos)
					playerSeekBar?.let { sb ->
						sb.progress = targetPos
						sb.invalidate()
					}
					showSeekAnimation(animSeekRight)
				}
				resetControlsTimer()
				return true
			}

			override fun onDown(e: MotionEvent): Boolean = true
		})

		val playerTouchListener = View.OnTouchListener { _, event ->
			if (event.action == MotionEvent.ACTION_DOWN) {
				resetControlsTimer()
			}
			if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
				if (isHolding2xSpeed) {
					stopHold2xSpeed()
					return@OnTouchListener true
				}
			}
			playerGestureDetector.onTouchEvent(event)
		}

		layoutPlayer?.setOnTouchListener(playerTouchListener)
		playerControlsOverlay?.setOnTouchListener(playerTouchListener)

		playerTextureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
			override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
				playerSurface = Surface(surfaceTexture)
				mediaPlayer?.let { mp ->
					try {
						mp.setSurface(playerSurface)
					} catch (ignored: Exception) {
					}
				}
				adjustVideoAspectRatio()
			}

			override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

			override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
				playerSurface?.release()
				playerSurface = null
				if (mediaPlayer != null && !DongflixPlayerManager.getInstance().isMiniPlayerActive) {
					mediaPlayer?.release()
					mediaPlayer = null
				}
				return true
			}

			override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
		}

		progressLoop = object : Runnable {
			override fun run() {
				try {
					val mp = mediaPlayer
					if (mp != null && mp.isPlaying && !isUserTrackingSeekBar[0]) {
						val cur = mp.currentPosition
						val tot = mp.duration

						playerSeekBar?.let { sb ->
							sb.progress = cur
							sb.invalidate()
						}

						if (miniVideoProgressBar != null && tot > 0) {
							val permille = ((cur.toLong() * 1000L) / tot.toLong()).toInt()
							miniVideoProgressBar?.progress = permille
							miniVideoProgressBar?.invalidate()
						}

						if (textTime != null) {
							val curSec = (cur / 1000) % 60
							val curMin = (cur / (1000 * 60)) % 60
							val totSec = (tot / 1000) % 60
							val totMin = (tot / (1000 * 60)) % 60
							textTime?.text = String.format(Locale.US, "%02d:%02d / %02d:%02d", curMin, curSec, totMin, totSec)
						}

						saveWatchHistoryToFirebase(cur, tot, false)
					}
				} catch (ignored: Exception) {
				}

				playerHandler?.postDelayed(this, 500L)
			}
		}

		btnPlay?.setOnClickListener {
			resetControlsTimer()
			val mp = mediaPlayer ?: return@setOnClickListener
			if (mp.isPlaying) {
				mp.pause()
				btnPlay?.setImageResource(R.drawable.ic_play1)
				window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
				saveWatchHistoryToFirebase(mp.currentPosition, mp.duration, true)
			} else {
				mp.start()
				btnPlay?.setImageResource(R.drawable.ic_pause)
				window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			}
		}

		btnRewind?.setOnClickListener {
			resetControlsTimer()
			val mp = mediaPlayer ?: return@setOnClickListener
			val targetPos = maxOf(0, mp.currentPosition - 10000)
			mp.seekTo(targetPos)
			playerSeekBar?.let { sb ->
				sb.progress = targetPos
				sb.invalidate()
			}
			showSeekAnimation(animSeekLeft)
		}

		btnForward?.setOnClickListener {
			resetControlsTimer()
			val mp = mediaPlayer ?: return@setOnClickListener
			val targetPos = minOf(mp.duration, mp.currentPosition + 10000)
			mp.seekTo(targetPos)
			playerSeekBar?.let { sb ->
				sb.progress = targetPos
				sb.invalidate()
			}
			showSeekAnimation(animSeekRight)
		}

		btnPrevEpisode?.setOnClickListener {
			resetControlsTimer()
			if (currentEpisodeIndex > 0) {
				playEpisodeByIndex(currentEpisodeIndex - 1, 0)
			}
		}

		btnNextEpisode?.setOnClickListener {
			resetControlsTimer()
			val totalItems = recyclerEpisode?.adapter?.itemCount ?: 0
			if (currentEpisodeIndex < (totalItems - 1)) {
				playEpisodeByIndex(currentEpisodeIndex + 1, 0)
			}
		}

		btnLock?.setOnClickListener { lockScreenControls() }
		btnUnlockScreen?.setOnClickListener { unlockScreenControls() }

		btnCast?.setOnClickListener {
			resetControlsTimer()
			try {
				startActivity(Intent(Settings.ACTION_CAST_SETTINGS))
			} catch (e: Exception) {
				Toast.makeText(this@WatchActivity, "Fitur Cast tidak tersedia di perangkat ini", Toast.LENGTH_SHORT).show()
			}
		}

		btnSettings?.setOnClickListener {
			resetControlsTimer()
			showSettingsBottomSheet()
		}

		btnQuality?.setOnClickListener {
			resetControlsTimer()
			showQualityDialog()
		}

		btnFullscreenEpisodes?.setOnClickListener {
			resetControlsTimer()
			showAllEpisodesBottomSheet()
		}

		btnOpenDetail?.setOnClickListener { showDetailBottomSheet() }
		btnSortEpisodes?.setOnClickListener { toggleSortEpisodes() }

		findViewById<TextView>(R.id.textview85)?.apply {
			setTextColor(Color.parseColor("#94A3B8"))
			setOnClickListener { showAllEpisodesBottomSheet() }
		}

		btnFill?.setOnClickListener {
			resetControlsTimer()
			isFillModeActive = !isFillModeActive
			adjustVideoAspectRatio()
			Toast.makeText(this@WatchActivity, if (isFillModeActive) "Mode: Fill" else "Mode: Original", Toast.LENGTH_SHORT).show()
		}

		btnActionServer?.setOnClickListener { showServerDialog() }

		btnFullscreen?.setOnClickListener {
			resetControlsTimer()
			toggleFullscreen()
		}

		btnBack?.setOnClickListener {
			if (isFullscreen) {
				toggleFullscreen()
			} else {
				minimizeToMiniPlayer()
			}
		}

		updateEpisodeNavButtons()
	}

	@Throws(Exception::class)
	private fun fetchHttpString(targetUrl: String, referer: String?, saveDmCookie: Boolean): String {
		var currentUrl = targetUrl
		var conn: HttpURLConnection? = null

		for (redirectCount in 0 until 6) {
			val url = URL(currentUrl)
			conn = (url.openConnection() as HttpURLConnection).apply {
				requestMethod = "GET"
				connectTimeout = 10000
				readTimeout = 10000
				instanceFollowRedirects = false
				setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
				setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8")
				setRequestProperty("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
				setRequestProperty("Sec-Fetch-Dest", "iframe")
				setRequestProperty("Sec-Fetch-Mode", "navigate")
				if (!referer.isNullOrEmpty()) {
					setRequestProperty("Referer", referer)
				}
			}

			if (saveDmCookie) {
				val cookiesHeader = conn.headerFields["Set-Cookie"]
				if (!cookiesHeader.isNullOrEmpty()) {
					val cookieSb = StringBuilder()
					for (c in cookiesHeader) {
						val mainPart = c.split(";")[0]
						if (cookieSb.isNotEmpty()) cookieSb.append("; ")
						cookieSb.append(mainPart)
					}
					dmSessionCookie = cookieSb.toString()
				}
			}

			val code = conn.responseCode
			if (code == HttpURLConnection.HTTP_MOVED_TEMP ||
				code == HttpURLConnection.HTTP_MOVED_PERM ||
				code == HttpURLConnection.HTTP_SEE_OTHER ||
				code == 307 || code == 308
			) {
				var loc = conn.getHeaderField("Location")
				conn.disconnect()
				if (loc.isNullOrEmpty()) break
				if (loc.startsWith("//")) loc = "https:$loc"
				else if (loc.startsWith("/")) loc = "${url.protocol}://${url.host}$loc"
				currentUrl = loc
				continue
			}
			break
		}

		val activeConn = conn ?: return ""
		val reader = BufferedReader(InputStreamReader(activeConn.inputStream, "UTF-8"))
		val sb = StringBuilder()
		var line: String? = reader.readLine()
		while (line != null) {
			sb.append(line).append("\n")
			line = reader.readLine()
		}
		reader.close()
		activeConn.disconnect()
		return sb.toString()
	}

	@Throws(Exception::class)
	private fun postJsonHttp(apiUrl: String, jsonPayload: String): String {
		val url = URL(apiUrl)
		val conn = (url.openConnection() as HttpURLConnection).apply {
			requestMethod = "POST"
			connectTimeout = 10000
			readTimeout = 10000
			doOutput = true
			setRequestProperty("Content-Type", "application/json")
			setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0 Safari/537.36")
		}

		val os: OutputStream = conn.outputStream
		os.write(jsonPayload.toByteArray(Charsets.UTF_8))
		os.flush()
		os.close()

		val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
		val sb = StringBuilder()
		var line: String? = reader.readLine()
		while (line != null) {
			sb.append(line)
			line = reader.readLine()
		}
		reader.close()
		conn.disconnect()
		return sb.toString()
	}

	private fun unpackDeanEdwards(html: String): String {
		try {
			val p = Pattern.compile(
				"eval\\(function\\(p,a,c,k,e,[dr]\\)\\{.*?\\}\\('(.*?)',(\\d+),(\\d+),'(.*?)'\\.split\\('\\|'\\)",
				Pattern.DOTALL
			)
			val m = p.matcher(html)
			if (m.find()) {
				val payload = m.group(1) ?: ""
				val radix = m.group(2)?.toInt() ?: 10
				val symtab = (m.group(4) ?: "").split("\\|".toRegex()).toTypedArray()

				val wordPat = Pattern.compile("\\b\\w+\\b")
				val wm = wordPat.matcher(payload)
				val sb = StringBuffer()
				while (wm.find()) {
					val word = wm.group() ?: continue
					try {
						val idx = word.toInt(radix)
						if (idx in symtab.indices && symtab[idx].isNotEmpty()) {
							wm.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(symtab[idx]))
						}
					} catch (ignored: Exception) {
					}
				}
				wm.appendTail(sb)
				return sb.toString()
			}
		} catch (ignored: Exception) {
		}
		return ""
	}

	fun extractStream(server: String?, targetUrl: String?) {
		extractStream(server, targetUrl, 0)
	}

	fun extractStream(server: String?, targetUrl: String?, seekOnReadyMs: Int) {
		if (targetUrl.isNullOrBlank()) return

		var cleanUrl = targetUrl.trim()
		if (cleanUrl.startsWith("//")) {
			cleanUrl = "https:$cleanUrl"
		} else if (!cleanUrl.startsWith("http")) {
			cleanUrl = "https://$cleanUrl"
		}
		val finalTargetUrl = cleanUrl
		val serverKey = server?.trim()?.lowercase(Locale.getDefault()) ?: ""
		val lowerUrl = finalTargetUrl.lowercase(Locale.getDefault())
		currentStreamReferer = finalTargetUrl

		when {
			serverKey.contains("ok") || serverKey.contains("odnoklassniki") || lowerUrl.contains("ok.ru") -> {
				Thread {
					try {
						val html = fetchHttpString(finalTargetUrl, "https://ok.ru/", false)
						var foundOptions = ""
						val idx = html.indexOf("data-options=")
						if (idx != -1) {
							val qStart = idx + 13
							val quote = html[qStart]
							val qEnd = html.indexOf(quote, qStart + 1)
							if (qEnd != -1) {
								foundOptions = html.substring(qStart + 1, qEnd)
							}
						}

						val tempNames = ArrayList<String>()
						val tempUrls = ArrayList<String>()

						if (foundOptions.isNotEmpty()) {
							val cleanJson = foundOptions
								.replace("&quot;", "\"")
								.replace("&amp;", "&")
								.replace("&#39;", "'")
								.replace("&lt;", "<")
								.replace("&gt;", ">")

							val optJson = JSONObject(cleanJson)
							var metadata: JSONObject? = null
							if (optJson.has("flashvars")) {
								val fv = optJson.getJSONObject("flashvars")
								if (fv.has("metadata")) {
									val mObj = fv.get("metadata")
									metadata = if (mObj is JSONObject) mObj else JSONObject(mObj.toString())
								}
							} else if (optJson.has("metadata")) {
								val mObj = optJson.get("metadata")
								metadata = if (mObj is JSONObject) mObj else JSONObject(mObj.toString())
							}

							if (metadata != null && metadata.has("videos")) {
								val videos = metadata.getJSONArray("videos")
								for (k in 0 until videos.length()) {
									val v = videos.getJSONObject(k)
									if (v.has("url")) {
										val rawU = v.getString("url").replace("\\/", "/").replace("\\u0026", "&")
										var rName = v.optString("name", "Resolusi ${k + 1}").uppercase(Locale.getDefault())

										when (rName.lowercase(Locale.getDefault())) {
											"mobile" -> rName = "144P"
											"lowest" -> rName = "240P"
											"low" -> rName = "360P"
											"medium" -> rName = "480P"
											"high" -> rName = "720P"
											"hd" -> rName = "1080P"
										}

										tempNames.add(rName)
										tempUrls.add(rawU)
									}
								}
							}
						}

						Handler(Looper.getMainLooper()).post {
							if (tempUrls.isNotEmpty()) {
								qualityNames.clear()
								qualityUrls.clear()
								qualityNames.addAll(tempNames)
								qualityUrls.addAll(tempUrls)

								isAutoQualityMode = true
								selectedQualityIndex = getRecommendedQualityIndex()
								updateQualityBadgeText()

								playVideoUrl(qualityUrls[selectedQualityIndex], seekOnReadyMs)
							} else {
								playViaWebViewInterceptor(finalTargetUrl)
							}
						}
					} catch (e: Exception) {
						playViaWebViewInterceptor(finalTargetUrl)
					}
				}.start()
			}

			serverKey.contains("daily") || lowerUrl.contains("dailymotion") || lowerUrl.contains("dai.ly") -> {
				Thread {
					try {
						var dailyId = ""
						val parsedUri = Uri.parse(finalTargetUrl)
						val qId = parsedUri.getQueryParameter("id")
						val qVideo = parsedUri.getQueryParameter("video")
						when {
							!qId.isNullOrEmpty() -> dailyId = qId
							!qVideo.isNullOrEmpty() -> dailyId = qVideo
							finalTargetUrl.contains("/video/") -> {
								val parts = finalTargetUrl.split("/video/".toRegex())
								if (parts.size > 1) dailyId = parts[1].split("[?&#_]".toRegex())[0]
							}
							finalTargetUrl.contains("dai.ly/") -> {
								val parts = finalTargetUrl.split("dai.ly/".toRegex())
								if (parts.size > 1) dailyId = parts[1].split("[?&#]".toRegex())[0]
							}
							else -> {
								val cPath = finalTargetUrl.split("\\?".toRegex())[0]
								dailyId = cPath.substring(cPath.lastIndexOf('/') + 1)
							}
						}

						if (dailyId.trim().isEmpty()) throw Exception("ID Dailymotion kosong")

						val cleanDmId = dailyId.trim()
						val dmApiUrl = "https://www.dailymotion.com/player/metadata/video/$cleanDmId?embedder=https%3A%2F%2Fwww.dailymotion.com%2F&app=com.dailymotion.neon"
						val resBody = fetchHttpString(dmApiUrl, "https://www.dailymotion.com/", true)

						val dmNames = ArrayList<String>()
						val dmUrls = ArrayList<String>()

						if (resBody.isNotEmpty()) {
							val metaObj = JSONObject(resBody)
							if (metaObj.has("qualities")) {
								val qualities = metaObj.getJSONObject("qualities")
								val checkKeys = arrayOf("1080", "720", "480", "380", "240", "auto")

								for (qKey in checkKeys) {
									if (qualities.has(qKey)) {
										val qArr = qualities.optJSONArray(qKey)
										if (qArr != null) {
											var chosenU = ""
											for (idx in 0 until qArr.length()) {
												val itemQ = qArr.getJSONObject(idx)
												val mime = itemQ.optString("type", "")
												val u = itemQ.optString("url", "")
												if (u.isNotEmpty() && mime.contains("mp4")) {
													chosenU = u
													break
												}
											}
											if (chosenU.isEmpty() && qArr.length() > 0) {
												chosenU = qArr.getJSONObject(0).optString("url", "")
											}
											if (chosenU.isNotEmpty()) {
												val label = if (qKey.equals("auto", ignoreCase = true)) "Auto" else "${qKey}P"
												dmNames.add(label)
												dmUrls.add(chosenU)
											}
										}
									}
								}
							}
						}

						Handler(Looper.getMainLooper()).post {
							if (dmUrls.isNotEmpty()) {
								qualityNames.clear()
								qualityUrls.clear()
								qualityNames.addAll(dmNames)
								qualityUrls.addAll(dmUrls)

								isAutoQualityMode = true
								selectedQualityIndex = getRecommendedQualityIndex()
								updateQualityBadgeText()

								playVideoUrl(qualityUrls[selectedQualityIndex], seekOnReadyMs)
							} else {
								playViaWebViewInterceptor("https://geo.dailymotion.com/player.html?video=$cleanDmId")
							}
						}
					} catch (e: Exception) {
						playViaWebViewInterceptor(finalTargetUrl)
					}
				}.start()
			}

			serverKey.contains("d-tube") || serverKey.contains("dtube") || lowerUrl.contains("d.tube") -> {
				Thread {
					try {
						var streamFound = ""
						val pDirectQm = Pattern.compile("(Qm[a-zA-Z0-9]{44})")
						val mDirectQm = pDirectQm.matcher(finalTargetUrl)
						if (mDirectQm.find()) {
							streamFound = "https://dweb.link/ipfs/${mDirectQm.group(1)}"
						}

						if (streamFound.isEmpty() && finalTargetUrl.contains("/v/")) {
							val afterV = finalTargetUrl.substring(finalTargetUrl.indexOf("/v/") + 3).split("[?#]".toRegex())[0]
							val parts = afterV.split("/".toRegex())
							if (parts.size >= 2) {
								val author = parts[0].trim()
								val permlink = parts[1].trim()
								val rpcBody = "{\"jsonrpc\":\"2.0\",\"method\":\"condenser_api.get_content\",\"params\":[\"$author\",\"$permlink\"],\"id\":1}"
								val rpcRes = postJsonHttp("https://api.hive.blog", rpcBody)
								val rpcJson = JSONObject(rpcRes)
								if (rpcJson.has("result")) {
									val resObj = rpcJson.getJSONObject("result")
									val metaStr = resObj.optString("json_metadata", "")
									if (metaStr.isNotEmpty()) {
										val metaJson = JSONObject(metaStr)
										val hash = metaJson.optJSONObject("video")
											?.optJSONObject("files")
											?.optJSONObject("ipfs")
											?.optJSONObject("vid")
											?.let { vidObj -> vidObj.optString("720", vidObj.optString("480", vidObj.optString("src", ""))) }
											?: ""
										if (hash.isNotEmpty()) {
											streamFound = "https://dweb.link/ipfs/$hash"
										}
									}
								}
							}
						}

						if (streamFound.isEmpty()) {
							val html = fetchHttpString(finalTargetUrl, "https://emb.d.tube/", false)
							val mQm = pDirectQm.matcher(html)
							if (mQm.find()) {
								streamFound = "https://dweb.link/ipfs/${mQm.group(1)}"
							}
						}

						val finalDtubeUrl = streamFound
						Handler(Looper.getMainLooper()).post {
							if (finalDtubeUrl.isNotEmpty()) {
								qualityNames.clear()
								qualityUrls.clear()
								qualityNames.add("Default")
								qualityUrls.add(finalDtubeUrl)
								isAutoQualityMode = true
								selectedQualityIndex = 0
								updateQualityBadgeText()

								playVideoUrl(finalDtubeUrl, seekOnReadyMs)
							} else {
								playViaWebViewInterceptor(finalTargetUrl)
							}
						}
					} catch (e: Exception) {
						playViaWebViewInterceptor(finalTargetUrl)
					}
				}.start()
			}

			serverKey.contains("rumble") || lowerUrl.contains("rumble.com") -> {
				Thread {
					try {
						var embedId = ""
						if (finalTargetUrl.contains("/embed/")) {
							val parts = finalTargetUrl.split("/embed/".toRegex())
							if (parts.size > 1) embedId = parts[1].split("[/?#]".toRegex())[0]
						}

						val rNames = ArrayList<String>()
						val rUrls = ArrayList<String>()

						if (embedId.isEmpty()) {
							val pageHtml = fetchHttpString(finalTargetUrl, "https://rumble.com/", false)
							val pEmbed = Pattern.compile("rumble\\.com/embed/([a-zA-Z0-9]+)")
							val mEmbed = pEmbed.matcher(pageHtml)
							if (mEmbed.find()) {
								embedId = mEmbed.group(1) ?: ""
							}
						}

						if (embedId.isNotEmpty()) {
							val apiUrl = "https://rumble.com/embedJS/u3/?request=video&ver=2&v=$embedId"
							val jsonStr = fetchHttpString(apiUrl, "https://rumble.com/", false)
							val rootObj = JSONObject(jsonStr)

							if (rootObj.has("ua")) {
								val uaObj = rootObj.getJSONObject("ua")
								val resKeys = arrayOf("1080", "720", "480", "360", "240")

								if (uaObj.has("mp4")) {
									val mp4Obj = uaObj.getJSONObject("mp4")
									for (rk in resKeys) {
										if (mp4Obj.has(rk)) {
											val u = mp4Obj.getJSONObject(rk).optString("url")
											if (u.isNotEmpty()) {
												rNames.add("${rk}P")
												rUrls.add(u)
											}
										}
									}
								}

								if (rUrls.isEmpty()) {
									for (rk in resKeys) {
										if (uaObj.has(rk)) {
											val resObj = uaObj.optJSONObject(rk)
											val u = resObj?.optString("url") ?: ""
											if (u.isNotEmpty()) {
												rNames.add("${rk}P")
												rUrls.add(u)
											}
										}
									}
								}

								if (rUrls.isEmpty() && uaObj.has("hls")) {
									val hlsObj = uaObj.getJSONObject("hls")
									if (hlsObj.has("auto")) {
										val u = hlsObj.getJSONObject("auto").optString("url")
										if (u.isNotEmpty()) {
											rNames.add("Auto")
											rUrls.add(u)
										}
									}
								}
							}
						}

						if (rUrls.isEmpty()) {
							val html = fetchHttpString(finalTargetUrl, "https://rumble.com/", false)
							val pMp4 = Pattern.compile("(https?://[a-zA-Z0-9.-]+\\.rumble\\.com/[^\"'\\s]+\\.mp4)")
							val mMp4 = pMp4.matcher(html)
							if (mMp4.find()) {
								rNames.add("Default")
								rUrls.add(mMp4.group(1) ?: "")
							}
						}

						Handler(Looper.getMainLooper()).post {
							if (rUrls.isNotEmpty()) {
								qualityNames.clear()
								qualityUrls.clear()
								qualityNames.addAll(rNames)
								qualityUrls.addAll(rUrls)

								isAutoQualityMode = true
								selectedQualityIndex = getRecommendedQualityIndex()
								updateQualityBadgeText()

								playVideoUrl(qualityUrls[selectedQualityIndex], seekOnReadyMs)
							} else {
								playViaWebViewInterceptor(finalTargetUrl)
							}
						}
					} catch (e: Exception) {
						playViaWebViewInterceptor(finalTargetUrl)
					}
				}.start()
			}

			serverKey.contains("odysee") || lowerUrl.contains("odysee.com") || lowerUrl.contains("odycdn.com") -> {
				Thread {
					try {
						var streamOdysee = ""
						if (finalTargetUrl.contains("odycdn.com") &&
							(finalTargetUrl.contains(".mp4") || finalTargetUrl.contains(".m3u8") || finalTargetUrl.contains("/streams/"))
						) {
							streamOdysee = finalTargetUrl
						} else if (finalTargetUrl.contains("signature=")) {
							val uri = Uri.parse(finalTargetUrl)
							val sig = uri.getQueryParameter("signature")
							val sigTs = uri.getQueryParameter("signature_ts")
							val path = uri.path
							if (path != null && path.contains("/embed/")) {
								val cleanClaim = path.replace("/$/embed/", "").replace("/embed/", "")
								streamOdysee = "https://player.odycdn.com/api/v4/streams/free/$cleanClaim/$sig/$sigTs"
							}
						}

						if (streamOdysee.isEmpty()) {
							val html = fetchHttpString(finalTargetUrl, "https://odysee.com/", false)
							val pOdy = Pattern.compile("(https?://player\\.odycdn\\.com/api/v4/streams/[^\"'\\s]+)")
							val mOdy = pOdy.matcher(html)
							if (mOdy.find()) {
								streamOdysee = (mOdy.group(1) ?: "").replace("\\/", "/")
							} else {
								val pMedia = Pattern.compile("(https?://[^\"'\\s]+\\.(?:mp4|m3u8)[^\"'\\s]*)")
								val mMedia = pMedia.matcher(html)
								if (mMedia.find()) {
									streamOdysee = (mMedia.group(1) ?: "").replace("\\/", "/")
								}
							}
						}

						if (streamOdysee.isEmpty() && finalTargetUrl.contains("/embed/")) {
							val afterEmbed = finalTargetUrl.substring(finalTargetUrl.indexOf("/embed/") + 7).split("[?#]".toRegex())[0]
							val lbryUri = "lbry://${afterEmbed.replace("/", "#")}"
							val lbryReq = "{\"method\":\"get\",\"params\":{\"uri\":\"$lbryUri\"}}"
							val lbryRes = postJsonHttp("https://api.na-backend.odysee.com/api/v1/proxy", lbryReq)
							val lbryJson = JSONObject(lbryRes)
							if (lbryJson.has("result")) {
								streamOdysee = lbryJson.getJSONObject("result").optString("streaming_url", "")
							}
						}

						val finalOdyUrl = streamOdysee
						Handler(Looper.getMainLooper()).post {
							if (finalOdyUrl.isNotEmpty()) {
								qualityNames.clear()
								qualityUrls.clear()
								qualityNames.add("Default")
								qualityUrls.add(finalOdyUrl)
								isAutoQualityMode = true
								selectedQualityIndex = 0
								updateQualityBadgeText()

								playVideoUrl(finalOdyUrl, seekOnReadyMs)
							} else {
								playViaWebViewInterceptor(finalTargetUrl)
							}
						}
					} catch (e: Exception) {
						playViaWebViewInterceptor(finalTargetUrl)
					}
				}.start()
			}

			else -> {
				Thread {
					try {
						if (lowerUrl.contains(".mp4") || lowerUrl.contains(".m3u8")) {
							Handler(Looper.getMainLooper()).post {
								qualityNames.clear()
								qualityUrls.clear()
								qualityNames.add("Default")
								qualityUrls.add(finalTargetUrl)
								isAutoQualityMode = true
								selectedQualityIndex = 0
								updateQualityBadgeText()

								playVideoUrl(finalTargetUrl, seekOnReadyMs)
							}
							return@Thread
						}

						val html = fetchHttpString(finalTargetUrl, finalTargetUrl, false)
						val unpacked = unpackDeanEdwards(html)
						val combinedSource = "$html\n$unpacked"
						var streamFound = ""

						val pSource = Pattern.compile("<(?:source|video)[^>]+src=[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
						val mSource = pSource.matcher(combinedSource)
						if (mSource.find()) {
							streamFound = (mSource.group(1) ?: "").replace("&amp;", "&")
						}

						if (streamFound.isEmpty()) {
							val pMedia = Pattern.compile("(https?:\\\\?/\\\\?/[^\"'\\s<>]+\\.(?:m3u8|mp4)[^\"'\\s<>]*)", Pattern.CASE_INSENSITIVE)
							val mMedia = pMedia.matcher(combinedSource)
							if (mMedia.find()) {
								streamFound = (mMedia.group(1) ?: "").replace("\\/", "/").replace("\\u0026", "&")
							}
						}

						if (streamFound.isEmpty()) {
							val pIframe = Pattern.compile("<iframe[^>]+src=[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
							val mIframe = pIframe.matcher(combinedSource)
							if (mIframe.find()) {
								val innerUrl = (mIframe.group(1) ?: "").replace("&amp;", "&")
								currentStreamReferer = innerUrl
								val innerHtml = fetchHttpString(innerUrl, finalTargetUrl, false)
								val innerUnpacked = unpackDeanEdwards(innerHtml)
								val innerCombined = "$innerHtml\n$innerUnpacked"
								val pInnerMedia = Pattern.compile("(https?:\\\\?/\\\\?/[^\"'\\s<>]+\\.(?:m3u8|mp4)[^\"'\\s<>]*)", Pattern.CASE_INSENSITIVE)
								val mInnerMedia = pInnerMedia.matcher(innerCombined)
								if (mInnerMedia.find()) {
									streamFound = (mInnerMedia.group(1) ?: "").replace("\\/", "/").replace("\\u0026", "&")
								}
							}
						}

						val finalGenericUrl = streamFound
						Handler(Looper.getMainLooper()).post {
							if (finalGenericUrl.isNotEmpty()) {
								qualityNames.clear()
								qualityUrls.clear()
								qualityNames.add("Default")
								qualityUrls.add(finalGenericUrl)
								isAutoQualityMode = true
								selectedQualityIndex = 0
								updateQualityBadgeText()

								playVideoUrl(finalGenericUrl, seekOnReadyMs)
							} else {
								playViaWebViewInterceptor(finalTargetUrl)
							}
						}
					} catch (e: Exception) {
						playViaWebViewInterceptor(finalTargetUrl)
					}
				}.start()
			}
		}
	}

	inner class ShimmerEpisodeAdapter(private val count: Int) : RecyclerView.Adapter<ShimmerEpisodeAdapter.ViewHolder>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val d = parent.context.resources.displayMetrics.density
			val v = View(parent.context).apply {
				layoutParams = RecyclerView.LayoutParams((65 * d).toInt(), (60 * d).toInt()).apply {
					marginEnd = (8 * d).toInt()
				}
				background = createShimmerDrawable(12 * d)
			}
			return ViewHolder(v)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
		override fun getItemCount(): Int = count
		inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
	}

	inner class ShimmerRekomendasiAdapter(private val count: Int) : RecyclerView.Adapter<ShimmerRekomendasiAdapter.ViewHolder>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val ctx = parent.context
			val d = ctx.resources.displayMetrics.density

			val box = LinearLayout(ctx).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams = GridLayoutManager.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					setMargins((4 * d).toInt(), 0, (4 * d).toInt(), (14 * d).toInt())
				}
			}

			val coverStub = View(ctx).apply {
				layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (145 * d).toInt())
				background = createShimmerDrawable(10 * d)
			}
			box.addView(coverStub)

			val titleStub = View(ctx).apply {
				layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (14 * d).toInt()).apply {
					topMargin = (8 * d).toInt()
				}
				background = createShimmerDrawable(5 * d)
			}
			box.addView(titleStub)

			return ViewHolder(box)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
		override fun getItemCount(): Int = count
		inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
	}

	inner class RecyclerEpisodeAdapter(
		private val _data: ArrayList<HashMap<String, Any>>
	) : RecyclerView.Adapter<RecyclerEpisodeAdapter.ViewHolder>() {

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val _v = layoutInflater.inflate(R.layout.eps_list, parent, false)
			return ViewHolder(_v)
		}

		override fun onBindViewHolder(_holder: ViewHolder, _position: Int) {
			val pos = _position
			val itemView = _holder.itemView
			val density = itemView.context.resources.displayMetrics.density

			val widthPx = (65 * density).toInt()
			val heightPx = (60 * density).toInt()
			val marginEndPx = (8 * density).toInt()

			itemView.layoutParams = RecyclerView.LayoutParams(widthPx, heightPx).apply {
				marginEnd = marginEndPx
			}

			val itemBg = GradientDrawable().apply {
				cornerRadius = 12 * density
			}

			val tvEpisode = itemView.findViewById<TextView>(R.id.textview1)
			val isActive = (pos == currentEpisodeIndex)

			if (isActive) {
				itemBg.setColor(0xFF3B82F6.toInt())
				tvEpisode?.apply {
					setTextColor(0xFFFFFFFF.toInt())
					setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				}
			} else {
				itemBg.setColor(Color.parseColor("#12141A"))
				tvEpisode?.apply {
					setTextColor(0xFFCBD5E1.toInt())
					setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				}
			}
			itemView.background = itemBg

			val itemData = _data[pos]
			if (tvEpisode != null) {
				when {
					itemData.containsKey("episode") -> {
						val epObj = itemData["episode"]
						if (epObj != null) {
							tvEpisode.text = epObj.toString().replace("Ep ", "").replace("Eps ", "").trim()
						}
					}
					itemData.containsKey("title") -> {
						val tObj = itemData["title"]
						if (tObj != null) {
							tvEpisode.text = tObj.toString().replace("[^0-9]".toRegex(), "").trim()
						}
					}
					else -> tvEpisode.text = (pos + 1).toString()
				}
			}

			itemView.setOnClickListener {
				if (currentEpisodeIndex == pos && mediaPlayer?.isPlaying == true) {
					return@setOnClickListener
				}

				if (episodeList !== _data) {
					episodeList.clear()
					episodeList.addAll(_data)
				}

				playEpisodeByIndex(pos, 0)
			}
		}

		override fun getItemCount(): Int = _data.size

		inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
	}

	inner class RecyclerRekomendasiAdapter(
		private val _data: ArrayList<HashMap<String, Any>>
	) : RecyclerView.Adapter<RecyclerRekomendasiAdapter.ViewHolder>() {

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val v = layoutInflater.inflate(R.layout.custom_anime, parent, false)
			return ViewHolder(v)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val item = _data[position]
			val view = holder.itemView
			val d = view.resources.displayMetrics.density

			view.layoutParams = GridLayoutManager.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				setMargins((4 * d).toInt(), 0, (4 * d).toInt(), (14 * d).toInt())
			}

			val textTitle = view.findViewById<TextView>(R.id.text_title)
			val textStatus = view.findViewById<TextView>(R.id.text_status)
			val textEpisode = view.findViewById<TextView>(R.id.text_episode)
			val imgCover = view.findViewById<ImageView>(R.id.img_cover)
			val view2 = view.findViewById<View>(R.id.view2)

			textStatus?.apply {
				background = GradientDrawable().apply {
					cornerRadius = 50f
					setColor(0xFF3B82F6.toInt())
				}
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				text = item["status"]?.toString() ?: ""
			}
			view2?.background = GradientDrawable(
				GradientDrawable.Orientation.BOTTOM_TOP,
				intArrayOf(Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT)
			)
			textTitle?.apply {
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				includeFontPadding = false
				text = item["title"]?.toString() ?: ""
			}
			textEpisode?.apply {
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				text = item["episode"]?.toString() ?: ""
			}

			var coverUrl = item["cover"]?.toString()?.trim() ?: ""
			if (coverUrl.contains("i.ibb.co.com")) coverUrl = coverUrl.replace("i.ibb.co.com", "i.ibb.co")
			if (coverUrl.startsWith("http://")) coverUrl = coverUrl.replace("http://", "https://")
			if (imgCover != null && coverUrl.isNotEmpty()) {
				Glide.with(applicationContext).load(Uri.parse(coverUrl)).into(imgCover)
			}

			view.setOnClickListener {
				val nextSlug = item["slug"]?.toString() ?: ""
				if (nextSlug.isNotEmpty()) {
					val intent = Intent(this@WatchActivity, WatchActivity::class.java).apply {
						putExtra("slug", nextSlug)
					}
					startActivity(intent)
					finish()
				}
			}
		}

		override fun getItemCount(): Int = _data.size

		inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
	}
}
