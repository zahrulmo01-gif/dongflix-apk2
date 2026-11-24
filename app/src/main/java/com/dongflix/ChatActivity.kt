package com.dongflix

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
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
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

	companion object {
		private const val FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app"
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
	}

	private var currentUser: FirebaseUser? = null
	private lateinit var liveChatRef: DatabaseReference
	private lateinit var usersRef: DatabaseReference
	private var chatListener: ValueEventListener? = null

	private var chatRoot: FrameLayout? = null
	private var chatHeader: LinearLayout? = null
	private var chatBottomBar: LinearLayout? = null
	private var chatScrollView: ScrollView? = null
	private var messagesContainer: LinearLayout? = null
	private var replyBanner: LinearLayout? = null
	private var replyTargetAuthor: TextView? = null
	private var replyTargetSnippet: TextView? = null
	private var stickerPreviewBox: LinearLayout? = null
	private var stickerPreviewImg: ImageView? = null
	private var mentionMenu: LinearLayout? = null
	private var chatInput: EditText? = null
	private var stickerPanel: LinearLayout? = null
	private var stickerGridContainer: LinearLayout? = null

	private val userProfileCache = HashMap<String, HashMap<String, Any>>()
	private val allUsersList = ArrayList<HashMap<String, Any>>()
	private val stickerUrls = ArrayList<String>()
	private val messageRowMap = LinkedHashMap<String, View>()

	private var selectedStickerUrl = ""
	private var replyToId = ""
	private var replyToAuthor = ""
	private var replyToSnippet = ""

	private var wasMiniPlayerActiveBeforeChat = false
	private var wasPlayingBeforeChat = false
	private var baseBottomPaddingPx = 0

	private var chatShimmerAnimator: ValueAnimator? = null
	private var chatShimmerPhase = 0f
	private var isChatLoading = true

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		currentUser = FirebaseAuth.getInstance().currentUser
		if (currentUser == null) {
			startActivity(Intent(this, LoginActivity::class.java))
			finish()
			return
		}

		pauseAndHideMiniPlayerForChat()

		val db = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
		liveChatRef = db.getReference("live_chat")
		usersRef = db.getReference("users")

		buildActivityLayout()
		setupKeyboardFollowBehavior()
		showLoadingPlaceholders()
		loadAllUsersDirectory()
		loadStickersFromFirebase()
		startListeningMessages()
	}

	private fun pauseAndHideMiniPlayerForChat() {
		val mgr = DongflixPlayerManager.getInstance()
		wasMiniPlayerActiveBeforeChat = mgr.isMiniPlayerActive
		mgr.removeMiniPlayerFromActivity(this)
		mgr.isMiniPlayerActive = false

		mgr.sharedMediaPlayer?.let { mp ->
			try {
				wasPlayingBeforeChat = mp.isPlaying
				if (wasPlayingBeforeChat) {
					mp.pause()
				}
			} catch (ignored: Exception) {
			}
		}
		mgr.updatePlaybackNotification(this)
	}

	private fun restoreMiniPlayerAfterChat() {
		val mgr = DongflixPlayerManager.getInstance()
		if (wasMiniPlayerActiveBeforeChat && mgr.sharedMediaPlayer != null) {
			mgr.isMiniPlayerActive = true
			mgr.attachMiniPlayerToActivity(this)
			if (wasPlayingBeforeChat) {
				try {
					if (mgr.sharedMediaPlayer?.isPlaying == false) {
						mgr.sharedMediaPlayer?.start()
					}
				} catch (ignored: Exception) {
				}
			}
			mgr.updatePlaybackNotification(this)
		}
	}

	private fun setupGlassBlur(blurView: BlurView?, rootContainer: ViewGroup?, radius: Float, strokeColor: Int) {
		if (blurView == null || rootContainer == null) return
		val d = resources.displayMetrics.density

		blurView.outlineProvider = object : ViewOutlineProvider() {
			override fun getOutline(view: View, outline: Outline) {
				outline.setRoundRect(0, 0, view.width, view.height, 1000f)
			}
		}
		blurView.clipToOutline = true

		val winBg = window.decorView.background
		blurView.setupWith(rootContainer, RenderScriptBlur(this))
			.setFrameClearDrawable(winBg)
			.setBlurRadius(radius)
		blurView.setOverlayColor(Color.parseColor("#66141824"))

		blurView.foreground = GradientDrawable().apply {
			setColor(Color.TRANSPARENT)
			cornerRadius = 1000f
			setStroke((1 * d).toInt(), strokeColor)
		}
	}

	private fun createChatShimmerDrawable(cornerRadiusPx: Float): Drawable {
		return object : Drawable() {
			private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
				color = Color.parseColor("#141822")
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

				val translateX = -w + (2f * w * chatShimmerPhase)
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

	private fun startChatShimmer() {
		isChatLoading = true
		stopChatShimmerOnly()
		chatShimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = 1200L
			repeatCount = ValueAnimator.INFINITE
			interpolator = LinearInterpolator()
			addUpdateListener { animation ->
				val container = messagesContainer ?: return@addUpdateListener
				if (!isChatLoading) return@addUpdateListener
				chatShimmerPhase = animation.animatedValue as Float
				invalidateShimmerRecursive(container)
			}
			start()
		}
	}

	private fun invalidateShimmerRecursive(v: View?) {
		if (v == null) return
		v.invalidate()
		if (v is ViewGroup) {
			for (k in 0 until v.childCount) {
				invalidateShimmerRecursive(v.getChildAt(k))
			}
		}
	}

	private fun stopChatShimmerOnly() {
		chatShimmerAnimator?.cancel()
		chatShimmerAnimator = null
	}

	private fun stopChatShimmer() {
		isChatLoading = false
		stopChatShimmerOnly()
	}

	private fun showLoadingPlaceholders() {
		val container = messagesContainer ?: return
		container.removeAllViews()
		isChatLoading = true

		val d = resources.displayMetrics.density
		val minePattern = booleanArrayOf(false, true, false, true, false)
		val lineWidthsDp = intArrayOf(195, 225, 175, 210, 185)

		for (i in minePattern.indices) {
			val isMine = minePattern[i]
			val row = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = if (isMine) Gravity.END else Gravity.START
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					bottomMargin = (12 * d).toInt()
				}
			}

			val avSize = (36 * d).toInt()
			val avatarPh = View(this).apply {
				layoutParams = LinearLayout.LayoutParams(avSize, avSize).apply {
					if (isMine) leftMargin = (8 * d).toInt() else rightMargin = (8 * d).toInt()
				}
				background = createChatShimmerDrawable(100f)
			}

			val bubblePh = LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
				background = createChatShimmerDrawable(18 * d)
				layoutParams = LinearLayout.LayoutParams(
					(lineWidthsDp[i] * d).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					gravity = if (isMine) Gravity.END else Gravity.START
				}
			}

			val line1 = View(this).apply {
				layoutParams = LinearLayout.LayoutParams((110 * d).toInt(), (10 * d).toInt())
				background = createChatShimmerDrawable(6 * d)
			}

			val line2 = View(this).apply {
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, (12 * d).toInt()
				).apply {
					topMargin = (8 * d).toInt()
				}
				background = createChatShimmerDrawable(6 * d)
			}

			bubblePh.addView(line1)
			bubblePh.addView(line2)

			if (isMine) {
				row.addView(bubblePh)
				row.addView(avatarPh)
			} else {
				row.addView(avatarPh)
				row.addView(bubblePh)
			}
			container.addView(row)
		}
		startChatShimmer()
	}

	private fun calculateLevel(exp: Long): Int {
		if (exp <= 0) return 1
		return ((exp - 1) / 1000L).toInt() + 1
	}

	private fun parseLongVal(o: Any?): Long {
		if (o == null) return 0L
		if (o is Number) return o.toLong()
		return try {
			o.toString().trim().toLong()
		} catch (e: Exception) {
			0L
		}
	}

	private fun formatClock(ts: Long): String {
		return SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date(ts))
	}

	private fun getDateKey(ts: Long): String {
		return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ts))
	}

	private fun getDateDividerLabel(ts: Long): String {
		val msgCal = Calendar.getInstance().apply { timeInMillis = ts }
		val nowCal = Calendar.getInstance()

		if (msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
			msgCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
		) {
			return "HARI INI"
		}

		val yestCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
		if (msgCal.get(Calendar.YEAR) == yestCal.get(Calendar.YEAR) &&
			msgCal.get(Calendar.DAY_OF_YEAR) == yestCal.get(Calendar.DAY_OF_YEAR)
		) {
			return "KEMARIN"
		}

		return SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(ts)).uppercase(Locale.US)
	}

	private fun setupKeyboardFollowBehavior() {
		val root = chatRoot ?: return
		val bottomBar = chatBottomBar ?: return
		val d = resources.displayMetrics.density
		baseBottomPaddingPx = (26 * d).toInt()

		@Suppress("DEPRECATION")
		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

		ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
			val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
			val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
			val bottomInset = maxOf(ime.bottom, nav.bottom)

			bottomBar.setPadding(
				(14 * d).toInt(),
				(6 * d).toInt(),
				(14 * d).toInt(),
				baseBottomPaddingPx + bottomInset
			)

			chatScrollView?.let { sv ->
				val extraScrollPad = (132 * d).toInt() + bottomInset
				sv.setPadding(sv.paddingLeft, sv.paddingTop, sv.paddingRight, extraScrollPad)
				if (ime.bottom > 0) {
					sv.postDelayed({ sv.fullScroll(View.FOCUS_DOWN) }, 90)
				}
			}
			insets
		}

		root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
			private var lastBottomInset = -1

			override fun onGlobalLayout() {
				val visibleRect = Rect()
				root.getWindowVisibleDisplayFrame(visibleRect)
				val screenHeight = root.rootView.height
				val heightDiff = screenHeight - (visibleRect.bottom - visibleRect.top)
				val keyboardHeight = if (heightDiff > (140 * d).toInt()) heightDiff else 0

				if (keyboardHeight != lastBottomInset) {
					lastBottomInset = keyboardHeight
					bottomBar.setPadding(
						(14 * d).toInt(),
						(6 * d).toInt(),
						(14 * d).toInt(),
						baseBottomPaddingPx + keyboardHeight
					)
					chatScrollView?.let { sv ->
						sv.setPadding(
							sv.paddingLeft,
							sv.paddingTop,
							sv.paddingRight,
							(132 * d).toInt() + keyboardHeight
						)
						if (keyboardHeight > 0) {
							sv.postDelayed({ sv.fullScroll(View.FOCUS_DOWN) }, 80)
						}
					}
				}
			}
		})

		chatInput?.setOnFocusChangeListener { _, hasFocus ->
			if (hasFocus) {
				chatScrollView?.postDelayed({ chatScrollView?.fullScroll(View.FOCUS_DOWN) }, 180)
			}
		}
	}

	private fun buildActivityLayout() {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			window.apply {
				clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
				addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
				statusBarColor = Color.TRANSPARENT
			}
		}

		val d = resources.displayMetrics.density
		var statusBarHeight = 0
		val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
		if (resId > 0) statusBarHeight = resources.getDimensionPixelSize(resId)

		val root = FrameLayout(this).apply {
			layoutParams = ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
			)
			setBackgroundColor(Color.parseColor("#050608"))
		}
		chatRoot = root

		val scrollView = ScrollView(this).apply {
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
			)
			setPadding(
				(14 * d).toInt(),
				statusBarHeight + (74 * d).toInt(),
				(14 * d).toInt(),
				(132 * d).toInt()
			)
			clipToPadding = false
			isVerticalScrollBarEnabled = false
		}
		chatScrollView = scrollView

		val msgContainer = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			)
		}
		messagesContainer = msgContainer
		scrollView.addView(msgContainer)
		root.addView(scrollView)

		val header = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			clipChildren = false
			clipToPadding = false
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP
			)
			setPadding((14 * d).toInt(), statusBarHeight + (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
		}
		chatHeader = header

		val backSize = (42 * d).toInt()
		val backBtn = FrameLayout(this).apply {
			layoutParams = LinearLayout.LayoutParams(backSize, backSize).apply {
				rightMargin = (10 * d).toInt()
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 14f
			setOnClickListener { finish() }
		}

		val backBlur = BlurView(this).apply {
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
			)
		}
		setupGlassBlur(backBlur, root, 20f, Color.parseColor("#33FFFFFF"))
		backBtn.addView(backBlur)

		val arrowSize = (20 * d).toInt()
		val backArrow = ImageView(this).apply {
			layoutParams = FrameLayout.LayoutParams(arrowSize, arrowSize, Gravity.CENTER)
			setImageResource(R.drawable.ic_back_arrow)
			setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
			scaleType = ImageView.ScaleType.FIT_CENTER
		}
		backBtn.addView(backArrow)

		val headerCapsule = FrameLayout(this).apply {
			layoutParams = LinearLayout.LayoutParams(0, (46 * d).toInt(), 1f)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 14f
		}

		val headerBlur = BlurView(this).apply {
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
			)
		}
		setupGlassBlur(headerBlur, root, 20f, Color.parseColor("#33FFFFFF"))
		headerCapsule.addView(headerBlur)

		val titleCol = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			gravity = Gravity.CENTER
			setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
		}

		val tvTitle = TextView(this).apply {
			text = "Live Chat"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
			setTypeface(null, Typeface.BOLD)
		}

		val tvOnline = TextView(this).apply {
			text = "online"
			setTextColor(Color.parseColor("#22C55E"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
			setTypeface(null, Typeface.BOLD)
		}

		titleCol.addView(tvTitle)
		titleCol.addView(tvOnline)
		headerCapsule.addView(
			titleCol,
			FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
		)

		header.addView(backBtn)
		header.addView(headerCapsule)
		root.addView(header)

		val bottomBar = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
			)
			setPadding((14 * d).toInt(), (6 * d).toInt(), (14 * d).toInt(), (26 * d).toInt())
			clipChildren = false
			clipToPadding = false
		}
		chatBottomBar = bottomBar

		val repBanner = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#E6131926"))
				cornerRadius = 14 * d
				setStroke((1 * d).toInt(), Color.parseColor("#2E38BDF8"))
			}
			setPadding((12 * d).toInt(), (8 * d).toInt(), (12 * d).toInt(), (8 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = (6 * d).toInt()
			}
			visibility = View.GONE
		}
		replyBanner = repBanner

		val replyTextCol = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
		}

		val repAuthor = TextView(this).apply {
			setTextColor(Color.parseColor("#38BDF8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
			setTypeface(null, Typeface.BOLD)
		}
		replyTargetAuthor = repAuthor

		val repSnippet = TextView(this).apply {
			setTextColor(Color.parseColor("#CBD5E1"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
			maxLines = 1
			ellipsize = TextUtils.TruncateAt.END
		}
		replyTargetSnippet = repSnippet

		replyTextCol.addView(repAuthor)
		replyTextCol.addView(repSnippet)

		val cancelReplyBtn = TextView(this).apply {
			text = "✕"
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
			setPadding((8 * d).toInt(), 0, (4 * d).toInt(), 0)
			setOnClickListener { clearReplyTarget() }
		}

		repBanner.addView(replyTextCol)
		repBanner.addView(cancelReplyBtn)
		bottomBar.addView(repBanner)

		val stPreviewBox = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			visibility = View.GONE
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = (6 * d).toInt()
			}
		}
		stickerPreviewBox = stPreviewBox

		val prevPx = (58 * d).toInt()
		val stPreviewImg = ImageView(this).apply {
			layoutParams = LinearLayout.LayoutParams(prevPx, prevPx)
			scaleType = ImageView.ScaleType.FIT_CENTER
		}
		stickerPreviewImg = stPreviewImg

		val removeStickerBtn = TextView(this).apply {
			text = "  ✕"
			setTextColor(Color.parseColor("#EF4444"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
			setTypeface(null, Typeface.BOLD)
			setOnClickListener {
				selectedStickerUrl = ""
				stickerPreviewBox?.visibility = View.GONE
			}
		}

		stPreviewBox.addView(stPreviewImg)
		stPreviewBox.addView(removeStickerBtn)
		bottomBar.addView(stPreviewBox)

		val mMenu = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#E60D1424"))
				cornerRadius = 14 * d
				setStroke((1 * d).toInt(), Color.parseColor("#26FFFFFF"))
			}
			setPadding((6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt())
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = (6 * d).toInt()
			}
			visibility = View.GONE
		}
		mentionMenu = mMenu
		bottomBar.addView(mMenu)

		val inputGlassFrame = FrameLayout(this).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, (52 * d).toInt()
			)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				elevation = 18f
			}
		}

		val inputBlur = BlurView(this).apply {
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
			)
		}
		setupGlassBlur(inputBlur, root, 20f, Color.parseColor("#33FFFFFF"))
		inputGlassFrame.addView(inputBlur)

		val inputRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding((16 * d).toInt(), (4 * d).toInt(), (6 * d).toInt(), (4 * d).toInt())
		}

		val inputEdit = EditText(this).apply {
			layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
			background = null
			setTextColor(Color.WHITE)
			hint = "Kirim pesan..."
			setHintTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
			isSingleLine = true
			addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
					updateMentionDropdown(s?.toString() ?: "")
				}
				override fun afterTextChanged(s: Editable?) {}
			})
		}
		chatInput = inputEdit

		val stBtnPx = (38 * d).toInt()
		val stickerBtn = FrameLayout(this).apply {
			layoutParams = LinearLayout.LayoutParams(stBtnPx, stBtnPx).apply {
				rightMargin = (6 * d).toInt()
			}
			background = GradientDrawable().apply {
				shape = GradientDrawable.OVAL
				setColor(Color.parseColor("#26FFFFFF"))
			}
			setOnClickListener { toggleStickerPanel() }
		}

		val stIconSize = (20 * d).toInt()
		val stickerIcon = ImageView(this).apply {
			layoutParams = FrameLayout.LayoutParams(stIconSize, stIconSize, Gravity.CENTER)
			setImageResource(R.drawable.ic_sticker)
			setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
			scaleType = ImageView.ScaleType.FIT_CENTER
		}
		stickerBtn.addView(stickerIcon)

		val sendPx = (40 * d).toInt()
		val sendBtn = FrameLayout(this).apply {
			layoutParams = LinearLayout.LayoutParams(sendPx, sendPx)
			background = GradientDrawable().apply {
				shape = GradientDrawable.OVAL
				setColor(Color.parseColor("#38BDF8"))
			}
			setOnClickListener { sendMessage() }
		}

		val sendIconSize = (18 * d).toInt()
		val sendIcon = ImageView(this).apply {
			layoutParams = FrameLayout.LayoutParams(sendIconSize, sendIconSize, Gravity.CENTER)
			setImageResource(R.drawable.ic_send)
			setColorFilter(Color.parseColor("#07101C"), PorterDuff.Mode.SRC_IN)
			scaleType = ImageView.ScaleType.FIT_CENTER
		}
		sendBtn.addView(sendIcon)

		inputRow.addView(inputEdit)
		inputRow.addView(stickerBtn)
		inputRow.addView(sendBtn)
		inputGlassFrame.addView(
			inputRow,
			FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
		)
		bottomBar.addView(inputGlassFrame)

		val stPanel = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#E60B0F1A"))
				cornerRadius = 16 * d
				setStroke((1 * d).toInt(), Color.parseColor("#24FFFFFF"))
			}
			setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
			visibility = View.GONE
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = (8 * d).toInt()
			}
		}
		stickerPanel = stPanel

		val stTitle = TextView(this).apply {
			text = "Pilih Stiker & GIF"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
			setTypeface(null, Typeface.BOLD)
			setPadding(0, 0, 0, (8 * d).toInt())
		}

		val stScroll = HorizontalScrollView(this).apply {
			isHorizontalScrollBarEnabled = false
		}
		val stGrid = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
		}
		stickerGridContainer = stGrid
		stScroll.addView(stGrid)

		stPanel.addView(stTitle)
		stPanel.addView(stScroll)
		bottomBar.addView(stPanel)

		root.addView(bottomBar)
		setContentView(root)
	}

	private fun toggleStickerPanel() {
		val panel = stickerPanel ?: return
		val nextVis = panel.visibility != View.VISIBLE
		panel.visibility = if (nextVis) View.VISIBLE else View.GONE
		if (nextVis) renderStickerRow()
	}

	private fun loadStickersFromFirebase() {
		stickerUrls.clear()
		Collections.addAll(stickerUrls, *DEFAULT_STICKERS)

		FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("stickers")
			.addListenerForSingleValueEvent(object : ValueEventListener {
				override fun onDataChange(snapshot: DataSnapshot) {
					if (!snapshot.exists()) return
					val fetched = ArrayList<String>()
					for (child in snapshot.children) {
						val value = child.value
						if (value is String && value.trim().isNotEmpty()) {
							fetched.add(value.trim())
						} else if (value is Map<*, *>) {
							val u = value["url"] ?: value["link"] ?: value["image"]
							if (u != null && u.toString().trim().isNotEmpty()) {
								fetched.add(u.toString().trim())
							}
						}
					}
					if (fetched.isNotEmpty()) {
						stickerUrls.clear()
						stickerUrls.addAll(fetched)
						if (stickerPanel?.visibility == View.VISIBLE) {
							renderStickerRow()
						}
					}
				}

				override fun onCancelled(error: DatabaseError) {}
			})
	}

	private fun renderStickerRow() {
		val grid = stickerGridContainer ?: return
		grid.removeAllViews()
		val d = resources.displayMetrics.density

		for (url in stickerUrls) {
			val boxSize = (68 * d).toInt()
			val box = FrameLayout(this).apply {
				layoutParams = LinearLayout.LayoutParams(boxSize, boxSize).apply {
					rightMargin = (8 * d).toInt()
				}
				background = GradientDrawable().apply {
					setColor(Color.parseColor("#141A29"))
					cornerRadius = 14 * d
					setStroke((1 * d).toInt(), Color.parseColor("#22FFFFFF"))
				}
				setOnClickListener {
					selectedStickerUrl = url
					val previewImg = stickerPreviewImg
					val previewBox = stickerPreviewBox
					if (previewImg != null && previewBox != null) {
						Glide.with(applicationContext).load(Uri.parse(url)).into(previewImg)
						previewBox.visibility = View.VISIBLE
					}
					stickerPanel?.visibility = View.GONE
				}
			}

			val pad = (6 * d).toInt()
			val img = ImageView(this).apply {
				setPadding(pad, pad, pad, pad)
				scaleType = ImageView.ScaleType.FIT_CENTER
			}
			Glide.with(applicationContext).load(Uri.parse(url)).into(img)

			box.addView(
				img,
				FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			)
			grid.addView(box)
		}
	}

	private fun loadAllUsersDirectory() {
		usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				allUsersList.clear()
				userProfileCache.clear()
				if (!snapshot.exists()) return
				for (child in snapshot.children) {
					val raw = child.value
					if (raw is Map<*, *>) {
						val uMap = HashMap<String, Any>()
						for ((k, v) in raw) {
							if (k != null && v != null) uMap[k.toString()] = v
						}
						val key = child.key ?: ""
						userProfileCache[key.lowercase(Locale.US)] = uMap
						allUsersList.add(uMap)
					}
				}
			}

			override fun onCancelled(error: DatabaseError) {}
		})
	}

	private fun updateMentionDropdown(fullText: String) {
		val menu = mentionMenu ?: return
		val cursor = chatInput?.selectionStart ?: fullText.length
		val before = fullText.substring(0, maxOf(0, minOf(cursor, fullText.length)))
		val atIdx = before.lastIndexOf('@')

		if (atIdx == -1 || (atIdx > 0 && !Character.isWhitespace(before[atIdx - 1]))) {
			menu.visibility = View.GONE
			return
		}

		val query = before.substring(atIdx + 1).lowercase(Locale.US)
		val matches = ArrayList<HashMap<String, Any>>()
		for (u in allUsersList) {
			val uname = u["username"]?.toString()?.lowercase(Locale.US) ?: ""
			val dname = u["name"]?.toString()?.lowercase(Locale.US) ?: ""
			if (uname.contains(query) || dname.contains(query)) {
				matches.add(u)
			}
			if (matches.size >= 6) break
		}

		if (matches.isEmpty()) {
			menu.visibility = View.GONE
			return
		}

		menu.removeAllViews()
		menu.visibility = View.VISIBLE
		val d = resources.displayMetrics.density

		for (u in matches) {
			val name = u["name"]?.toString() ?: "User"
			val rawUsername = u["username"]?.toString()?.trim() ?: ""
			val tagHandle = if (rawUsername.isNotEmpty()) rawUsername else name

			val itemTv = TextView(this).apply {
				text = "$name (@$tagHandle)"
				setTextColor(Color.parseColor("#38BDF8"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
				setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
				setOnClickListener {
					val input = chatInput ?: return@setOnClickListener
					val currentVal = input.text.toString()
					val pos = input.selectionStart
					val safePos = maxOf(0, minOf(pos, currentVal.length))
					val pre = currentVal.substring(0, safePos)
					val lastAt = pre.lastIndexOf('@')
					val post = currentVal.substring(safePos)
					val prefix = if (lastAt >= 0) pre.substring(0, lastAt) else pre
					val updated = "$prefix@$tagHandle $post"
					input.setText(updated)
					val selPos = minOf(updated.length, (if (lastAt >= 0) lastAt else pre.length) + tagHandle.length + 2)
					input.setSelection(selPos)
					menu.visibility = View.GONE
				}
			}
			menu.addView(itemTv)
		}
	}

	private fun startListeningMessages() {
		val listener = object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				stopChatShimmer()
				var msgList: MutableList<HashMap<String, Any>> = ArrayList()
				if (snapshot.exists()) {
					for (child in snapshot.children) {
						val raw = child.value
						if (raw is Map<*, *>) {
							val m = HashMap<String, Any>()
							for ((k, v) in raw) {
								if (k != null && v != null) m[k.toString()] = v
							}
							child.key?.let { m["_id"] = it }
							msgList.add(m)
						}
					}
				}
				msgList.sortWith { a, b ->
					parseLongVal(a["timestamp"]).compareTo(parseLongVal(b["timestamp"]))
				}
				if (msgList.size > 60) {
					msgList = msgList.subList(msgList.size - 60, msgList.size)
				}
				renderMessages(msgList)
			}

			override fun onCancelled(error: DatabaseError) {
				stopChatShimmer()
			}
		}
		chatListener = listener
		liveChatRef.limitToLast(60).addValueEventListener(listener)
	}

	private fun renderMessages(msgList: List<HashMap<String, Any>>) {
		val container = messagesContainer ?: return
		container.removeAllViews()
		messageRowMap.clear()

		val d = resources.displayMetrics.density
		val myEmail = currentUser?.email?.lowercase(Locale.US) ?: ""
		val isCurrentUserAdmin = ADMIN_EMAIL.equals(myEmail, ignoreCase = true)

		var lastDateKey = ""

		for (item in msgList) {
			val msgId = item["_id"]?.toString() ?: ""
			var ts = parseLongVal(item["timestamp"])
			if (ts <= 0L) ts = System.currentTimeMillis()

			val dateKey = getDateKey(ts)
			if (dateKey != lastDateKey) {
				lastDateKey = dateKey
				container.addView(buildDateDivider(getDateDividerLabel(ts), d))
			}

			val email = item["email"]?.toString() ?: ""
			val uKey = if (email.isNotEmpty()) {
				email.trim().lowercase(Locale.US).replace(".", ",")
			} else {
				item["uid"]?.toString() ?: ""
			}

			val cachedProf = userProfileCache[uKey]
			val cachedName = cachedProf?.get("name")?.toString()?.trim() ?: ""
			val authorName = when {
				cachedName.isNotEmpty() -> cachedName
				item["author"] != null -> item["author"].toString()
				email.contains("@") -> email.split("@")[0]
				else -> "User"
			}

			val cachedPhoto = cachedProf?.get("photoURL")?.toString()?.trim() ?: ""
			val photoUrl = if (cachedPhoto.isNotEmpty()) {
				cachedPhoto
			} else {
				item["photoURL"]?.toString()?.trim() ?: ""
			}

			val exp = if (cachedProf?.get("exp") != null) {
				parseLongVal(cachedProf["exp"])
			} else {
				parseLongVal(item["exp"])
			}

			val isMsgAdmin = ADMIN_EMAIL.equals(email, ignoreCase = true) || (item["isAdmin"] == true)
			val isMine = (myEmail.isNotEmpty() && myEmail.equals(email, ignoreCase = true)) ||
				(currentUser != null && currentUser?.uid == item["uid"])

			val text = item["text"]?.toString() ?: ""
			val stickerUrl = item["stickerUrl"]?.toString() ?: ""

			val row = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = if (isMine) Gravity.END else Gravity.START
				clipChildren = false
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					bottomMargin = (12 * d).toInt()
				}
			}

			val avSize = (36 * d).toInt()
			val avatar = ImageView(this).apply {
				layoutParams = LinearLayout.LayoutParams(avSize, avSize).apply {
					if (isMine) leftMargin = (8 * d).toInt() else rightMargin = (8 * d).toInt()
				}
				scaleType = ImageView.ScaleType.CENTER_CROP
			}
			bindChatAvatar(avatar, photoUrl)

			val bubbleCol = LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				clipChildren = false
				layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
			}

			val bubbleBg = GradientDrawable().apply {
				orientation = GradientDrawable.Orientation.TOP_BOTTOM
				if (isMine) {
					colors = intArrayOf(Color.parseColor("#1D3358"), Color.parseColor("#13213B"))
					cornerRadii = floatArrayOf(
						18 * d, 18 * d,
						6 * d, 6 * d,
						18 * d, 18 * d,
						18 * d, 18 * d
					)
					setStroke((1 * d).toInt(), Color.parseColor("#5538BDF8"))
				} else {
					colors = intArrayOf(Color.parseColor("#181C26"), Color.parseColor("#11141C"))
					cornerRadii = floatArrayOf(
						6 * d, 6 * d,
						18 * d, 18 * d,
						18 * d, 18 * d,
						18 * d, 18 * d
					)
					setStroke((1 * d).toInt(), Color.parseColor("#24FFFFFF"))
				}
			}

			val bubble = LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				minimumWidth = (185 * d).toInt()
				setPadding((14 * d).toInt(), (11 * d).toInt(), (18 * d).toInt(), (10 * d).toInt())
				background = bubbleBg
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					gravity = if (isMine) Gravity.END else Gravity.START
				}
			}

			val replyObj = item["replyTo"]
			if (replyObj is Map<*, *>) {
				val targetId = replyObj["id"]?.toString() ?: ""
				val rAuthor = replyObj["author"]?.toString() ?: ""
				val rSnippet = replyObj["snippet"]?.toString() ?: ""

				val quoteBox = LinearLayout(this).apply {
					orientation = LinearLayout.VERTICAL
					background = GradientDrawable().apply {
						setColor(Color.parseColor("#66070B14"))
						cornerRadius = 12 * d
						setStroke((1 * d).toInt(), Color.parseColor("#2638BDF8"))
					}
					setPadding((10 * d).toInt(), (6 * d).toInt(), (12 * d).toInt(), (6 * d).toInt())
					layoutParams = LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
					).apply {
						bottomMargin = (7 * d).toInt()
					}
					setOnClickListener {
						val targetRow = messageRowMap[targetId]
						if (targetRow != null) {
							chatScrollView?.smoothScrollTo(0, targetRow.top)
						}
					}
				}

				val qAuthor = TextView(this).apply {
					this.text = rAuthor
					setTextColor(Color.parseColor("#38BDF8"))
					setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
					setTypeface(null, Typeface.BOLD)
				}

				val qSnip = TextView(this).apply {
					this.text = rSnippet
					setTextColor(Color.parseColor("#CBD5E1"))
					setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
					maxLines = 2
					ellipsize = TextUtils.TruncateAt.END
				}

				quoteBox.addView(qAuthor)
				quoteBox.addView(qSnip)
				bubble.addView(quoteBox)
			}

			val metaRow = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				setPadding(0, 0, (6 * d).toInt(), 0)
			}

			val tvSender = TextView(this).apply {
				this.text = authorName
				setTextColor(if (isMsgAdmin) Color.parseColor("#FBBF24") else Color.parseColor("#7DD3FC"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
				setTypeface(null, Typeface.BOLD)
				maxLines = 1
			}

			val tvLv = TextView(this).apply {
				this.text = if (isMsgAdmin) "Lv999+" else "Lv${calculateLevel(exp)}"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
				setTypeface(null, Typeface.BOLD)
				maxLines = 1
				setTextColor(if (isMsgAdmin) Color.parseColor("#451A03") else Color.parseColor("#38BDF8"))
				background = GradientDrawable().apply {
					cornerRadius = 100f
					setColor(if (isMsgAdmin) Color.parseColor("#FBBF24") else Color.parseColor("#2638BDF8"))
				}
				setPadding((6 * d).toInt(), (1.5f * d).toInt(), (6 * d).toInt(), (1.5f * d).toInt())
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					leftMargin = (6 * d).toInt()
				}
			}

			metaRow.addView(tvSender)
			metaRow.addView(tvLv)

			if (isMsgAdmin) {
				val tvAdm = TextView(this).apply {
					this.text = "★ ADMIN"
					setTextColor(Color.WHITE)
					setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
					setTypeface(null, Typeface.BOLD)
					maxLines = 1
					background = GradientDrawable().apply {
						cornerRadius = 100f
						setColor(Color.parseColor("#EA580C"))
					}
					setPadding((6 * d).toInt(), (1.5f * d).toInt(), (6 * d).toInt(), (1.5f * d).toInt())
					layoutParams = LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
					).apply {
						leftMargin = (5 * d).toInt()
					}
				}
				metaRow.addView(tvAdm)
			}

			bubble.addView(metaRow)

			if (text.isNotEmpty()) {
				val tvBody = TextView(this).apply {
					this.text = formatMentionsSpannable(text)
					setTextColor(Color.parseColor("#F8FAFC"))
					setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
					layoutParams = LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
					).apply {
						topMargin = (5 * d).toInt()
					}
				}
				bubble.addView(tvBody)
			}

			if (stickerUrl.isNotEmpty()) {
				val stSize = (124 * d).toInt()
				val stImg = ImageView(this).apply {
					layoutParams = LinearLayout.LayoutParams(stSize, stSize).apply {
						topMargin = (6 * d).toInt()
					}
					scaleType = ImageView.ScaleType.FIT_CENTER
				}
				Glide.with(applicationContext).load(Uri.parse(stickerUrl)).into(stImg)
				bubble.addView(stImg)
			}

			val tvTime = TextView(this).apply {
				this.text = formatClock(ts)
				setTextColor(Color.parseColor("#94A3B8"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				).apply {
					gravity = Gravity.END
					topMargin = (3 * d).toInt()
				}
			}
			bubble.addView(tvTime)

			bubble.setOnClickListener {
				replyToId = msgId
				replyToAuthor = authorName
				replyToSnippet = when {
					text.isNotEmpty() -> text
					stickerUrl.isNotEmpty() -> "[Stiker/GIF]"
					else -> "Pesan"
				}
				replyTargetAuthor?.text = replyToAuthor
				replyTargetSnippet?.text = replyToSnippet
				replyBanner?.visibility = View.VISIBLE
				chatInput?.requestFocus()
			}

			if (isMine || isCurrentUserAdmin) {
				bubble.setOnLongClickListener {
					if (msgId.isEmpty()) return@setOnLongClickListener false
					AlertDialog.Builder(this@ChatActivity)
						.setTitle("Hapus Pesan")
						.setMessage("Hapus pesan ini dari live chat?")
						.setPositiveButton("Hapus") { _, _ ->
							liveChatRef.child(msgId).removeValue()
						}
						.setNegativeButton("Batal", null)
						.show()
					true
				}
			}

			bubbleCol.addView(bubble)

			if (isMine) {
				row.addView(bubbleCol)
				row.addView(avatar)
			} else {
				row.addView(avatar)
				row.addView(bubbleCol)
			}

			if (msgId.isNotEmpty()) {
				messageRowMap[msgId] = row
			}
			container.addView(row)
		}

		chatScrollView?.post {
			chatScrollView?.fullScroll(View.FOCUS_DOWN)
		}
	}

	private fun buildDateDivider(label: String, d: Float): View {
		val wrap = FrameLayout(this).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = (6 * d).toInt()
				bottomMargin = (10 * d).toInt()
			}
		}

		val badge = TextView(this).apply {
			text = label
			setTextColor(Color.parseColor("#E2E8F0"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
			setTypeface(null, Typeface.BOLD)
			setPadding((14 * d).toInt(), (4 * d).toInt(), (14 * d).toInt(), (4 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#181E2B"))
				cornerRadius = 100f
				setStroke((1 * d).toInt(), Color.parseColor("#24FFFFFF"))
			}
		}

		val bLp = FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
		)
		wrap.addView(badge, bLp)
		return wrap
	}

	private fun formatMentionsSpannable(raw: String): SpannableString {
		val ss = SpannableString(raw)
		var idx = 0
		while (idx < raw.length) {
			val at = raw.indexOf('@', idx)
			if (at < 0) break
			var end = at + 1
			while (end < raw.length && !Character.isWhitespace(raw[end])) {
				end++
			}
			if (end > at + 1) {
				ss.setSpan(ForegroundColorSpan(Color.parseColor("#38BDF8")), at, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
				ss.setSpan(StyleSpan(Typeface.BOLD), at, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
			}
			idx = end
		}
		return ss
	}

	private fun bindChatAvatar(target: ImageView, photoUrl: String?) {
		target.outlineProvider = object : ViewOutlineProvider() {
			override fun getOutline(view: View, outline: Outline) {
				outline.setOval(0, 0, view.width, view.height)
			}
		}
		target.clipToOutline = true

		if (!photoUrl.isNullOrEmpty()) {
			target.setPadding(0, 0, 0, 0)
			target.clearColorFilter()
			target.setBackgroundColor(Color.parseColor("#161A24"))
			if (photoUrl.startsWith("data:image")) {
				try {
					val b64 = photoUrl.substring(photoUrl.indexOf(",") + 1)
					val bytes = Base64.decode(b64, Base64.DEFAULT)
					val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
					if (bmp != null) {
						target.setImageBitmap(bmp)
						return
					}
				} catch (ignored: Exception) {
				}
			} else {
				Glide.with(applicationContext)
					.load(Uri.parse(photoUrl))
					.circleCrop()
					.into(target)
				return
			}
		}

		val d = resources.displayMetrics.density
		val pad = (5 * d).toInt()
		target.background = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(Color.parseColor("#1D2436"))
			setStroke((1 * d).toInt(), Color.parseColor("#2AFFFFFF"))
		}
		target.scaleType = ImageView.ScaleType.FIT_CENTER
		target.setPadding(pad, pad, pad, pad)
		target.setImageResource(R.drawable.ic_profile)
		target.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
	}

	private fun clearReplyTarget() {
		replyToId = ""
		replyToAuthor = ""
		replyToSnippet = ""
		replyBanner?.visibility = View.GONE
	}

	private fun sendMessage() {
		val user = currentUser ?: return
		val input = chatInput ?: return
		val text = input.text.toString().trim()
		if (text.isEmpty() && selectedStickerUrl.isEmpty()) return

		val email = user.email?.trim()?.lowercase(Locale.US) ?: ""
		val uKey = if (email.isNotEmpty()) email.replace(".", ",") else user.uid
		val prof = userProfileCache[uKey]

		val profName = prof?.get("name")?.toString()?.trim() ?: ""
		val author = when {
			profName.isNotEmpty() -> profName
			email.contains("@") -> email.split("@")[0]
			else -> "User"
		}

		val username = prof?.get("username")?.toString()
			?: if (email.contains("@")) email.split("@")[0] else ""

		val photo = prof?.get("photoURL")?.toString() ?: ""

		val exp = if (prof?.get("exp") != null) parseLongVal(prof["exp"]) else 0L
		val isAdm = ADMIN_EMAIL.equals(email, ignoreCase = true)

		val payload = HashMap<String, Any>().apply {
			put("uid", user.uid)
			put("email", email)
			put("author", author)
			put("username", username)
			put("photoURL", photo)
			put("exp", exp)
			put("text", text)
			put("stickerUrl", selectedStickerUrl)
			put("timestamp", System.currentTimeMillis())
			put("isAdmin", isAdm)
			if (replyToId.isNotEmpty()) {
				put(
					"replyTo",
					hashMapOf(
						"id" to replyToId,
						"author" to replyToAuthor,
						"snippet" to replyToSnippet
					)
				)
			}
		}

		val key = System.currentTimeMillis().toString()
		liveChatRef.child(key).setValue(payload).addOnCompleteListener { task ->
			if (task.isSuccessful) {
				input.setText("")
				selectedStickerUrl = ""
				stickerPreviewBox?.visibility = View.GONE
				clearReplyTarget()
			} else {
				Toast.makeText(this@ChatActivity, "Gagal mengirim pesan", Toast.LENGTH_SHORT).show()
			}
		}
	}

	override fun finish() {
		restoreMiniPlayerAfterChat()
		super.finish()
	}

	override fun onDestroy() {
		stopChatShimmer()
		chatListener?.let { liveChatRef.removeEventListener(it) }
		super.onDestroy()
	}
}
