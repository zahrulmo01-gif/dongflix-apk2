package com.dongflix

import android.Manifest
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import java.io.File
import java.util.Calendar
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

	companion object {
		private const val FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app"
		private const val CATALOG_URL = "https://dongflix-api1.zahrulmo01.workers.dev/"
	}

	private var currentUserKey = ""
	private var currentUserEmail = ""
	private var currentDisplayName = "R Z"
	private var currentUsername = "rulsz"
	private var currentPhotoUrl = ""
	private var currentUserExp = 0L

	private var listMap = ArrayList<HashMap<String, Any>>()
	private var fullCatalogList = ArrayList<HashMap<String, Any>>()
	private var historyList = ArrayList<HashMap<String, Any>>()
	private var favoritesList = ArrayList<HashMap<String, Any>>()
	private var downloadsList = ArrayList<HashMap<String, Any>>()
	private val viewsTotalMap = HashMap<String, Long>()
	private val viewsDailyMap = HashMap<String, Long>()

	private var viewsTotalRef: DatabaseReference? = null
	private var viewsDailyTodayRef: DatabaseReference? = null
	private var userProfileRef: DatabaseReference? = null
	private var viewsTotalListener: ValueEventListener? = null
	private var viewsDailyListener: ValueEventListener? = null
	private var userProfileListener: ValueEventListener? = null

	private lateinit var dongflixReq: RequestNetwork
	private lateinit var dongflixReqListener: RequestNetwork.RequestListener

	private var gridview1: GridView? = null
	private var recyclerPopular: RecyclerView? = null
	private var recyclerPopularDongflix: RecyclerView? = null
	private var recyclerRomanceDonghua: RecyclerView? = null
	private var recyclerSearchList: RecyclerView? = null
	private var recyclerCultivationDonghua: RecyclerView? = null
	private var recyclerCompletedDonghua: RecyclerView? = null
	private var recyclerComedyDonghua: RecyclerView? = null
	private var recyclerMysteryDonghua: RecyclerView? = null
	private var recyclerMovieDonghua: RecyclerView? = null

	private var textHeroTitle: TextView? = null
	private var textHeroDesc: TextView? = null
	private var textSeeAll: TextView? = null
	private var btnClearSearch: TextView? = null
	private var textSearchSectionLabel: TextView? = null
	private var textPopularDongflixSeeAll: TextView? = null
	private var textRomanceSeeAll: TextView? = null
	private var textCultivationSeeAll: TextView? = null
	private var textCompletedSeeAll: TextView? = null
	private var textComedySeeAll: TextView? = null
	private var textMysterySeeAll: TextView? = null
	private var textMovieSeeAll: TextView? = null

	private var imgHeroBanner: ImageView? = null
	private var imgPlayIcon: ImageView? = null
	private var imgHeroFavorite: ImageView? = null
	private var imgTopNotif: ImageView? = null
	private var imgTopChat: ImageView? = null
	private var imgUserAvatar: ImageView? = null

	private var btnHeroPlay: LinearLayout? = null
	private var indicatorCapsule: LinearLayout? = null
	private var searchBarContainer: LinearLayout? = null
	private var btnHeroFavorite: FrameLayout? = null
	private var btnTopNotif: FrameLayout? = null
	private var btnTopChat: FrameLayout? = null
	private var dot0: View? = null
	private var dot1: View? = null
	private var dot2: View? = null
	private var inputSearch: EditText? = null

	private val heroSliderHandler = Handler(Looper.getMainLooper())
	private var heroSliderRunnable: Runnable? = null
	private var heroCurrentIndex = 0

	private var pullRefreshIndicator: FrameLayout? = null
	private var pullRefreshSpinner: ProgressBar? = null
	private var imgPullRefreshIcon: ImageView? = null
	private var isRefreshingCatalog = false
	private var pullStartY = 0f
	private var isPullingFromTop = false

	private var layoutSectionContinue: LinearLayout? = null
	private var textTitleContinue: TextView? = null
	private var textContinueSeeAll: TextView? = null
	private var recyclerContinue: RecyclerView? = null

	// Pustaka UI
	private var tabPustakaRiwayat: TextView? = null
	private var tabPustakaDaftarku: TextView? = null
	private var tabPustakaUnduhan: TextView? = null
	private var textPustakaSectionLabel: TextView? = null
	private var textPustakaCountBadge: TextView? = null
	private var layoutPustakaEmptyState: LinearLayout? = null
	private var textPustakaEmptyTitle: TextView? = null
	private var textPustakaEmptyDesc: TextView? = null
	private var recyclerPustakaList: RecyclerView? = null
	private var activePustakaTab = 0

	// Akun UI
	private var imgAccountAvatar: ImageView? = null
	private var tvAccountInitial: TextView? = null
	private var tvAccountName: TextView? = null
	private var tvAccountHandle: TextView? = null
	private var tvStatWatchTime: TextView? = null
	private var tvStatSavedCount: TextView? = null
	private var tvStatExpCount: TextView? = null
	private var tvAccountLevelTitle: TextView? = null
	private var tvAccountExpProgressLabel: TextView? = null
	private var tvAccountExpPercent: TextView? = null
	private var viewAccountExpTrack: View? = null
	private var viewAccountExpFill: View? = null

	private var navbarExpanded: FrameLayout? = null
	private var btnCompactHome: FrameLayout? = null
	private var miniPlayerInlineSlot: FrameLayout? = null
	private var btnCircleSearch: FrameLayout? = null
	private var tabBeranda: LinearLayout? = null
	private var tabPustaka: LinearLayout? = null
	private var tabAkun: LinearLayout? = null
	private var imgHome: ImageView? = null
	private var imgFavorite: ImageView? = null
	private var imgProfile: ImageView? = null
	private var imgSearch: ImageView? = null
	private var txtHome: TextView? = null
	private var txtFavorite: TextView? = null
	private var txtProfile: TextView? = null
	private var liquidDragPill: View? = null
	private var activeNavTabIndex = 0
	private var isNavbarCompact = false

	private var isDraggingNavbarPill = false
	private var navDownRawX = 0f
	private var navInitialPillTx = 0f

	private var mainShimmerAnimator: ValueAnimator? = null
	private var mainShimmerPhase = 0f
	private var isMainLoading = true

	private var favPrefs: SharedPreferences? = null
	private var currentHeroItem: HashMap<String, Any>? = null
	private var outfitFont: Typeface? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val currentUser = FirebaseAuth.getInstance().currentUser
		if (currentUser == null) {
			val loginIntent = Intent(this, LoginActivity::class.java).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
			}
			startActivity(loginIntent)
			finish()
			return
		}

		setContentView(R.layout.activity_main)
		CrashLogDialog.showIfAvailable(this)

		DongflixPlayerManager.getInstance().initWithApplication(application)
		favPrefs = getSharedPreferences("dongflix_favorites", Context.MODE_PRIVATE)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
		) {
			requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 201)
		}

		initialize()
		setupContinueWatchingSection()
		setupPullToRefresh()
		setupPustakaSection()
		setupAccountSection()
		initializeLogic()
		loadLoggedInUserProfile(currentUser)
		loadUserHistoryAndFavoritesFromFirebase(currentUser)
		attachFirebasePopularityListeners()
		loadLocalDownloads()
	}

	private fun safeStr(map: Map<String, Any>?, key: String?, fallback: String): String {
		if (map == null || key == null) return fallback
		return map[key]?.toString() ?: fallback
	}

	private fun normalizeCoverUrl(raw: String?): String {
		if (raw == null) return ""
		val url = raw.trim().replace("i.ibb.co.com", "i.ibb.co")
		return if (url.startsWith("http://")) url.replace("http://", "https://") else url
	}

	private fun cleanText(input: String?): String {
		if (input == null) return ""
		return input
			.replace("â€™", "'")
			.replace("â€œ", "\"")
			.replace("â€ ", "\"")
			.replace("â€“", "-")
			.replace("\\[[^\\x20-\\x7E]+\\]".toRegex(), "")
			.replace("[^\\x20-\\x7E\\n]".toRegex(), "")
			.trim()
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

	private fun applyOutfitFontsWithoutBold() {
		val tf = getOutfitTypeface()
		val contentView = window.decorView.findViewById<View>(android.R.id.content) ?: return

		val queue: Queue<View> = LinkedList()
		queue.add(contentView)
		while (queue.isNotEmpty()) {
			val v = queue.poll()
			if (v is TextView) {
				v.setTypeface(tf, Typeface.NORMAL)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					v.letterSpacing = -0.01f
				}
			} else if (v is ViewGroup) {
				for (i in 0 until v.childCount) queue.add(v.getChildAt(i))
			}
		}
	}

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		val nav = navbarExpanded
		val pill = liquidDragPill
		if (nav != null && nav.visibility == View.VISIBLE && pill != null) {
			val loc = IntArray(2)
			nav.getLocationOnScreen(loc)
			val rawX = ev.rawX
			val rawY = ev.rawY
			val insideNav = rawX >= loc[0] && rawX <= (loc[0] + nav.width) &&
				rawY >= loc[1] && rawY <= (loc[1] + nav.height)

			val d = resources.displayMetrics.density
			val trackW = nav.width
			val minTx = 0f
			val maxTx = maxOf(0f, trackW - pill.width - (10 * d))

			when (ev.actionMasked) {
				MotionEvent.ACTION_DOWN -> {
					if (insideNav) {
						navDownRawX = rawX
						navInitialPillTx = pill.translationX
						isDraggingNavbarPill = false
						pill.animate().scaleX(1.06f).scaleY(1.04f).setDuration(100).start()
						return true
					}
				}
				MotionEvent.ACTION_MOVE -> {
					if (navDownRawX > 0f) {
						val dx = rawX - navDownRawX
						if (abs(dx) > (6 * d)) isDraggingNavbarPill = true
						if (isDraggingNavbarPill) {
							pill.translationX = maxOf(minTx, minOf(maxTx, navInitialPillTx + dx))
							return true
						}
					}
				}
				MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
					if (navDownRawX > 0f) {
						pill.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
						val slotW = maxOf(1f, (trackW - (10 * d)) / 3f)
						val targetIdx = if (isDraggingNavbarPill) {
							maxOf(0, minOf(2, (pill.translationX / slotW).roundToInt()))
						} else {
							maxOf(0, minOf(2, ((rawX - loc[0]) / maxOf(1f, trackW / 3f)).toInt()))
						}
						navDownRawX = 0f
						isDraggingNavbarPill = false
						selectNavTabByIndex(targetIdx, true)
						return true
					}
				}
			}
		}
		return super.dispatchTouchEvent(ev)
	}

	private fun applyCircularAvatar(target: ImageView?, photoUrl: String?) {
		if (target == null) return
		val d = resources.displayMetrics.density
		val sizePx = maxOf((46 * d).toInt(), if (target.width > 0) target.width else (46 * d).toInt())

		if (!photoUrl.isNullOrBlank()) {
			val cleanUrl = photoUrl.trim()
			target.setPadding(0, 0, 0, 0)
			target.clearColorFilter()
			target.background = null
			target.scaleType = ImageView.ScaleType.CENTER_CROP

			if (cleanUrl.startsWith("data:image")) {
				try {
					val base64Part = cleanUrl.substring(cleanUrl.indexOf(",") + 1)
					val decoded = Base64.decode(base64Part, Base64.DEFAULT)
					val rawBmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
					if (rawBmp != null) {
						val edge = minOf(rawBmp.width, rawBmp.height)
						val square = Bitmap.createBitmap(
							rawBmp,
							(rawBmp.width - edge) / 2,
							(rawBmp.height - edge) / 2,
							edge,
							edge
						)
						val scaled = Bitmap.createScaledBitmap(square, sizePx, sizePx, true)
						val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
						val canvas = Canvas(out)
						val p = Paint(Paint.ANTI_ALIAS_FLAG)
						val r = Rect(0, 0, sizePx, sizePx)
						canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p)
						p.xfermode = android.graphics.PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
						canvas.drawBitmap(scaled, r, r, p)
						target.setImageBitmap(out)
						return
					}
				} catch (ignored: Exception) {
				}
			} else if (!isFinishing && !isDestroyed) {
				Glide.with(this).load(Uri.parse(cleanUrl)).circleCrop()
					.placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(target)
				return
			}
		}

		val pad = (3 * d).toInt()
		target.background = null
		target.scaleType = ImageView.ScaleType.FIT_CENTER
		target.setPadding(pad, pad, pad, pad)
		target.setImageResource(R.drawable.ic_profile)
		target.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
	}

	private fun loadLoggedInUserProfile(user: FirebaseUser?) {
		val email = user?.email ?: return
		currentUserEmail = email.trim().lowercase(Locale.US)
		val fallbackName = if (currentUserEmail.contains("@")) {
			currentUserEmail.substring(0, currentUserEmail.indexOf("@"))
		} else {
			currentUserEmail
		}

		currentDisplayName = fallbackName
		currentUsername = fallbackName.lowercase(Locale.US).replace("[^a-z0-9_]".toRegex(), "")
		if (currentUsername.isEmpty()) currentUsername = "rulsz"
		currentPhotoUrl = user.photoUrl?.toString() ?: ""

		findViewById<TextView>(R.id.text_username)?.text = currentDisplayName
		applyCircularAvatar(imgUserAvatar, currentPhotoUrl)
		refreshAccountUi()

		currentUserKey = currentUserEmail.replace(".", ",")
		if (userProfileRef != null && userProfileListener != null) {
			userProfileRef?.removeEventListener(userProfileListener!!)
		}
		userProfileRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("users").child(currentUserKey)
		val listener = object : ValueEventListener {
			override fun onDataChange(snap: DataSnapshot) {
				if (isFinishing || isDestroyed) return
				if (!snap.exists()) {
					refreshAccountUi()
					return
				}
				val nameStr = snap.child("name").value?.toString()?.trim() ?: ""
				currentDisplayName = if (nameStr.isNotEmpty()) nameStr else fallbackName

				val userStr = snap.child("username").value?.toString()?.trim() ?: ""
				currentUsername = if (userStr.isNotEmpty()) {
					userStr.replaceFirst("^@".toRegex(), "")
				} else {
					val cleaned = currentDisplayName.lowercase(Locale.US).replace("[^a-z0-9_]".toRegex(), "")
					if (cleaned.isNotEmpty()) cleaned else fallbackName
				}

				currentPhotoUrl = snap.child("photoURL").value?.toString()?.trim() ?: ""
				currentUserExp = parseLongSafe(snap.child("exp").value)

				findViewById<TextView>(R.id.text_username)?.text = currentDisplayName
				applyCircularAvatar(imgUserAvatar, currentPhotoUrl)
				refreshAccountUi()
			}

			override fun onCancelled(error: DatabaseError) {
				if (!isFinishing && !isDestroyed) refreshAccountUi()
			}
		}
		userProfileListener = listener
		userProfileRef?.addValueEventListener(listener)
	}

	private fun setupPullToRefresh() {
		val mainScroll = findViewById<NestedScrollView>(R.id.main_scroll_view) ?: return
		val d = resources.displayMetrics.density

		pullRefreshIndicator?.background = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(Color.parseColor("#181C26"))
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			pullRefreshIndicator?.elevation = 18f
			pullRefreshSpinner?.indeterminateTintList = ColorStateList.valueOf(Color.WHITE)
		}
		imgPullRefreshIcon?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

		val thresholdPx = 90 * d
		mainScroll.setOnTouchListener { _, event ->
			if (isRefreshingCatalog) return@setOnTouchListener false
			when (event.actionMasked) {
				MotionEvent.ACTION_DOWN -> {
					pullStartY = event.rawY
					isPullingFromTop = (mainScroll.scrollY <= 0)
				}
				MotionEvent.ACTION_MOVE -> {
					if (isPullingFromTop && mainScroll.scrollY <= 0) {
						val deltaY = event.rawY - pullStartY
						if (deltaY > (16 * d)) {
							pullRefreshIndicator?.visibility = View.VISIBLE
							imgPullRefreshIcon?.apply {
								visibility = View.VISIBLE
								rotation = minOf(180f, deltaY * 1.4f)
							}
							pullRefreshSpinner?.visibility = View.GONE
						}
					}
				}
				MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
					if (isPullingFromTop && mainScroll.scrollY <= 0) {
						if (event.rawY - pullStartY >= thresholdPx) {
							triggerCatalogRefresh()
						} else {
							pullRefreshIndicator?.visibility = View.GONE
						}
					}
					isPullingFromTop = false
				}
			}
			false
		}
	}

	private fun triggerCatalogRefresh() {
		if (isRefreshingCatalog) return
		isRefreshingCatalog = true
		pullRefreshIndicator?.visibility = View.VISIBLE
		imgPullRefreshIcon?.visibility = View.GONE
		pullRefreshSpinner?.visibility = View.VISIBLE
		dongflixReq.startRequestNetwork("GET", CATALOG_URL, "", dongflixReqListener)
	}

	private fun hideRefreshIndicator() {
		isRefreshingCatalog = false
		pullRefreshIndicator?.visibility = View.GONE
		pullRefreshSpinner?.visibility = View.GONE
		imgPullRefreshIcon?.visibility = View.VISIBLE
	}

	private fun startHeroAutoSlider() {
		stopHeroAutoSlider()
		if (listMap.size < 3) return

		val d = resources.displayMetrics.density
		val activeW = (16 * d).toInt()
		val inactiveW = (5 * d).toInt()
		val dotH = (5 * d).toInt()
		val dotGap = (5 * d).toInt()

		val activeDot = GradientDrawable().apply {
			shape = GradientDrawable.RECTANGLE
			setColor(Color.WHITE)
			cornerRadius = 100f
		}

		val inactiveDot = GradientDrawable().apply {
			shape = GradientDrawable.RECTANGLE
			setColor(Color.parseColor("#80FFFFFF"))
			cornerRadius = 100f
		}

		heroCurrentIndex = 0
		val runnable = object : Runnable {
			override fun run() {
				if (isFinishing || isDestroyed || listMap.size < 3) return
				val pos = heroCurrentIndex % 3
				val item = listMap[pos]
				currentHeroItem = item

				val heroCover = normalizeCoverUrl(safeStr(item, "cover", ""))
				val heroSlug = safeStr(item, "slug", "")

				textHeroTitle?.text = cleanText(safeStr(item, "title", ""))
				textHeroDesc?.text = cleanText(safeStr(item, "synopsis", ""))
				updateHeroFavoriteIcon(heroSlug)

				if (imgHeroBanner != null && heroCover.isNotEmpty()) {
					Glide.with(this@MainActivity).load(Uri.parse(heroCover)).centerCrop()
						.placeholder(R.drawable.default_image).error(R.drawable.default_image).into(imgHeroBanner!!)
				}
				btnHeroPlay?.setOnClickListener { openWatchBySlug(heroSlug) }

				val dots = arrayOf(dot0, dot1, dot2)
				for (i in dots.indices) {
					val dotView = dots[i] ?: continue
					val isActive = (i == pos)
					val lp = dotView.layoutParams as? LinearLayout.LayoutParams
					if (lp != null) {
						lp.width = if (isActive) activeW else inactiveW
						lp.height = dotH
						if (i > 0) lp.leftMargin = dotGap
						dotView.layoutParams = lp
					}
					dotView.background = if (isActive) activeDot else inactiveDot
				}

				heroCurrentIndex = (pos + 1) % 3
				heroSliderHandler.postDelayed(this, 4000L)
			}
		}
		heroSliderRunnable = runnable
		heroSliderHandler.post(runnable)
	}

	private fun stopHeroAutoSlider() {
		heroSliderRunnable?.let { heroSliderHandler.removeCallbacks(it) }
		heroSliderRunnable = null
		heroSliderHandler.removeCallbacksAndMessages(null)
	}

	private fun bindSectionLikePopularToday(rv: RecyclerView?, items: ArrayList<HashMap<String, Any>>) {
		if (rv == null) return
		val d = resources.displayMetrics.density
		rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
		rv.isNestedScrollingEnabled = false
		rv.onFlingListener = null
		rv.clipToPadding = false
		rv.setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
		rv.adapter = HorizontalPosterAdapter(items, 114, 162)
	}

	private fun buildEmptyDummyList(count: Int): ArrayList<HashMap<String, Any>> {
		val list = ArrayList<HashMap<String, Any>>()
		for (i in 0 until count) {
			list.add(hashMapOf("is_loading" to true))
		}
		return list
	}

	private fun populateCategorizedSections() {
		if (fullCatalogList.isEmpty()) return

		val cultivation = ArrayList<HashMap<String, Any>>()
		val completed = ArrayList<HashMap<String, Any>>()
		val comedy = ArrayList<HashMap<String, Any>>()
		val mystery = ArrayList<HashMap<String, Any>>()
		val movie = ArrayList<HashMap<String, Any>>()
		val romance = ArrayList<HashMap<String, Any>>()

		for (item in fullCatalogList) {
			val status = safeStr(item, "status", "").lowercase(Locale.US)
			val genres = safeStr(item, "genres", "").lowercase(Locale.US)
			val meta = (
				"$genres ${safeStr(item, "title", "")} " +
					"${safeStr(item, "synopsis", "")} ${safeStr(item, "type", "")} ${safeStr(item, "episode", "")}"
				).lowercase(Locale.US)

			if (meta.contains("kultivasi") || meta.contains("cultivation") || meta.contains("abadi") || meta.contains("immortal")) {
				cultivation.add(item)
			}
			if (status.contains("completed") || status.contains("tamat")) completed.add(item)
			if (meta.contains("comedy") || meta.contains("komedi") || meta.contains("slice of life")) comedy.add(item)
			if (meta.contains("mystery") || meta.contains("misteri") || meta.contains("teki") ||
				meta.contains("thriller") || meta.contains("suspense") || meta.contains("detective")
			) {
				mystery.add(item)
			}
			if (meta.contains("movie") || meta.contains("film")) movie.add(item)
			if (genres.contains("romance") || genres.contains("romantis") || genres.contains("romansa") ||
				meta.contains("romance") || meta.contains("romantis")
			) {
				romance.add(item)
			}
		}

		val fallback = ArrayList(fullCatalogList.subList(0, minOf(8, fullCatalogList.size)))
		if (cultivation.isEmpty()) cultivation.addAll(fallback)
		if (completed.isEmpty()) completed.addAll(fallback)
		if (comedy.isEmpty()) comedy.addAll(fallback)
		if (mystery.isEmpty()) mystery.addAll(fallback)
		if (movie.isEmpty()) movie.addAll(fallback)
		if (romance.isEmpty()) romance.addAll(fallback)

		bindSectionLikePopularToday(recyclerCultivationDonghua, ArrayList(cultivation.subList(0, minOf(10, cultivation.size))))
		bindSectionLikePopularToday(recyclerCompletedDonghua, ArrayList(completed.subList(0, minOf(10, completed.size))))
		bindSectionLikePopularToday(recyclerComedyDonghua, ArrayList(comedy.subList(0, minOf(10, comedy.size))))
		bindSectionLikePopularToday(recyclerMysteryDonghua, ArrayList(mystery.subList(0, minOf(10, mystery.size))))
		bindSectionLikePopularToday(recyclerMovieDonghua, ArrayList(movie.subList(0, minOf(10, movie.size))))
		bindSectionLikePopularToday(recyclerRomanceDonghua, ArrayList(romance.subList(0, minOf(10, romance.size))))
	}

	private fun setupContinueWatchingSection() {
		layoutSectionContinue = findViewById(R.id.layout_section_continue)
		textTitleContinue = findViewById(R.id.text_title_continue)
		textContinueSeeAll = findViewById(R.id.text_continue_see_all)
		recyclerContinue = findViewById(R.id.recyclerContinue)

		recyclerContinue?.let { rv ->
			val d = resources.displayMetrics.density
			rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
			rv.isNestedScrollingEnabled = false
			rv.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (132 * d).toInt())
		}
		textContinueSeeAll?.apply {
			text = "Lihat riwayat"
			setTextColor(Color.parseColor("#94A3B8"))
			setOnClickListener {
				activePustakaTab = 0
				selectNavTabByIndex(1, true)
			}
		}
	}

	private fun setupPustakaSection() {
		val pageFavorite = findViewById<View>(R.id.layout_favorite)
		if (pageFavorite !is ViewGroup) return

		pageFavorite.removeAllViews()
		pageFavorite.setBackgroundColor(Color.parseColor("#000000"))

		val d = resources.displayMetrics.density
		var statusH = 0
		val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
		if (resId > 0) statusH = resources.getDimensionPixelSize(resId)

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			setPadding((12 * d).toInt(), statusH + (12 * d).toInt(), (12 * d).toInt(), 0)
		}

		val headerRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		}

		val tvTitle = TextView(this).apply {
			text = "Pustaka Saya"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}

		textPustakaCountBadge = TextView(this).apply {
			text = "0 item"
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			setPadding((10 * d).toInt(), (4 * d).toInt(), (10 * d).toInt(), (4 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#12141A"))
				cornerRadius = 100f
				setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#1E222B"))
			}
		}

		headerRow.addView(tvTitle)
		headerRow.addView(textPustakaCountBadge)
		root.addView(headerRow)

		val tabsPillContainer = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			setPadding((4 * d).toInt(), (4 * d).toInt(), (4 * d).toInt(), (4 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (44 * d).toInt()).apply {
				topMargin = (14 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#12141A"))
				cornerRadius = 100f
				setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#1E222B"))
			}
		}

		tabPustakaRiwayat = buildPustakaPillTab("Riwayat", 0)
		tabPustakaDaftarku = buildPustakaPillTab("Daftarku", 1)
		tabPustakaUnduhan = buildPustakaPillTab("Unduhan", 2)

		tabsPillContainer.addView(tabPustakaRiwayat)
		tabsPillContainer.addView(tabPustakaDaftarku)
		tabsPillContainer.addView(tabPustakaUnduhan)
		root.addView(tabsPillContainer)

		textPustakaSectionLabel = TextView(this).apply {
			text = "Terakhir Ditonton"
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (14 * d).toInt()
				bottomMargin = (10 * d).toInt()
			}
		}
		root.addView(textPustakaSectionLabel)

		layoutPustakaEmptyState = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			gravity = Gravity.CENTER
			setPadding((24 * d).toInt(), (48 * d).toInt(), (24 * d).toInt(), (120 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			visibility = View.GONE
		}

		textPustakaEmptyTitle = TextView(this).apply {
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			gravity = Gravity.CENTER
		}

		textPustakaEmptyDesc = TextView(this).apply {
			setTextColor(Color.parseColor("#64748B"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			gravity = Gravity.CENTER
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (6 * d).toInt()
			}
		}

		layoutPustakaEmptyState?.addView(textPustakaEmptyTitle)
		layoutPustakaEmptyState?.addView(textPustakaEmptyDesc)
		root.addView(layoutPustakaEmptyState)

		recyclerPustakaList = RecyclerView(this).apply {
			layoutManager = LinearLayoutManager(this@MainActivity)
			clipToPadding = false
			setPadding(0, 0, 0, (130 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
		}
		root.addView(recyclerPustakaList)

		pageFavorite.addView(root)
		updatePustakaTabVisual(activePustakaTab)
	}

	private fun buildPustakaPillTab(label: String, tabIndex: Int): TextView {
		return TextView(this).apply {
			text = label
			gravity = Gravity.CENTER
			textSize = TypedValue.COMPLEX_UNIT_SP.let { 13.5f }
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
			setOnClickListener {
				activePustakaTab = tabIndex
				updatePustakaTabVisual(tabIndex)
				renderActivePustakaTab()
			}
		}
	}

	private fun updatePustakaTabVisual(idx: Int) {
		val tabs = arrayOf(tabPustakaRiwayat, tabPustakaDaftarku, tabPustakaUnduhan)
		for (i in tabs.indices) {
			val tv = tabs[i] ?: continue
			val selected = (i == idx)
			if (selected) {
				tv.background = GradientDrawable().apply {
					setColor(Color.parseColor("#2563EB"))
					cornerRadius = 100f
				}
				tv.setTextColor(Color.WHITE)
			} else {
				tv.setBackgroundColor(Color.TRANSPARENT)
				tv.setTextColor(Color.parseColor("#94A3B8"))
			}
		}
	}

	private fun setupAccountSection() {
		val pageProfile = findViewById<View>(R.id.layout_profile)
		if (pageProfile !is ViewGroup) return

		pageProfile.removeAllViews()
		pageProfile.setBackgroundColor(Color.parseColor("#000000"))

		val d = resources.displayMetrics.density
		var statusH = 0
		val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
		if (resId > 0) statusH = resources.getDimensionPixelSize(resId)

		val scroll = ScrollView(this).apply {
			isFillViewport = true
			isVerticalScrollBarEnabled = false
			layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
		}

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((12 * d).toInt(), statusH + (12 * d).toInt(), (12 * d).toInt(), (130 * d).toInt())
			layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		}

		val topHeaderBar = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				bottomMargin = (12 * d).toInt()
			}
		}

		val tvPageTitle = TextView(this).apply {
			text = "Akun"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}

		val setPad = (9 * d).toInt()
		val setRes = resources.getIdentifier("ic_settings", "drawable", packageName)
		val btnAccountSetting = ImageView(this).apply {
			layoutParams = LinearLayout.LayoutParams((38 * d).toInt(), (38 * d).toInt())
			setPadding(setPad, setPad, setPad, setPad)
			setImageResource(if (setRes > 0) setRes else R.drawable.ic_profile)
			setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
			background = GradientDrawable().apply {
				shape = GradientDrawable.OVAL
				setColor(Color.parseColor("#12141A"))
				setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#1E2433"))
			}
			setOnClickListener { openAccountSettingsDialog() }
		}

		topHeaderBar.addView(tvPageTitle)
		topHeaderBar.addView(btnAccountSetting)
		root.addView(topHeaderBar)

		val profileCard = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#12141A"))
				cornerRadius = 18 * d
				setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#1E2433"))
			}
		}

		val topRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
		}

		val avatarHolder = FrameLayout(this).apply {
			layoutParams = LinearLayout.LayoutParams((58 * d).toInt(), (58 * d).toInt()).apply {
				rightMargin = (12 * d).toInt()
			}
		}

		imgAccountAvatar = ImageView(this).apply {
			layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			scaleType = ImageView.ScaleType.CENTER_CROP
		}

		tvAccountInitial = TextView(this).apply {
			layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			gravity = Gravity.CENTER
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}

		avatarHolder.addView(imgAccountAvatar)
		avatarHolder.addView(tvAccountInitial)

		val identityCol = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}

		tvAccountName = TextView(this).apply {
			text = "R Z"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			maxLines = 1
			ellipsize = TextUtils.TruncateAt.END
		}

		tvAccountHandle = TextView(this).apply {
			text = "@rulsz"
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (2 * d).toInt()
			}
		}

		identityCol.addView(tvAccountName)
		identityCol.addView(tvAccountHandle)

		val btnEditProfile = TextView(this).apply {
			text = "Ubah Profil"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			setPadding((14 * d).toInt(), (8 * d).toInt(), (14 * d).toInt(), (8 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#1A2030"))
				cornerRadius = 100f
				setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#2AFFFFFF"))
			}
			setOnClickListener { openEditProfileDialog() }
		}

		topRow.addView(avatarHolder)
		topRow.addView(identityCol)
		topRow.addView(btnEditProfile)
		profileCard.addView(topRow)

		val innerDiv = View(this).apply {
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxOf(1, (1 * d).toInt())).apply {
				topMargin = (14 * d).toInt()
				bottomMargin = (12 * d).toInt()
			}
			setBackgroundColor(Color.parseColor("#1AFFFFFF"))
		}
		profileCard.addView(innerDiv)

		val statsRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		}

		tvStatWatchTime = TextView(this)
		tvStatSavedCount = TextView(this)
		tvStatExpCount = TextView(this)

		statsRow.addView(buildStatColumn(tvStatWatchTime!!, "0m", "Waktu Tonton", d))
		statsRow.addView(buildStatColumn(tvStatSavedCount!!, "0", "Tersimpan", d))
		statsRow.addView(buildStatColumn(tvStatExpCount!!, "0", "Total XP", d))
		profileCard.addView(statsRow)

		root.addView(profileCard)

		val lifetimeBanner = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding((16 * d).toInt(), (12 * d).toInt(), (16 * d).toInt(), (12 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (12 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#0C2438"))
				cornerRadius = 16 * d
				setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#38BDF8"))
			}
		}

		val tvLifetimeTitle = TextView(this).apply {
			text = "✦ Premium Aktif Seumur Hidup"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}

		val tvLifetimeSub = TextView(this).apply {
			text = "Aktif"
			setTextColor(Color.parseColor("#38BDF8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}

		lifetimeBanner.addView(tvLifetimeTitle)
		lifetimeBanner.addView(tvLifetimeSub)
		root.addView(lifetimeBanner)

		val levelCard = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((16 * d).toInt(), (15 * d).toInt(), (16 * d).toInt(), (15 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (12 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#12141A"))
				cornerRadius = 18 * d
				setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#1E2433"))
			}
		}

		val lvlTopRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
		}

		tvAccountLevelTitle = TextView(this).apply {
			text = "Level 1"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}

		tvAccountExpPercent = TextView(this).apply {
			text = "0%"
			setTextColor(Color.parseColor("#38BDF8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}

		lvlTopRow.addView(tvAccountLevelTitle)
		lvlTopRow.addView(tvAccountExpPercent)
		levelCard.addView(lvlTopRow)

		tvAccountExpProgressLabel = TextView(this).apply {
			text = "0 / 1000 XP menuju level berikutnya"
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (4 * d).toInt()
				bottomMargin = (10 * d).toInt()
			}
		}
		levelCard.addView(tvAccountExpProgressLabel)

		val barFrame = FrameLayout(this).apply {
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (7 * d).toInt())
		}

		viewAccountExpTrack = View(this).apply {
			layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#1E2433"))
				cornerRadius = 100f
			}
		}

		viewAccountExpFill = View(this).apply {
			layoutParams = FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#38BDF8"))
				cornerRadius = 100f
			}
		}

		barFrame.addView(viewAccountExpTrack)
		barFrame.addView(viewAccountExpFill)
		levelCard.addView(barFrame)

		barFrame.post { updateAccountExpBarWidth(barFrame.width) }
		root.addView(levelCard)

		val menuCard = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (12 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#12141A"))
				cornerRadius = 18 * d
				setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#1E2433"))
			}
		}

		menuCard.addView(
			buildAccountMenuItem(
				"Komentar Saya",
				"Lihat semua komentar yang pernah kamu tulis",
				Color.WHITE,
				d
			) { openMyCommentsDialog() }
		)
		menuCard.addView(
			buildAccountMenuItem(
				"Pustaka & Riwayat",
				"Lihat riwayat tontonan dan favorit tersimpan",
				Color.WHITE,
				d
			) { selectNavTabByIndex(1, true) }
		)
		menuCard.addView(
			buildAccountMenuItem(
				"Chat & Komunitas",
				"Buka ruang obrolan dan request judul donghua",
				Color.WHITE,
				d
			) { startActivity(Intent(this@MainActivity, ChatActivity::class.java)) }
		)
		menuCard.addView(
			buildAccountMenuItem(
				"Keluar Akun",
				"Logout dari sesi akun saat ini",
				Color.parseColor("#F87171"),
				d
			) {
				FirebaseAuth.getInstance().signOut()
				val i = Intent(this@MainActivity, LoginActivity::class.java).apply {
					addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
				}
				startActivity(i)
				finish()
			}
		)

		root.addView(menuCard)
		scroll.addView(root)
		pageProfile.addView(scroll)
		refreshAccountUi()
	}

	private fun openAccountSettingsDialog() {
		val d = resources.displayMetrics.density
		val dialog = Dialog(this).apply {
			requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
		}

		val card = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((20 * d).toInt(), (20 * d).toInt(), (20 * d).toInt(), (18 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#12141A"))
				cornerRadius = 20 * d
				setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#24FFFFFF"))
			}
		}

		val tvTitle = TextView(this).apply {
			text = "Pengaturan Akun"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}
		card.addView(tvTitle)

		val tvInfo = TextView(this).apply {
			text = "Email: ${if (currentUserEmail.isEmpty()) "-" else currentUserEmail}\n" +
				"Status: Premium Aktif Seumur Hidup\n" +
				"Sinkronisasi Riwayat & EXP: Aktif"
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			setLineSpacing(4 * d, 1.05f)
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (10 * d).toInt()
				bottomMargin = (18 * d).toInt()
			}
		}
		card.addView(tvInfo)

		val btnRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.END
		}

		val btnEdit = TextView(this).apply {
			text = "Ubah Profil"
			setTextColor(Color.parseColor("#38BDF8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			setPadding((14 * d).toInt(), (8 * d).toInt(), (14 * d).toInt(), (8 * d).toInt())
			setOnClickListener {
				dialog.dismiss()
				openEditProfileDialog()
			}
		}

		val btnClose = TextView(this).apply {
			text = "Tutup"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			setPadding((18 * d).toInt(), (8 * d).toInt(), (18 * d).toInt(), (8 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				leftMargin = (8 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#2563EB"))
				cornerRadius = 100f
			}
			setOnClickListener { dialog.dismiss() }
		}

		btnRow.addView(btnEdit)
		btnRow.addView(btnClose)
		card.addView(btnRow)

		dialog.setContentView(card)
		dialog.window?.apply {
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			val screenW = resources.displayMetrics.widthPixels
			setLayout(minOf((340 * d).toInt(), screenW - (36 * d).toInt()), ViewGroup.LayoutParams.WRAP_CONTENT)
		}
		dialog.show()
	}

	private fun openMyCommentsDialog() {
		val d = resources.displayMetrics.density
		val dialog = Dialog(this).apply {
			requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
		}

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((18 * d).toInt(), (18 * d).toInt(), (18 * d).toInt(), (16 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#12141A"))
				cornerRadius = 20 * d
				setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#24FFFFFF"))
			}
		}

		val tvTitle = TextView(this).apply {
			text = "Komentar Saya"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}
		root.addView(tvTitle)

		val tvStatus = TextView(this).apply {
			text = "Memuat komentar..."
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (10 * d).toInt()
			}
		}
		root.addView(tvStatus)

		val scroll = ScrollView(this).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				minOf((360 * d).toInt(), resources.displayMetrics.heightPixels / 2)
			).apply {
				topMargin = (10 * d).toInt()
				bottomMargin = (14 * d).toInt()
			}
		}

		val listBox = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
		}
		scroll.addView(listBox)
		root.addView(scroll)

		val btnClose = TextView(this).apply {
			text = "Tutup"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			gravity = Gravity.CENTER
			setPadding((18 * d).toInt(), (9 * d).toInt(), (18 * d).toInt(), (9 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#2563EB"))
				cornerRadius = 100f
			}
			setOnClickListener { dialog.dismiss() }
		}
		root.addView(btnClose)

		dialog.setContentView(root)
		dialog.window?.apply {
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			val screenW = resources.displayMetrics.widthPixels
			setLayout(minOf((360 * d).toInt(), screenW - (28 * d).toInt()), ViewGroup.LayoutParams.WRAP_CONTENT)
		}
		dialog.show()

		FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("comments")
			.addListenerForSingleValueEvent(object : ValueEventListener {
				override fun onDataChange(snap: DataSnapshot) {
					if (isFinishing || isDestroyed) return
					val mine = ArrayList<HashMap<String, String>>()
					val curU = FirebaseAuth.getInstance().currentUser
					val myUid = curU?.uid ?: ""

					if (snap.exists()) {
						for (slugSnap in snap.children) {
							val slugKey = slugSnap.key ?: ""
							for (itemSnap in slugSnap.children) {
								val v = itemSnap.value
								if (v is Map<*, *>) {
									val cEmail = v["email"]?.toString()?.trim() ?: ""
									val cUid = v["uid"]?.toString()?.trim() ?: ""
									val match = (currentUserEmail.isNotEmpty() && currentUserEmail.equals(cEmail, ignoreCase = true)) ||
										(myUid.isNotEmpty() && myUid == cUid)
									if (match) {
										mine.add(
											hashMapOf(
												"slug" to slugKey,
												"text" to (v["text"]?.toString()?.trim() ?: ""),
												"stickerUrl" to (v["stickerUrl"]?.toString()?.trim() ?: ""),
												"ts" to parseLongSafe(v["timestamp"]).toString()
											)
										)
									}
								}
							}
						}
					}

					mine.sortWith { a, b ->
						parseLongSafe(b["ts"]).compareTo(parseLongSafe(a["ts"]))
					}

					tvStatus.text = if (mine.isEmpty()) {
						"Kamu belum menulis komentar apa pun."
					} else {
						"Total ${mine.size} komentar"
					}

					val density = resources.displayMetrics.density
					listBox.removeAllViews()
					for (item in mine) {
						val row = LinearLayout(this@MainActivity).apply {
							orientation = LinearLayout.VERTICAL
							setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
							layoutParams = LinearLayout.LayoutParams(
								ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
							).apply {
								bottomMargin = (8 * density).toInt()
							}
							background = GradientDrawable().apply {
								setColor(Color.parseColor("#181C26"))
								cornerRadius = 12 * density
							}
						}

						val slugRaw = item["slug"] ?: ""
						val tvSlug = TextView(this@MainActivity).apply {
							text = slugRaw.replace("_", "-")
							setTextColor(Color.parseColor("#38BDF8"))
							setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
							setTypeface(getOutfitTypeface(), Typeface.NORMAL)
						}

						val rawBody = item["text"] ?: ""
						val bodyStr = when {
							rawBody.isNotEmpty() -> rawBody
							!(item["stickerUrl"].isNullOrEmpty()) -> "[Stiker]"
							else -> ""
						}

						val tvBody = TextView(this@MainActivity).apply {
							text = bodyStr
							setTextColor(Color.WHITE)
							setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
							setTypeface(getOutfitTypeface(), Typeface.NORMAL)
							layoutParams = LinearLayout.LayoutParams(
								ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
							).apply {
								topMargin = (3 * density).toInt()
							}
						}

						row.addView(tvSlug)
						row.addView(tvBody)
						row.setOnClickListener {
							if (slugRaw.isNotEmpty()) {
								dialog.dismiss()
								openWatchBySlug(slugRaw)
							}
						}
						listBox.addView(row)
					}
				}

				override fun onCancelled(error: DatabaseError) {
					if (!isFinishing && !isDestroyed) {
						tvStatus.text = "Gagal memuat komentar."
					}
				}
			})
	}

	private fun buildStatColumn(valueView: TextView, initVal: String, label: String, d: Float): LinearLayout {
		val col = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			gravity = Gravity.CENTER
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}

		valueView.apply {
			text = initVal
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}

		val tvLbl = TextView(this).apply {
			text = label
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (2 * d).toInt()
			}
		}

		col.addView(valueView)
		col.addView(tvLbl)
		return col
	}

	private fun buildAccountMenuItem(
		title: String,
		sub: String,
		titleColor: Int,
		d: Float,
		listener: View.OnClickListener
	): LinearLayout {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding((14 * d).toInt(), (13 * d).toInt(), (14 * d).toInt(), (13 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			setOnClickListener(listener)
		}

		val txtCol = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}

		val tvTitle = TextView(this).apply {
			text = title
			setTextColor(titleColor)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}

		val tvSub = TextView(this).apply {
			text = sub
			setTextColor(Color.parseColor("#64748B"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}

		txtCol.addView(tvTitle)
		txtCol.addView(tvSub)

		val chev = TextView(this).apply {
			text = "›"
			setTextColor(Color.parseColor("#64748B"))
			textSize = 18f
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}

		row.addView(txtCol)
		row.addView(chev)
		return row
	}

	private fun openEditProfileDialog() {
		val d = resources.displayMetrics.density
		val dialog = Dialog(this).apply {
			requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
		}

		val card = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((20 * d).toInt(), (20 * d).toInt(), (20 * d).toInt(), (18 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#12141A"))
				cornerRadius = 20 * d
				setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#24FFFFFF"))
			}
		}

		val tvTitle = TextView(this).apply {
			text = "Ubah Profil"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
		}
		card.addView(tvTitle)

		val inBg = GradientDrawable().apply {
			setColor(Color.parseColor("#181C26"))
			cornerRadius = 12 * d
			setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#1FFFFFFF"))
		}

		val etName = EditText(this).apply {
			setText(currentDisplayName)
			hint = "Nama tampilan"
			setHintTextColor(Color.parseColor("#64748B"))
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			isSingleLine = true
			setPadding((14 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (14 * d).toInt()
			}
			background = inBg
		}
		card.addView(etName)

		val etUsername = EditText(this).apply {
			setText(currentUsername)
			hint = "Username (tanpa @)"
			setHintTextColor(Color.parseColor("#64748B"))
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			isSingleLine = true
			setPadding((14 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = (10 * d).toInt()
				bottomMargin = (18 * d).toInt()
			}
			background = inBg.constantState?.newDrawable() ?: inBg
		}
		card.addView(etUsername)

		val btnRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.END
		}

		val btnCancel = TextView(this).apply {
			text = "Batal"
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			setPadding((16 * d).toInt(), (9 * d).toInt(), (16 * d).toInt(), (9 * d).toInt())
			setOnClickListener { dialog.dismiss() }
		}

		val btnSave = TextView(this).apply {
			text = "Simpan"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			setPadding((18 * d).toInt(), (9 * d).toInt(), (18 * d).toInt(), (9 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				leftMargin = (8 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#2563EB"))
				cornerRadius = 100f
			}
			setOnClickListener {
				val newName = etName.text.toString().trim()
				var newUser = etUsername.text.toString().trim()
					.replaceFirst("^@".toRegex(), "")
					.replace("\\s+".toRegex(), "_")
				if (newName.isEmpty()) {
					Toast.makeText(this@MainActivity, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
					return@setOnClickListener
				}
				if (newUser.isEmpty()) {
					newUser = newName.lowercase(Locale.US).replace("[^a-z0-9_]".toRegex(), "")
					if (newUser.isEmpty()) newUser = "user"
				}

				currentDisplayName = newName
				currentUsername = newUser

				findViewById<TextView>(R.id.text_username)?.text = currentDisplayName
				refreshAccountUi()

				if (currentUserKey.isNotEmpty()) {
					val updates = HashMap<String, Any>().apply {
						put("name", currentDisplayName)
						put("username", currentUsername)
						put("email", currentUserEmail)
						put("isLifetimePremium", true)
					}
					FirebaseDatabase.getInstance(FIREBASE_DB_URL)
						.getReference("users")
						.child(currentUserKey)
						.updateChildren(updates)
				}

				Toast.makeText(this@MainActivity, "Profil diperbarui", Toast.LENGTH_SHORT).show()
				dialog.dismiss()
			}
		}

		btnRow.addView(btnCancel)
		btnRow.addView(btnSave)
		card.addView(btnRow)

		dialog.setContentView(card)
		dialog.window?.apply {
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			val screenW = resources.displayMetrics.widthPixels
			setLayout(minOf((340 * d).toInt(), screenW - (36 * d).toInt()), ViewGroup.LayoutParams.WRAP_CONTENT)
		}
		dialog.show()
	}

	private fun refreshAccountUi() {
		tvAccountName?.text = currentDisplayName
		tvAccountHandle?.text = "@$currentUsername"

		val initial = if (currentDisplayName.trim().isNotEmpty()) {
			currentDisplayName.trim().substring(0, 1).uppercase(Locale.US)
		} else {
			"R"
		}

		if (currentPhotoUrl.trim().isNotEmpty()) {
			imgAccountAvatar?.apply {
				visibility = View.VISIBLE
				clearColorFilter()
				if (!isFinishing && !isDestroyed) {
					Glide.with(this@MainActivity).load(Uri.parse(currentPhotoUrl.trim())).circleCrop()
						.placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(this)
				}
			}
			tvAccountInitial?.visibility = View.GONE
		} else {
			imgAccountAvatar?.visibility = View.GONE
			tvAccountInitial?.apply {
				visibility = View.VISIBLE
				text = initial
				background = GradientDrawable().apply {
					shape = GradientDrawable.OVAL
					setColor(Color.parseColor("#1D4ED8"))
				}
			}
		}

		var totalWatchedMs = 0L
		for (h in historyList) {
			totalWatchedMs += parseLongSafe(h["positionMs"])
		}
		val totalMinutes = maxOf(0L, totalWatchedMs / 60000L)
		val watchLabel = if (totalMinutes >= 60L) {
			String.format(Locale.US, "%dj %dm", totalMinutes / 60L, totalMinutes % 60L)
		} else {
			"${totalMinutes}m"
		}

		tvStatWatchTime?.text = watchLabel
		tvStatSavedCount?.text = getMergedFavoritesList().size.toString()
		tvStatExpCount?.text = currentUserExp.toString()

		val level = if (currentUserExp <= 0L) 1 else ((currentUserExp - 1L) / 1000L).toInt() + 1
		val expInLevel = if (currentUserExp <= 0L) 0L else ((currentUserExp - 1L) % 1000L) + 1L
		val percent = maxOf(0L, minOf(100L, (expInLevel * 100L) / 1000L)).toInt()

		tvAccountLevelTitle?.text = "Level $level"
		tvAccountExpProgressLabel?.text = "$expInLevel / 1000 XP menuju level ${level + 1}"
		tvAccountExpPercent?.text = "$percent%"

		val parentBar = viewAccountExpTrack?.parent as? View
		if (parentBar != null && parentBar.width > 0) {
			updateAccountExpBarWidth(parentBar.width)
		}
	}

	private fun updateAccountExpBarWidth(totalBarWidth: Int) {
		val fill = viewAccountExpFill ?: return
		if (totalBarWidth <= 0) return
		val expInLevel = if (currentUserExp <= 0L) 0L else ((currentUserExp - 1L) % 1000L) + 1L
		val ratio = maxOf(0f, minOf(1f, expInLevel / 1000f))
		var targetW = (totalBarWidth * ratio).roundToInt()
		if (ratio > 0f && targetW < 8) targetW = 8
		val lp = fill.layoutParams
		if (lp != null && lp.width != targetW) {
			lp.width = targetW
			fill.layoutParams = lp
		}
	}

	private fun loadUserHistoryAndFavoritesFromFirebase(user: FirebaseUser?) {
		val email = user?.email ?: return
		currentUserKey = email.trim().lowercase(Locale.getDefault()).replace(".", ",")
		val userNode = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("users").child(currentUserKey)

		userNode.child("history").addValueEventListener(object : ValueEventListener {
			override fun onDataChange(snap: DataSnapshot) {
				if (isFinishing || isDestroyed) return
				val parsed = ArrayList<HashMap<String, Any>>()
				if (snap.exists()) {
					for (child in snap.children) {
						val item = extractMapFromSnapshot(child)
						if (item.isNotEmpty()) {
							if (!item.containsKey("slug") && child.key != null) item["slug"] = child.key!!
							parsed.add(item)
						}
					}
					parsed.sortWith { a, b ->
						val tsA = parseLongSafe(a["updatedAt"] ?: a["timestamp"])
						val tsB = parseLongSafe(b["updatedAt"] ?: b["timestamp"])
						tsB.compareTo(tsA)
					}
				}
				historyList = parsed
				renderContinueWatchingCards()
				refreshAccountUi()
				if (activePustakaTab == 0) renderActivePustakaTab()
			}

			override fun onCancelled(error: DatabaseError) {}
		})

		userNode.child("favorites").addValueEventListener(object : ValueEventListener {
			override fun onDataChange(snap: DataSnapshot) {
				if (isFinishing || isDestroyed) return
				val parsedFavs = ArrayList<HashMap<String, Any>>()
				if (snap.exists()) {
					for (child in snap.children) {
						val item = extractMapFromSnapshot(child)
						if (item.isNotEmpty()) {
							if (!item.containsKey("slug") && child.key != null) item["slug"] = child.key!!
							parsedFavs.add(item)
						}
					}
				}
				favoritesList = parsedFavs
				refreshAccountUi()
				if (activePustakaTab == 1) renderActivePustakaTab()
			}

			override fun onCancelled(error: DatabaseError) {}
		})
	}

	private fun extractMapFromSnapshot(snap: DataSnapshot?): HashMap<String, Any> {
		val map = HashMap<String, Any>()
		if (snap == null || !snap.exists()) return map
		val value = snap.value
		if (value is Map<*, *>) {
			for ((k, v) in value) {
				if (k != null && v != null) map[k.toString()] = v
			}
		} else if (value is String) {
			val s = value.trim()
			if (s.startsWith("{")) {
				try {
					val type = object : TypeToken<HashMap<String, Any>>() {}.type
					val jsonMap = Gson().fromJson<HashMap<String, Any>>(s, type)
					if (jsonMap != null) map.putAll(jsonMap)
				} catch (ignored: Exception) {
				}
			} else if (s.isNotEmpty()) {
				map["title"] = s
				map["slug"] = snap.key ?: s
			}
		}
		return map
	}

	private fun parseLongSafe(o: Any?): Long {
		if (o == null) return 0L
		if (o is Number) return o.toLong()
		return try {
			o.toString().trim().toLong()
		} catch (e: Exception) {
			0L
		}
	}

	private fun renderContinueWatchingCards() {
		val section = layoutSectionContinue ?: return
		val rv = recyclerContinue ?: return
		if (historyList.isEmpty()) {
			section.visibility = View.GONE
			return
		}
		section.visibility = View.VISIBLE
		rv.adapter = ContinueWatchingAdapter(historyList)
	}

	private fun loadLocalDownloads() {
		val found = ArrayList<HashMap<String, Any>>()
		try {
			val dirs = arrayOf(
				getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
				getExternalFilesDir(Environment.DIRECTORY_MOVIES),
				filesDir
			)
			for (dir in dirs) {
				if (dir != null && dir.exists() && dir.isDirectory && dir.canRead()) {
					collectDownloadFiles(dir, found, 0)
				}
			}
		} catch (ignored: Exception) {
		}
		downloadsList = found
	}

	private fun collectDownloadFiles(dir: File?, out: ArrayList<HashMap<String, Any>>, depth: Int) {
		if (dir == null || depth > 3) return
		val children = try {
			dir.listFiles()
		} catch (e: Exception) {
			return
		} ?: return

		for (f in children) {
			if (!f.exists() || !f.canRead()) continue
			if (f.isDirectory) {
				collectDownloadFiles(f, out, depth + 1)
			} else if (f.length() > 0L) {
				val lower = f.name.lowercase(Locale.US)
				if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm")) {
					out.add(
						hashMapOf(
							"title" to f.name.replace("\\.[^.]+$".toRegex(), "").replace("_", " "),
							"episode" to "Offline",
							"file_path" to f.absolutePath,
							"size_mb" to String.format(Locale.US, "%.1f MB", f.length() / (1024f * 1024f))
						)
					)
				}
			}
		}
	}

	private fun renderActivePustakaTab() {
		val rv = recyclerPustakaList ?: return

		val activeData: ArrayList<HashMap<String, Any>>
		val sectionLabel: String
		val emptyTitle: String
		val emptyDesc: String

		when (activePustakaTab) {
			0 -> {
				activeData = historyList
				sectionLabel = "Terakhir Ditonton"
				emptyTitle = "Belum ada riwayat tontonan"
				emptyDesc = "Episode yang kamu tonton akan muncul di sini."
			}
			1 -> {
				activeData = getMergedFavoritesList()
				sectionLabel = "Daftar Tersimpan"
				emptyTitle = "Daftarku masih kosong"
				emptyDesc = "Simpan donghua favoritmu untuk ditonton nanti."
			}
			else -> {
				loadLocalDownloads()
				activeData = downloadsList
				sectionLabel = "Video Offline"
				emptyTitle = "Belum ada unduhan"
				emptyDesc = "Video yang kamu unduh akan tersimpan di perangkat ini."
			}
		}

		textPustakaSectionLabel?.text = sectionLabel
		textPustakaCountBadge?.text = "${activeData.size} item"

		if (activeData.isEmpty()) {
			layoutPustakaEmptyState?.visibility = View.VISIBLE
			textPustakaEmptyTitle?.text = emptyTitle
			textPustakaEmptyDesc?.text = emptyDesc
			rv.visibility = View.GONE
		} else {
			layoutPustakaEmptyState?.visibility = View.GONE
			rv.visibility = View.VISIBLE
			rv.adapter = PustakaListAdapter(activeData, activePustakaTab)
		}
	}

	private fun openDownloadedVideoSafely(filePath: String?, title: String) {
		if (filePath.isNullOrBlank()) {
			Toast.makeText(this, "File video tidak ditemukan", Toast.LENGTH_SHORT).show()
			return
		}
		val videoFile = File(filePath)
		if (!videoFile.exists() || !videoFile.canRead() || videoFile.length() <= 0L) {
			Toast.makeText(this, "File video rusak atau sudah dihapus", Toast.LENGTH_SHORT).show()
			renderActivePustakaTab()
			return
		}
		try {
			val intent = Intent(Intent.ACTION_VIEW)
			val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				FileProvider.getUriForFile(this, "$packageName.fileprovider", videoFile)
			} else {
				Uri.fromFile(videoFile)
			}
			intent.setDataAndType(uri, "video/*")
			startActivity(Intent.createChooser(intent, "Putar $title"))
		} catch (e: Exception) {
			Toast.makeText(this, "Tidak ada pemutar video yang mendukung file ini", Toast.LENGTH_SHORT).show()
		}
	}

	private fun removePustakaItem(mode: Int, item: HashMap<String, Any>?) {
		if (item == null) return
		when (mode) {
			0 -> {
				val slug = safeStr(item, "slug", "")
				if (slug.isNotEmpty() && currentUserKey.isNotEmpty()) {
					FirebaseDatabase.getInstance(FIREBASE_DB_URL)
						.getReference("users").child(currentUserKey).child("history").child(slug)
						.removeValue()
				}
				historyList.remove(item)
				renderContinueWatchingCards()
				renderActivePustakaTab()
			}
			1 -> {
				val slug = safeStr(item, "slug", "")
				if (slug.isNotEmpty()) {
					favPrefs?.edit()?.remove("fav_$slug")?.apply()
					if (currentUserKey.isNotEmpty()) {
						FirebaseDatabase.getInstance(FIREBASE_DB_URL)
							.getReference("users").child(currentUserKey).child("favorites").child(slug)
							.removeValue()
					}
				}
				favoritesList.remove(item)
				renderActivePustakaTab()
			}
			else -> {
				val path = safeStr(item, "file_path", "")
				if (path.isNotEmpty()) {
					try {
						val f = File(path)
						if (f.exists()) f.delete()
					} catch (ignored: Exception) {
					}
				}
				renderActivePustakaTab()
			}
		}
	}

	private fun getMergedFavoritesList(): ArrayList<HashMap<String, Any>> {
		val dedup = LinkedHashMap<String, HashMap<String, Any>>()
		for (item in favoritesList) {
			val s = safeStr(item, "slug", "")
			if (s.isNotEmpty()) dedup[s] = item
		}
		favPrefs?.all?.forEach { (k, v) ->
			if (k.startsWith("fav_") && v is String) {
				try {
					val type = object : TypeToken<HashMap<String, Any>>() {}.type
					val localMap = Gson().fromJson<HashMap<String, Any>>(v, type)
					if (localMap != null) {
						val s = safeStr(localMap, "slug", k.substring(4))
						if (!dedup.containsKey(s)) dedup[s] = localMap
					}
				} catch (ignored: Exception) {
				}
			}
		}
		return ArrayList(dedup.values)
	}

	private fun sortCatalogByViews(
		src: List<HashMap<String, Any>>,
		primary: Map<String, Long>,
		secondary: Map<String, Long>?
	): ArrayList<HashMap<String, Any>> {
		val sorted = ArrayList(src)
		Collections.sort(sorted) { a, b ->
			val sA = safeStr(a, "slug", "")
			val sB = safeStr(b, "slug", "")
			val pA = primary[sA] ?: 0L
			val pB = primary[sB] ?: 0L
			if (pB != pA) return@sort pB.compareTo(pA)
			if (secondary == null) return@sort 0
			val secA = secondary[sA] ?: 0L
			val secB = secondary[sB] ?: 0L
			secB.compareTo(secA)
		}
		return sorted
	}

	private fun sortAndRenderPopularSections() {
		if (fullCatalogList.isEmpty()) return

		val todaySorted = sortCatalogByViews(fullCatalogList, viewsDailyMap, viewsTotalMap)
		val dongflixSorted = sortCatalogByViews(fullCatalogList, viewsTotalMap, viewsDailyMap)

		val todaySlice = ArrayList(todaySorted.subList(0, minOf(8, todaySorted.size)))
		val dongflixPopularSlice = ArrayList(dongflixSorted.subList(0, minOf(10, dongflixSorted.size)))

		bindSectionLikePopularToday(recyclerPopular, todaySlice)
		bindSectionLikePopularToday(recyclerPopularDongflix, dongflixPopularSlice)

		if (recyclerSearchList != null && (inputSearch?.text?.toString()?.trim()?.isEmpty() != false)) {
			recyclerSearchList?.adapter = ListPopularAdapter(dongflixPopularSlice)
		}
		populateCategorizedSections()
	}

	private fun attachFirebasePopularityListeners() {
		detachFirebasePopularityListeners()

		val cal = Calendar.getInstance()
		val todayStr = String.format(
			Locale.US,
			"%04d-%02d-%02d",
			cal.get(Calendar.YEAR),
			cal.get(Calendar.MONTH) + 1,
			cal.get(Calendar.DAY_OF_MONTH)
		)

		val rootDb = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
		viewsTotalRef = rootDb.child("views_total")
		viewsDailyTodayRef = rootDb.child("views_daily").child(todayStr)

		val totalListener = object : ValueEventListener {
			override fun onDataChange(snap: DataSnapshot) {
				if (isFinishing || isDestroyed) return
				viewsTotalMap.clear()
				if (snap.exists()) {
					for (child in snap.children) {
						child.key?.let { k -> viewsTotalMap[k] = parseLongSafe(child.value) }
					}
				}
				sortAndRenderPopularSections()
			}

			override fun onCancelled(error: DatabaseError) {}
		}

		val dailyListener = object : ValueEventListener {
			override fun onDataChange(snap: DataSnapshot) {
				if (isFinishing || isDestroyed) return
				viewsDailyMap.clear()
				if (snap.exists()) {
					for (child in snap.children) {
						child.key?.let { k -> viewsDailyMap[k] = parseLongSafe(child.value) }
					}
				}
				sortAndRenderPopularSections()
			}

			override fun onCancelled(error: DatabaseError) {}
		}

		viewsTotalListener = totalListener
		viewsDailyListener = dailyListener
		viewsTotalRef?.addValueEventListener(totalListener)
		viewsDailyTodayRef?.addValueEventListener(dailyListener)
	}

	private fun detachFirebasePopularityListeners() {
		if (viewsTotalRef != null && viewsTotalListener != null) {
			viewsTotalRef?.removeEventListener(viewsTotalListener!!)
		}
		if (viewsDailyTodayRef != null && viewsDailyListener != null) {
			viewsDailyTodayRef?.removeEventListener(viewsDailyListener!!)
		}
	}

	private fun isFavoriteSlug(slug: String?): Boolean {
		return favPrefs != null && !slug.isNullOrEmpty() && favPrefs?.contains("fav_$slug") == true
	}

	private fun toggleFavoriteItem(item: HashMap<String, Any>?) {
		val prefs = favPrefs ?: return
		if (item == null) return
		val slug = safeStr(item, "slug", "")
		val title = safeStr(item, "title", "Donghua")
		if (slug.isEmpty()) return

		val favNode = if (currentUserKey.isNotEmpty()) {
			FirebaseDatabase.getInstance(FIREBASE_DB_URL)
				.getReference("users").child(currentUserKey).child("favorites").child(slug)
		} else {
			null
		}

		if (isFavoriteSlug(slug)) {
			prefs.edit().remove("fav_$slug").apply()
			favNode?.removeValue()
			Toast.makeText(this, "Dihapus dari Daftarku", Toast.LENGTH_SHORT).show()
		} else {
			prefs.edit().putString("fav_$slug", Gson().toJson(item)).apply()
			favNode?.setValue(item)
			Toast.makeText(this, "Disimpan ke Daftarku: $title", Toast.LENGTH_SHORT).show()
		}
		updateHeroFavoriteIcon(slug)
		if (activePustakaTab == 1) renderActivePustakaTab()
	}

	private fun updateHeroFavoriteIcon(slug: String) {
		val favIcon = imgHeroFavorite ?: return
		if (isFavoriteSlug(slug)) {
			favIcon.setImageResource(R.drawable.ic_bookmark_filled)
			favIcon.setColorFilter(Color.parseColor("#3B82F6"))
		} else {
			favIcon.setImageResource(R.drawable.ic_bookmark)
			favIcon.setColorFilter(Color.WHITE)
		}
	}

	private fun openWatchBySlug(slug: String?) {
		if (slug.isNullOrBlank()) return
		hideKeyboard()
		val intent = Intent(this, WatchActivity::class.java).apply {
			putExtra("slug", slug.trim())
		}
		startActivity(intent)
		overridePendingTransition(R.anim.slide_up_enter, R.anim.no_anim)
	}

	private fun openNotificationsSheet() {
		val latestTitle = currentHeroItem?.let { safeStr(it, "title", "Episode terbaru Dongflix telah tersedia.") }
			?: "Episode terbaru Dongflix telah tersedia."
		AlertDialog.Builder(this)
			.setTitle("Notifikasi")
			.setMessage("• Update katalog harian aktif\n• $latestTitle")
			.setPositiveButton("Tutup", null)
			.show()
	}

	private fun showKeyboardOnSearch() {
		val et = inputSearch ?: return
		et.requestFocus()
		et.postDelayed({
			val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
			imm?.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
		}, 120L)
	}

	private fun hideKeyboard() {
		val et = inputSearch ?: return
		val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
		imm?.hideSoftInputFromWindow(et.windowToken, 0)
	}

	private fun createMainShimmerDrawable(cornerRadiusPx: Float): Drawable {
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
				canvas.drawRoundRect(RectF(b), cornerRadiusPx, cornerRadiusPx, basePaint)

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
				shaderMatrix.setTranslate(-w + (2f * w * mainShimmerPhase), 0f)
				gradient?.setLocalMatrix(shaderMatrix)
				canvas.drawRoundRect(RectF(b), cornerRadiusPx, cornerRadiusPx, shimmerPaint)
			}

			override fun setAlpha(alpha: Int) {}
			override fun setColorFilter(colorFilter: ColorFilter?) {}
			@Deprecated("Deprecated in Java")
			override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
		}
	}

	private fun startMainShimmer() {
		isMainLoading = true
		mainShimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = 1250L
			repeatCount = ValueAnimator.INFINITE
			interpolator = LinearInterpolator()
			addUpdateListener { animation ->
				if (!isMainLoading) return@addUpdateListener
				mainShimmerPhase = animation.animatedValue as Float
				val singles = arrayOf(imgHeroBanner, textHeroTitle, textHeroDesc, btnHeroPlay, btnHeroFavorite)
				for (v in singles) v?.invalidate()

				val hRecyclers = arrayOf(
					recyclerPopular, recyclerPopularDongflix, recyclerRomanceDonghua,
					recyclerCultivationDonghua, recyclerCompletedDonghua,
					recyclerComedyDonghua, recyclerMysteryDonghua, recyclerMovieDonghua
				)
				for (rv in hRecyclers) {
					rv?.let {
						for (i in 0 until it.childCount) invalidateRecursive(it.getChildAt(i))
					}
				}
				gridview1?.let { gv ->
					for (i in 0 until gv.childCount) invalidateRecursive(gv.getChildAt(i))
				}
			}
			start()
		}
	}

	private fun invalidateRecursive(v: View?) {
		if (v == null) return
		v.invalidate()
		if (v is ViewGroup) {
			for (i in 0 until v.childCount) invalidateRecursive(v.getChildAt(i))
		}
	}

	private fun stopMainShimmer() {
		isMainLoading = false
		mainShimmerAnimator?.cancel()
		mainShimmerAnimator = null
	}

	override fun onDestroy() {
		stopMainShimmer()
		stopHeroAutoSlider()
		detachFirebasePopularityListeners()
		if (userProfileRef != null && userProfileListener != null) {
			userProfileRef?.removeEventListener(userProfileListener!!)
		}
		super.onDestroy()
	}

	private fun initialize() {
		dongflixReq = RequestNetwork(this)

		pullRefreshIndicator = findViewById(R.id.pull_refresh_indicator)
		pullRefreshSpinner = findViewById(R.id.pull_refresh_spinner)
		imgPullRefreshIcon = findViewById(R.id.img_pull_refresh_icon)

		gridview1 = findViewById(R.id.gridview1)
		recyclerPopular = findViewById(R.id.recyclerPopular)
		recyclerPopularDongflix = findViewById(R.id.recyclerPopularDongflix)
		recyclerRomanceDonghua = findViewById(R.id.recyclerRomanceDonghua)
		recyclerCultivationDonghua = findViewById(R.id.recyclerCultivationDonghua)
		recyclerCompletedDonghua = findViewById(R.id.recyclerCompletedDonghua)
		recyclerComedyDonghua = findViewById(R.id.recyclerComedyDonghua)
		recyclerMysteryDonghua = findViewById(R.id.recyclerMysteryDonghua)
		recyclerMovieDonghua = findViewById(R.id.recyclerMovieDonghua)

		recyclerSearchList = findViewById(R.id.recycler_search_list)
		searchBarContainer = findViewById(R.id.search_bar_container)
		inputSearch = findViewById(R.id.input_search)
		btnClearSearch = findViewById(R.id.btn_clear_search)
		textSearchSectionLabel = findViewById(R.id.text_search_section_label)

		textHeroTitle = findViewById(R.id.text_hero_title)
		textHeroDesc = findViewById(R.id.text_hero_desc)
		textSeeAll = findViewById(R.id.text_see_all)
		textPopularDongflixSeeAll = findViewById(R.id.text_popular_dongflix_see_all)
		textRomanceSeeAll = findViewById(R.id.text_romance_see_all)
		textCultivationSeeAll = findViewById(R.id.text_cultivation_see_all)
		textCompletedSeeAll = findViewById(R.id.text_completed_see_all)
		textComedySeeAll = findViewById(R.id.text_comedy_see_all)
		textMysterySeeAll = findViewById(R.id.text_mystery_see_all)
		textMovieSeeAll = findViewById(R.id.text_movie_see_all)

		imgHeroBanner = findViewById(R.id.img_hero_banner)
		imgUserAvatar = findViewById(R.id.img_user_avatar)
		btnHeroPlay = findViewById(R.id.btn_hero_play)
		btnHeroFavorite = findViewById(R.id.btn_hero_favorite)
		imgHeroFavorite = findViewById(R.id.img_hero_favorite)
		imgPlayIcon = findViewById(R.id.img_play_icon)
		indicatorCapsule = findViewById(R.id.indicator_capsule)
		dot0 = findViewById(R.id.dot_0)
		dot1 = findViewById(R.id.dot_1)
		dot2 = findViewById(R.id.dot_2)

		btnTopNotif = findViewById(R.id.btn_top_search)
		btnTopChat = findViewById(R.id.btn_top_chat)
		imgTopNotif = findViewById(R.id.img_top_search)
		imgTopChat = findViewById(R.id.img_top_chat)

		navbarExpanded = findViewById(R.id.navbar)
		btnCompactHome = findViewById(R.id.btn_compact_home)
		miniPlayerInlineSlot = findViewById(R.id.mini_player_inline_slot)
		btnCircleSearch = findViewById(R.id.btn_circle_search)
		tabBeranda = findViewById(R.id.tab_beranda)
		tabPustaka = findViewById(R.id.tab_pustaka)
		tabAkun = findViewById(R.id.tab_akun)
		imgHome = findViewById(R.id.img_home)
		imgFavorite = findViewById(R.id.img_favorite)
		imgProfile = findViewById(R.id.img_profile)
		imgSearch = findViewById(R.id.img_search)
		txtHome = findViewById(R.id.txt_home)
		txtFavorite = findViewById(R.id.txt_favorite)
		txtProfile = findViewById(R.id.txt_profile)

		val seeAllConfig = LinkedHashMap<TextView?, Array<String>>().apply {
			put(textSeeAll, arrayOf("Baru Diupdate", "all"))
			put(textPopularDongflixSeeAll, arrayOf("Populer di Dongflix", "popular_dongflix"))
			put(textRomanceSeeAll, arrayOf("Donghua Romantis Tipis-Tipis", "romance"))
			put(textCultivationSeeAll, arrayOf("Kultivasi & Keabadian", "cultivation"))
			put(textCompletedSeeAll, arrayOf("Donghua Tamat", "completed"))
			put(textComedySeeAll, arrayOf("Donghua Komedi Ringan", "comedy"))
			put(textMysterySeeAll, arrayOf("Misteri & Teka-Teki", "mystery"))
			put(textMovieSeeAll, arrayOf("Donghua Movie", "movie"))
		}

		for ((tv, cfg) in seeAllConfig) {
			if (tv != null) {
				val sectionTitle = cfg[0]
				val filterKey = cfg[1]
				tv.setTextColor(Color.parseColor("#94A3B8"))
				tv.setOnClickListener {
					val intent = Intent(this@MainActivity, CatalogActivity::class.java).apply {
						putExtra("section_title", sectionTitle)
						putExtra("filter_type", filterKey)
					}
					startActivity(intent)
				}
			}
		}

		textContinueSeeAll?.setTextColor(Color.parseColor("#94A3B8"))

		imgTopNotif?.let {
			val notifId = resources.getIdentifier("ic_notifications", "drawable", packageName)
			it.setImageResource(if (notifId > 0) notifId else R.drawable.ic_search)
		}
		btnTopNotif?.setOnClickListener { openNotificationsSheet() }
		btnHeroFavorite?.setOnClickListener { currentHeroItem?.let { item -> toggleFavoriteItem(item) } }
		btnTopChat?.setOnClickListener { startActivity(Intent(this@MainActivity, ChatActivity::class.java)) }

		gridview1?.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
			if (pos < listMap.size) {
				val item = listMap[pos]
				if (!item.containsKey("is_loading")) openWatchBySlug(safeStr(item, "slug", ""))
			}
		}

		btnClearSearch?.setOnClickListener {
			inputSearch?.setText("")
			showKeyboardOnSearch()
		}

		inputSearch?.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
			override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
				val q = s?.toString()?.trim()?.lowercase(Locale.getDefault()) ?: ""
				btnClearSearch?.visibility = if (q.isEmpty()) View.GONE else View.VISIBLE
				if (q.isEmpty()) {
					textSearchSectionLabel?.apply {
						visibility = View.VISIBLE
						text = "Populer di Dongflix"
					}
					recyclerSearchList?.adapter = ListPopularAdapter(listMap)
				} else {
					textSearchSectionLabel?.visibility = View.GONE
					val filtered = ArrayList<HashMap<String, Any>>()
					for (item in fullCatalogList) {
						if (safeStr(item, "title", "").lowercase(Locale.getDefault()).contains(q) ||
							safeStr(item, "genres", "").lowercase(Locale.getDefault()).contains(q)
						) {
							filtered.add(item)
						}
					}
					recyclerSearchList?.adapter = ListPopularAdapter(filtered)
				}
			}
			override fun afterTextChanged(s: Editable?) {}
		})

		dongflixReqListener = object : RequestNetwork.RequestListener {
			override fun onResponse(p1: String, response: String, p3: HashMap<String, Any>) {
				if (isFinishing || isDestroyed) return
				stopMainShimmer()
				hideRefreshIndicator()
				DongflixPlayerManager.getInstance().cachedAllCatalogJson = response

				fullCatalogList = try {
					val type = object : TypeToken<ArrayList<HashMap<String, Any>>>() {}.type
					Gson().fromJson<ArrayList<HashMap<String, Any>>>(response, type) ?: ArrayList()
				} catch (e: Exception) {
					ArrayList()
				}

				listMap = ArrayList(fullCatalogList.subList(0, minOf(12, fullCatalogList.size)))
				gridview1?.let { gv ->
					gv.adapter = Gridview1Adapter(listMap)
					setGridViewHeightBasedOnChildren(gv, 3)
				}
				recyclerSearchList?.adapter = ListPopularAdapter(listMap)

				if (listMap.size >= 3) {
					textHeroTitle?.setBackgroundColor(Color.TRANSPARENT)
					textHeroDesc?.setBackgroundColor(Color.TRANSPARENT)
					imgHeroBanner?.setBackgroundColor(Color.TRANSPARENT)

					val playBg = GradientDrawable().apply {
						shape = GradientDrawable.RECTANGLE
						setColor(Color.WHITE)
						cornerRadius = 100f
					}

					btnHeroPlay?.apply {
						background = playBg
						visibility = View.VISIBLE
						isEnabled = true
					}
					btnHeroFavorite?.apply {
						visibility = View.VISIBLE
						isEnabled = true
						applyUniversalGlassStyle(this, this)
					}
					imgPlayIcon?.visibility = View.VISIBLE
					indicatorCapsule?.visibility = View.VISIBLE
					startHeroAutoSlider()
				}

				bindSectionLikePopularToday(recyclerPopular, listMap)
				bindSectionLikePopularToday(recyclerPopularDongflix, listMap)
				populateCategorizedSections()
				sortAndRenderPopularSections()
				applyOutfitFontsWithoutBold()
			}

			override fun onErrorResponse(p1: String, p2: String) {
				if (isFinishing || isDestroyed) return
				stopMainShimmer()
				hideRefreshIndicator()
			}
		}
	}

	private fun initializeLogic() {
		_UI()
		setupIos26DraggableNavbar()
		_Nav()
		setupScrollResponsiveNavbar()
		applyOutfitFontsWithoutBold()

		val d = resources.displayMetrics.density
		val dummy = buildEmptyDummyList(12)

		gridview1?.let { gv ->
			gv.adapter = Gridview1Adapter(dummy)
			gv.setOnTouchListener { _, event -> event.action == MotionEvent.ACTION_MOVE }
			setGridViewHeightBasedOnChildren(gv, 3)
		}
		textHeroTitle?.apply {
			text = ""
			background = createMainShimmerDrawable(8 * d)
		}
		textHeroDesc?.apply {
			text = ""
			background = createMainShimmerDrawable(6 * d)
		}
		imgHeroBanner?.apply {
			setImageResource(android.R.color.transparent)
			background = createMainShimmerDrawable(0f)
		}
		btnHeroPlay?.apply { visibility = View.GONE; isEnabled = false }
		btnHeroFavorite?.apply { visibility = View.GONE; isEnabled = false }
		indicatorCapsule?.visibility = View.INVISIBLE
		imgPlayIcon?.visibility = View.GONE

		val standardList = arrayOf(
			recyclerPopular, recyclerPopularDongflix, recyclerRomanceDonghua,
			recyclerCultivationDonghua, recyclerCompletedDonghua,
			recyclerComedyDonghua, recyclerMysteryDonghua, recyclerMovieDonghua
		)
		for (rv in standardList) bindSectionLikePopularToday(rv, dummy)

		recyclerSearchList?.apply {
			layoutManager = LinearLayoutManager(this@MainActivity)
			adapter = ListPopularAdapter(dummy)
		}

		startMainShimmer()
		dongflixReq.startRequestNetwork("GET", CATALOG_URL, "", dongflixReqListener)
	}

	private fun setupRealTimeBlurView(blurTarget: View?, root: ViewGroup?, radius: Float) {
		if (blurTarget !is BlurView || root == null) return
		blurTarget.setupWith(root, RenderScriptBlur(this))
			.setFrameClearDrawable(window.decorView.background)
			.setBlurRadius(radius)
		blurTarget.outlineProvider = object : ViewOutlineProvider() {
			override fun getOutline(view: View, outline: Outline) {
				outline.setRoundRect(0, 0, view.width, view.height, 1000f)
			}
		}
		blurTarget.clipToOutline = true
	}

	private fun applyUniversalGlassStyle(blurTarget: View?, containerTarget: View?) {
		val d = resources.displayMetrics.density
		if (blurTarget != null && blurTarget !is BlurView) {
			blurTarget.background = GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				intArrayOf(Color.parseColor("#B3161923"), Color.parseColor("#CC0A0C12"))
			).apply {
				cornerRadius = 100f
				setStroke((1 * d).toInt(), Color.parseColor("#33FFFFFF"))
			}
		}
		containerTarget?.let {
			it.foreground = GradientDrawable().apply {
				setColor(Color.TRANSPARENT)
				cornerRadius = 1000f
				setStroke((1 * d).toInt(), Color.parseColor("#33FFFFFF"))
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) it.elevation = 18f
		}
	}

	private fun setupIos26DraggableNavbar() {
		val nav = navbarExpanded ?: return
		if (tabBeranda == null || nav.childCount < 2) return
		val tabsRow = nav.getChildAt(1) as? ViewGroup ?: return
		val d = resources.displayMetrics.density

		if (liquidDragPill == null) {
			liquidDragPill = View(this).apply {
				background = GradientDrawable(
					GradientDrawable.Orientation.TOP_BOTTOM,
					intArrayOf(Color.parseColor("#38FFFFFF"), Color.parseColor("#22FFFFFF"))
				).apply {
					cornerRadius = 100f
				}
				layoutParams = FrameLayout.LayoutParams(
					(96 * d).toInt(),
					(54 * d).toInt(),
					Gravity.CENTER_VERTICAL or Gravity.START
				).apply {
					leftMargin = (5 * d).toInt()
				}
			}
			nav.addView(liquidDragPill, 1)
		}
		tabsRow.post { snapPillToTab(activeNavTabIndex, false) }
	}

	private fun snapPillToTab(idx: Int, animate: Boolean) {
		val pill = liquidDragPill ?: return
		val b = tabBeranda ?: return
		val p = tabPustaka ?: return
		val a = tabAkun ?: return
		val target = when (idx) {
			1 -> p
			2 -> a
			else -> b
		}
		val d = resources.displayMetrics.density

		var targetW = target.width
		val navW = navbarExpanded?.width ?: 0
		if (targetW <= 0 && navW > 0) {
			targetW = (navW - (10 * d).toInt()) / 3
		}
		if (targetW <= 0) targetW = (96 * d).toInt()

		val lp = pill.layoutParams
		if (lp != null && lp.width != targetW) {
			lp.width = targetW
			pill.layoutParams = lp
		}

		val tx = if (target.left > 0) maxOf(0f, target.left - (5 * d)) else (idx * targetW).toFloat()
		if (animate) {
			pill.animate().translationX(tx).setInterpolator(OvershootInterpolator(1.05f)).setDuration(220L).start()
		} else {
			pill.translationX = tx
		}
	}

	private fun selectNavTabByIndex(idx: Int, animatePill: Boolean) {
		activeNavTabIndex = idx
		snapPillToTab(idx, animatePill)

		val pages = arrayOf(
			findViewById<View>(R.id.main_scroll_view),
			findViewById<View>(R.id.layout_favorite),
			findViewById<View>(R.id.layout_profile),
			findViewById<View>(R.id.layout_search)
		)
		for (p in pages) p?.visibility = View.GONE

		val active = Color.WHITE
		val inactive = Color.parseColor("#94A3B8")
		imgHome?.setColorFilter(if (idx == 0) active else inactive)
		imgFavorite?.setColorFilter(if (idx == 1) active else inactive)
		imgProfile?.setColorFilter(if (idx == 2) active else inactive)
		imgSearch?.setColorFilter(inactive)

		txtHome?.setTextColor(if (idx == 0) active else inactive)
		txtFavorite?.setTextColor(if (idx == 1) active else inactive)
		txtProfile?.setTextColor(if (idx == 2) active else inactive)

		hideKeyboard()
		when (idx) {
			0 -> {
				pages[0]?.visibility = View.VISIBLE
				setNavbarCompactState(false)
			}
			1 -> {
				pages[1]?.visibility = View.VISIBLE
				renderActivePustakaTab()
			}
			else -> {
				pages[2]?.visibility = View.VISIBLE
				refreshAccountUi()
			}
		}
	}

	fun _UI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			window.apply {
				clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
				addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
				@Suppress("DEPRECATION")
				decorView.systemUiVisibility =
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				statusBarColor = Color.TRANSPARENT
			}
		}

		var statusH = 0
		val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
		if (resId > 0) statusH = resources.getDimensionPixelSize(resId)
		val d = resources.displayMetrics.density

		findViewById<View>(R.id.relativelayout69)?.setPadding(
			(16 * d).toInt(),
			statusH + (8 * d).toInt(),
			(16 * d).toInt(),
			(10 * d).toInt()
		)

		applyUniversalGlassStyle(btnTopNotif, btnTopNotif)
		applyUniversalGlassStyle(btnTopChat, btnTopChat)
		imgTopNotif?.setColorFilter(Color.WHITE)
		imgTopChat?.setColorFilter(Color.WHITE)
		imgHeroFavorite?.setColorFilter(Color.WHITE)

		searchBarContainer?.let { sb ->
			val sbLp = sb.layoutParams as? LinearLayout.LayoutParams
			if (sbLp != null) {
				sbLp.topMargin = statusH + (12 * d).toInt()
				sb.layoutParams = sbLp
			}
			sb.background = GradientDrawable().apply {
				setColor(Color.parseColor("#12141A"))
				cornerRadius = 100f
				setStroke((1 * d).toInt(), Color.parseColor("#26FFFFFF"))
			}
		}

		btnClearSearch?.background = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(Color.parseColor("#252936"))
		}

		findViewById<View>(R.id.header_shadow)?.background = GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			intArrayOf(Color.parseColor("#E6000000"), Color.TRANSPARENT)
		)
		findViewById<View>(R.id.hero_shadow)?.background = GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			intArrayOf(Color.TRANSPARENT, Color.parseColor("#FF000000"))
		)

		val rootMain = findViewById<ViewGroup>(R.id.root_main_container)
		val navBlur = findViewById<View>(R.id.navbar_blur_bg)
		val compactBlur = findViewById<View>(R.id.compact_home_blur_bg)
		val searchBlur = findViewById<View>(R.id.search_blur_bg)

		setupRealTimeBlurView(navBlur, rootMain, 20f)
		setupRealTimeBlurView(compactBlur, rootMain, 20f)
		setupRealTimeBlurView(searchBlur, rootMain, 20f)

		applyUniversalGlassStyle(navBlur, navbarExpanded)
		applyUniversalGlassStyle(compactBlur, btnCompactHome)
		applyUniversalGlassStyle(searchBlur, btnCircleSearch)
	}

	private fun setupScrollResponsiveNavbar() {
		val mainScroll = findViewById<NestedScrollView>(R.id.main_scroll_view) ?: return
		mainScroll.setOnScrollChangeListener(
			NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldY ->
				if (!DongflixPlayerManager.getInstance().isMiniPlayerActive) {
					resetNavbarToExpanded()
					return@OnScrollChangeListener
				}
				val dy = scrollY - oldY
				if (scrollY <= 20 || dy < -12) setNavbarCompactState(false)
				else if (dy > 14 && scrollY > 80) setNavbarCompactState(true)
			}
		)
	}

	fun resetNavbarToExpanded() {
		setNavbarCompactState(false)
	}

	private fun setNavbarCompactState(compact: Boolean) {
		var targetCompact = compact
		if (targetCompact && !DongflixPlayerManager.getInstance().isMiniPlayerActive) targetCompact = false
		if (isNavbarCompact == targetCompact) return
		isNavbarCompact = targetCompact

		if (targetCompact) {
			navbarExpanded?.visibility = View.GONE
			btnCompactHome?.visibility = View.VISIBLE
		} else {
			btnCompactHome?.visibility = View.GONE
			miniPlayerInlineSlot?.visibility = View.GONE
			navbarExpanded?.visibility = View.VISIBLE
			snapPillToTab(activeNavTabIndex, false)
		}
		DongflixPlayerManager.getInstance().setCompactScrollMode(this, targetCompact)
	}

	fun _Nav() {
		val pageHome = findViewById<View>(R.id.main_scroll_view)
		val pageSearch = findViewById<View>(R.id.layout_search)
		val pageFavorite = findViewById<View>(R.id.layout_favorite)
		val pageProfile = findViewById<View>(R.id.layout_profile)

		btnCompactHome?.setOnClickListener { selectNavTabByIndex(0, true) }
		btnCircleSearch?.setOnClickListener {
			pageHome?.visibility = View.GONE
			pageFavorite?.visibility = View.GONE
			pageProfile?.visibility = View.GONE
			pageSearch?.visibility = View.VISIBLE
			imgSearch?.setColorFilter(Color.WHITE)
			showKeyboardOnSearch()
		}
		selectNavTabByIndex(0, false)
	}

	fun setGridViewHeightBasedOnChildren(gv: GridView, columns: Int) {
		gv.post {
			val gridAdapter = gv.adapter ?: return@post
			if (gridAdapter.count == 0) return@post
			val items = gridAdapter.count
			val rows = ceil(items.toDouble() / columns).toInt()
			val d = resources.displayMetrics.density
			val hGap = (12 * d).toInt()
			val vGap = (16 * d).toInt()
			val padBot = (16 * d).toInt()
			val colW = (gv.width - gv.paddingLeft - gv.paddingRight - (hGap * (columns - 1))) / columns
			if (colW <= 0) return@post

			val itemV = gridAdapter.getView(0, null, gv)
			itemV.measure(
				View.MeasureSpec.makeMeasureSpec(colW, View.MeasureSpec.EXACTLY),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
			)

			val lp = gv.layoutParams
			lp.height = (itemV.measuredHeight * rows) + (vGap * maxOf(0, rows - 1)) + padBot
			gv.layoutParams = lp
			gv.requestLayout()
		}
	}

	inner class ContinueWatchingAdapter(
		private val data: ArrayList<HashMap<String, Any>>
	) : RecyclerView.Adapter<ContinueWatchingAdapter.ViewHolder>() {

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val d = parent.resources.displayMetrics.density
			val card = FrameLayout(parent.context).apply {
				layoutParams = RecyclerView.LayoutParams((245 * d).toInt(), (124 * d).toInt()).apply {
					rightMargin = (12 * d).toInt()
				}
				background = GradientDrawable(
					GradientDrawable.Orientation.LEFT_RIGHT,
					intArrayOf(Color.parseColor("#071830"), Color.parseColor("#0C2847"))
				).apply {
					cornerRadius = 16 * d
				}
				clipToOutline = true
			}

			val thumb = ImageView(parent.context).apply {
				layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
				scaleType = ImageView.ScaleType.CENTER_CROP
				alpha = 0.7f
				tag = "thumb"
			}
			card.addView(thumb)

			val scrim = View(parent.context).apply {
				layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
				background = GradientDrawable(
					GradientDrawable.Orientation.BOTTOM_TOP,
					intArrayOf(Color.parseColor("#E6030A14"), Color.parseColor("#66030A14"), Color.TRANSPARENT)
				)
			}
			card.addView(scrim)

			val col = LinearLayout(parent.context).apply {
				orientation = LinearLayout.VERTICAL
				setPadding((14 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
				layoutParams = FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
				)
			}

			val tvTitle = TextView(parent.context).apply {
				setTextColor(Color.WHITE)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				isSingleLine = true
				maxLines = 1
				ellipsize = TextUtils.TruncateAt.END
				tag = "title"
			}

			val tvEp = TextView(parent.context).apply {
				setTextColor(Color.parseColor("#CBD5E1"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				maxLines = 1
				ellipsize = TextUtils.TruncateAt.END
				tag = "ep"
			}

			col.addView(tvTitle)
			col.addView(tvEp)
			card.addView(col)

			val barTrack = FrameLayout(parent.context).apply {
				layoutParams = FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, maxOf(2, (2 * d).toInt()), Gravity.BOTTOM or Gravity.START
				)
				setBackgroundColor(Color.parseColor("#26FFFFFF"))
			}

			val barFill = View(parent.context).apply {
				layoutParams = FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
				setBackgroundColor(Color.parseColor("#38BDF8"))
				tag = "bar_fill"
			}

			barTrack.addView(barFill)
			card.addView(barTrack)

			return ViewHolder(card)
		}

		override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
			val item = data[pos]
			val thumb = holder.itemView.findViewWithTag<ImageView>("thumb")
			val tvTitle = holder.itemView.findViewWithTag<TextView>("title")
			val tvEp = holder.itemView.findViewWithTag<TextView>("ep")
			val barFill = holder.itemView.findViewWithTag<View>("bar_fill")

			val epRaw = safeStr(item, "episode", "1").trim()
			var cov = normalizeCoverUrl(safeStr(item, "cover", ""))
			if (cov.isEmpty()) cov = normalizeCoverUrl(safeStr(item, "poster", ""))

			tvTitle?.text = cleanText(safeStr(item, "title", "Donghua"))
			tvEp?.text = if (epRaw.uppercase(Locale.US).startsWith("EP")) {
				epRaw.uppercase(Locale.US)
			} else {
				"EP $epRaw"
			}
			if (thumb != null && cov.isNotEmpty() && !isFinishing && !isDestroyed) {
				Glide.with(this@MainActivity).load(Uri.parse(cov)).into(thumb)
			}

			val posMs = parseLongSafe(item["positionMs"])
			val durMs = parseLongSafe(item["durationMs"])
			val parentTrack = barFill?.parent as? View
			parentTrack?.post {
				val totalW = parentTrack.width
				if (totalW <= 0) return@post
				val d = barFill.resources.displayMetrics.density
				val segW = maxOf((18 * d).toInt(), (totalW * 0.14f).roundToInt())
				val leftPos = if (durMs > 0L && posMs >= 0L) {
					val ratio = maxOf(0f, minOf(1f, posMs.toFloat() / durMs.toFloat()))
					((totalW - segW) * ratio).roundToInt()
				} else {
					maxOf(0, (totalW - segW) / 2)
				}
				barFill.layoutParams = FrameLayout.LayoutParams(segW, ViewGroup.LayoutParams.MATCH_PARENT).apply {
					leftMargin = leftPos
				}
			}

			holder.itemView.setOnClickListener { openWatchBySlug(safeStr(item, "slug", "")) }
		}

		override fun getItemCount(): Int = data.size

		inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
	}

	inner class HorizontalPosterAdapter(
		private val data: ArrayList<HashMap<String, Any>>,
		private val cardWidthDp: Int,
		private val cardHeightDp: Int
	) : RecyclerView.Adapter<HorizontalPosterAdapter.ViewHolder>() {

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val d = parent.resources.displayMetrics.density
			val wPx = (cardWidthDp * d).toInt()
			val hPx = (cardHeightDp * d).toInt()

			val root = LinearLayout(parent.context).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams = RecyclerView.LayoutParams(wPx, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
					rightMargin = (10 * d).toInt()
				}
			}

			val card = CardView(parent.context).apply {
				layoutParams = LinearLayout.LayoutParams(wPx, hPx)
				radius = 16 * d
				cardElevation = 0f
				setCardBackgroundColor(Color.parseColor("#12141A"))
			}

			val overlay = FrameLayout(parent.context).apply {
				layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			}

			val imgCover = ImageView(parent.context).apply {
				layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
				scaleType = ImageView.ScaleType.CENTER_CROP
				tag = "img_cover"
			}
			overlay.addView(imgCover)

			val scrim = View(parent.context).apply {
				layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (60 * d).toInt(), Gravity.BOTTOM)
				tag = "bottom_scrim"
			}
			overlay.addView(scrim)

			val tvStatus = TextView(parent.context).apply {
				layoutParams = FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START
				).apply {
					setMargins((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), 0)
				}
				setPadding((8 * d).toInt(), (3 * d).toInt(), (8 * d).toInt(), (3 * d).toInt())
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
				setTextColor(Color.WHITE)
				maxLines = 1
				tag = "text_status"
			}
			overlay.addView(tvStatus)

			val tvEp = TextView(parent.context).apply {
				layoutParams = FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.START
				).apply {
					setMargins((9 * d).toInt(), 0, (9 * d).toInt(), (8 * d).toInt())
				}
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
				setTextColor(Color.WHITE)
				maxLines = 1
				ellipsize = TextUtils.TruncateAt.END
				tag = "text_episode"
			}
			overlay.addView(tvEp)

			card.addView(overlay)
			root.addView(card)

			val tvTitle = TextView(parent.context).apply {
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					topMargin = (7 * d).toInt()
				}
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
				setTextColor(Color.WHITE)
				includeFontPadding = false
				setLineSpacing(2 * d, 1.05f)
				minLines = 2
				maxLines = 2
				ellipsize = TextUtils.TruncateAt.END
				tag = "text_title"
			}
			root.addView(tvTitle)

			return ViewHolder(root)
		}

		override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
			val v = holder.itemView
			val d = v.resources.displayMetrics.density
			val rlp = v.layoutParams as? RecyclerView.LayoutParams
			if (rlp != null) {
				rlp.rightMargin = if (pos == data.size - 1) 0 else (10 * d).toInt()
				v.layoutParams = rlp
			}

			val imgCover = v.findViewWithTag<ImageView>("img_cover")
			val scrim = v.findViewWithTag<View>("bottom_scrim")
			val tvStatus = v.findViewWithTag<TextView>("text_status")
			val tvEp = v.findViewWithTag<TextView>("text_episode")
			val tvTitle = v.findViewWithTag<TextView>("text_title")

			val item = data[pos]

			if (item.containsKey("is_loading")) {
				imgCover?.apply {
					setImageResource(android.R.color.transparent)
					background = createMainShimmerDrawable(16 * d)
				}
				scrim?.visibility = View.GONE
				tvStatus?.apply { text = ""; visibility = View.INVISIBLE }
				tvEp?.apply { text = ""; visibility = View.INVISIBLE }
				tvTitle?.apply {
					text = ""
					minHeight = (36 * d).toInt()
					background = createMainShimmerDrawable(8 * d)
				}
				v.setOnClickListener(null)
			} else {
				imgCover?.setBackgroundColor(Color.TRANSPARENT)
				scrim?.apply {
					visibility = View.VISIBLE
					background = GradientDrawable(
						GradientDrawable.Orientation.BOTTOM_TOP,
						intArrayOf(Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT)
					)
				}
				tvStatus?.apply {
					visibility = View.VISIBLE
					background = GradientDrawable().apply {
						cornerRadius = 100f
						setColor(Color.parseColor("#3B82F6"))
					}
					text = safeStr(item, "status", "")
				}
				tvEp?.apply { visibility = View.VISIBLE; text = safeStr(item, "episode", "") }
				tvTitle?.apply {
					minHeight = 0
					setBackgroundColor(Color.TRANSPARENT)
					text = cleanText(safeStr(item, "title", ""))
				}

				val cov = normalizeCoverUrl(safeStr(item, "cover", ""))
				if (imgCover != null && cov.isNotEmpty() && !isFinishing && !isDestroyed) {
					Glide.with(this@MainActivity).load(Uri.parse(cov)).into(imgCover)
				}
				v.setOnClickListener { openWatchBySlug(safeStr(item, "slug", "")) }
			}
		}

		override fun getItemCount(): Int = data.size

		inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
	}

	inner class PustakaListAdapter(
		private val data: ArrayList<HashMap<String, Any>>,
		private val mode: Int
	) : RecyclerView.Adapter<PustakaListAdapter.ViewHolder>() {

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val ctx = parent.context
			val d = ctx.resources.displayMetrics.density

			val card = LinearLayout(ctx).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				setPadding((10 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
				layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
					bottomMargin = (10 * d).toInt()
				}
				background = GradientDrawable().apply {
					setColor(Color.parseColor("#12141A"))
					cornerRadius = 16 * d
					setStroke(maxOf(1, (1 * d).toInt()), Color.parseColor("#1AFFFFFF"))
				}
			}

			val thumbCard = CardView(ctx).apply {
				layoutParams = LinearLayout.LayoutParams((64 * d).toInt(), (86 * d).toInt()).apply {
					rightMargin = (12 * d).toInt()
				}
				radius = 12 * d
				cardElevation = 0f
				setCardBackgroundColor(Color.parseColor("#181C26"))
			}

			val thumb = ImageView(ctx).apply {
				layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
				scaleType = ImageView.ScaleType.CENTER_CROP
				tag = "thumb"
			}
			thumbCard.addView(thumb)

			val info = LinearLayout(ctx).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
			}

			val tvTitle = TextView(ctx).apply {
				setTextColor(Color.WHITE)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				maxLines = 2
				ellipsize = TextUtils.TruncateAt.END
				tag = "title"
			}

			val tvBadge = TextView(ctx).apply {
				setTextColor(Color.parseColor("#38BDF8"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				setPadding((8 * d).toInt(), (2 * d).toInt(), (8 * d).toInt(), (2 * d).toInt())
				layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
					topMargin = (6 * d).toInt()
				}
				background = GradientDrawable().apply {
					setColor(Color.parseColor("#0C2438"))
					cornerRadius = 100f
				}
				tag = "ep"
			}

			val tvSub = TextView(ctx).apply {
				setTextColor(Color.parseColor("#94A3B8"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
					topMargin = (4 * d).toInt()
				}
				tag = "sub"
			}

			info.addView(tvTitle)
			info.addView(tvBadge)
			info.addView(tvSub)

			val btnRemove = TextView(ctx).apply {
				text = "✕"
				setTextColor(Color.parseColor("#94A3B8"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				gravity = Gravity.CENTER
				layoutParams = LinearLayout.LayoutParams((30 * d).toInt(), (30 * d).toInt()).apply {
					leftMargin = (8 * d).toInt()
				}
				background = GradientDrawable().apply {
					shape = GradientDrawable.OVAL
					setColor(Color.parseColor("#181C26"))
				}
				tag = "btn_remove"
			}

			card.addView(thumbCard)
			card.addView(info)
			card.addView(btnRemove)
			return ViewHolder(card)
		}

		override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
			val item = data[pos]
			val img = holder.itemView.findViewWithTag<ImageView>("thumb")
			val tvTitle = holder.itemView.findViewWithTag<TextView>("title")
			val tvEp = holder.itemView.findViewWithTag<TextView>("ep")
			val tvSub = holder.itemView.findViewWithTag<TextView>("sub")
			val btnRemove = holder.itemView.findViewWithTag<TextView>("btn_remove")

			val title = cleanText(safeStr(item, "title", if (mode == 2) "Video Offline" else "Donghua"))
			tvTitle?.text = title

			when (mode) {
				0 -> {
					val epRaw = safeStr(item, "episode", "1").trim()
					tvEp?.text = if (epRaw.uppercase(Locale.US).startsWith("EP")) {
						epRaw.uppercase(Locale.US)
					} else {
						"EP $epRaw"
					}
					tvSub?.text = safeStr(item, "remainingText", "Lanjutkan menonton")
				}
				1 -> {
					val ep = safeStr(item, "episode", "")
					tvEp?.text = if (ep.isEmpty()) "Favorit" else ep
					tvSub?.text = safeStr(item, "status", "Tersimpan di Daftarku")
				}
				else -> {
					tvEp?.text = "OFFLINE • ${safeStr(item, "size_mb", "")}"
					tvSub?.text = "Ketuk untuk memutar video"
				}
			}

			btnRemove?.setOnClickListener { removePustakaItem(mode, item) }

			if (mode == 2) {
				img?.setImageResource(R.drawable.default_image)
				val path = safeStr(item, "file_path", "")
				holder.itemView.setOnClickListener { openDownloadedVideoSafely(path, title) }
			} else {
				var cov = normalizeCoverUrl(safeStr(item, "cover", ""))
				if (cov.isEmpty()) cov = normalizeCoverUrl(safeStr(item, "poster", ""))
				if (img != null && cov.isNotEmpty() && !isFinishing && !isDestroyed) {
					Glide.with(this@MainActivity).load(Uri.parse(cov)).into(img)
				}
				holder.itemView.setOnClickListener { openWatchBySlug(safeStr(item, "slug", "")) }
			}
		}

		override fun getItemCount(): Int = data.size

		inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
	}

	inner class Gridview1Adapter(
		private val data: ArrayList<HashMap<String, Any>>
	) : BaseAdapter() {

		override fun getCount(): Int = data.size
		override fun getItem(idx: Int): HashMap<String, Any> = data[idx]
		override fun getItemId(idx: Int): Long = idx.toLong()

		override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
			val v = layoutInflater.inflate(R.layout.custom_anime, parent, false)
			val d = v.resources.displayMetrics.density

			val tvTitle = v.findViewById<TextView>(R.id.text_title)
			val tvStatus = v.findViewById<TextView>(R.id.text_status)
			val tvEp = v.findViewById<TextView>(R.id.text_episode)
			val imgCover = v.findViewById<ImageView>(R.id.img_cover)
			val view2 = v.findViewById<View>(R.id.view2)

			val item = data[pos]
			if (item.containsKey("is_loading")) {
				tvTitle?.apply { text = ""; background = createMainShimmerDrawable(8 * d) }
				tvStatus?.apply { text = ""; background = createMainShimmerDrawable(50f) }
				tvEp?.apply { text = ""; background = createMainShimmerDrawable(6 * d) }
				imgCover?.apply {
					setImageResource(android.R.color.transparent)
					background = createMainShimmerDrawable(16 * d)
				}
				view2?.visibility = View.GONE
			} else {
				imgCover?.setBackgroundColor(Color.TRANSPARENT)
				tvTitle?.apply {
					setBackgroundColor(Color.TRANSPARENT)
					includeFontPadding = false
					text = cleanText(safeStr(item, "title", ""))
				}
				tvEp?.apply {
					visibility = View.VISIBLE
					setBackgroundColor(Color.TRANSPARENT)
					text = safeStr(item, "episode", "")
				}
				view2?.apply {
					visibility = View.VISIBLE
					background = GradientDrawable(
						GradientDrawable.Orientation.BOTTOM_TOP,
						intArrayOf(Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT)
					)
				}
				tvStatus?.apply {
					background = GradientDrawable().apply {
						cornerRadius = 50f
						setColor(0xFF3B82F6.toInt())
					}
					text = safeStr(item, "status", "")
				}
				val cov = normalizeCoverUrl(safeStr(item, "cover", ""))
				if (imgCover != null && cov.isNotEmpty() && !isFinishing && !isDestroyed) {
					Glide.with(this@MainActivity).load(Uri.parse(cov)).into(imgCover)
				}
			}
			return v
		}
	}

	inner class ListPopularAdapter(
		private val data: ArrayList<HashMap<String, Any>>
	) : RecyclerView.Adapter<ListPopularAdapter.ViewHolder>() {

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val v = layoutInflater.inflate(R.layout.item_popular_donghua, parent, false)
			v.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			return ViewHolder(v)
		}

		override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
			val v = holder.itemView
			val d = v.resources.displayMetrics.density

			val tvTitle = v.findViewById<TextView>(resources.getIdentifier("text_popular_title", "id", packageName))
			val tvDesc = v.findViewById<TextView>(resources.getIdentifier("text_popular_desc", "id", packageName))
			val tvGenre = v.findViewById<TextView>(resources.getIdentifier("text_popular_genre", "id", packageName))
			val tvStatus = v.findViewById<TextView>(resources.getIdentifier("text_popular_status", "id", packageName))
			val imgCover = v.findViewById<ImageView>(resources.getIdentifier("img_popular_cover", "id", packageName))
			val linear35 = v.findViewById<View>(resources.getIdentifier("linear35", "id", packageName))

			val item = data[pos]

			if (item.containsKey("is_loading")) {
				tvTitle?.apply { text = ""; background = createMainShimmerDrawable(100f) }
				tvDesc?.apply { text = ""; background = createMainShimmerDrawable(6 * d) }
				tvGenre?.apply { text = ""; background = createMainShimmerDrawable(6 * d) }
				tvStatus?.apply { text = "                    "; background = createMainShimmerDrawable(100f) }
				imgCover?.apply {
					setImageResource(android.R.color.transparent)
					background = createMainShimmerDrawable(16 * d)
				}
			} else {
				tvTitle?.apply { setBackgroundColor(Color.TRANSPARENT); text = cleanText(safeStr(item, "title", "")) }
				tvDesc?.apply { setBackgroundColor(Color.TRANSPARENT); text = cleanText(safeStr(item, "synopsis", "")) }
				tvGenre?.apply {
					setBackgroundColor(Color.TRANSPARENT)
					text = safeStr(item, "genres", "").replace("[", "").replace("]", "")
				}
				imgCover?.setBackgroundColor(Color.TRANSPARENT)

				tvStatus?.apply {
					background = GradientDrawable().apply {
						cornerRadius = 50f
						setColor(0xFF111111.toInt())
					}
					text = "${safeStr(item, "episode", "")} / ${safeStr(item, "status", "")}"
				}
				val cov = normalizeCoverUrl(safeStr(item, "cover", ""))
				if (imgCover != null && cov.isNotEmpty() && !isFinishing && !isDestroyed) {
					Glide.with(this@MainActivity).load(Uri.parse(cov)).into(imgCover)
				}
				val clickTarget = linear35 ?: v
				clickTarget.setOnClickListener { openWatchBySlug(safeStr(item, "slug", "")) }
			}
		}

		override fun getItemCount(): Int = data.size

		inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
	}
}
