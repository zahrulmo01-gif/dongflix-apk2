package com.dongflix

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import kotlin.math.ceil

class CatalogActivity : AppCompatActivity() {

	companion object {
		private const val FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app"
		private const val CATALOG_URL = "https://dongflix-api1.zahrulmo01.workers.dev/"

		private const val FILTER_SEMUA = 0
		private const val FILTER_DONGHUA = 1

		private const val STATUS_ALL = 0
		private const val STATUS_ONGOING = 1
		private const val STATUS_COMPLETED = 2

		private const val SORT_DEFAULT = 0
		private const val SORT_TITLE_ASC = 1
		private const val SORT_TITLE_DESC = 2
		private const val SORT_POPULAR = 3

		private val BASE_CATEGORIES = arrayOf(
			"Action", "Adventure", "Comedy", "Cultivation", "Demons",
			"Drama", "Fantasy", "Game", "Historical", "Isekai",
			"Martial Arts", "Mystery", "Reincarnation", "Romance",
			"Sci-Fi", "Slice of Life", "Super Power", "Supernatural",
			"Suspense", "Thriller", "Time Travel", "Urban Fantasy"
		)
	}

	private var catalogRoot: FrameLayout? = null
	private var catalogHeader: LinearLayout? = null
	private var catalogScrollView: NestedScrollView? = null
	private var gridCatalog: GridView? = null
	private var layoutLoadingMore: LinearLayout? = null
	private var btnBack: View? = null
	private var btnScrollTop: FrameLayout? = null

	private var filterMenu: TextView? = null
	private var filterSemua: TextView? = null
	private var filterDonghua: TextView? = null
	private var textCatalogTitle: TextView? = null
	private var textCatalogSubtitle: TextView? = null

	private lateinit var req: RequestNetwork
	private lateinit var reqListener: RequestNetwork.RequestListener

	private var allDataList = ArrayList<HashMap<String, Any>>()
	private var filteredDataList = ArrayList<HashMap<String, Any>>()
	private val displayedList = ArrayList<HashMap<String, Any>>()
	private val availableCategories = ArrayList<String>()
	private val viewsTotalMap = HashMap<String, Long>()
	private lateinit var adapter: CatalogAdapter

	private var viewsTotalRef: DatabaseReference? = null
	private var viewsTotalListener: ValueEventListener? = null

	private var activeFilter = FILTER_SEMUA
	private var activeStatusFilter = STATUS_ALL
	private var activeSortMode = SORT_DEFAULT
	private var selectedCategory = "Semua Kategori"
	private var incomingFilterType = "all"
	private var baseHeaderTitle = "Katalog Donghua"

	private var currentLimit = 18
	private val PAGE_SIZE = 18
	private var lastMeasuredRowCount = -1
	private var lastMeasuredItemHeight = -1
	private var isLoadingMore = false
	private var isScrollTopVisible = false
	private val paginationHandler = Handler(Looper.getMainLooper())

	private var catalogShimmerAnimator: ValueAnimator? = null
	private var catalogShimmerPhase = 0f
	private var isCatalogLoading = true
	private var outfitFont: Typeface? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_catalog)

		WindowCompat.setDecorFitsSystemWindows(window, false)
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

		catalogRoot = findViewById(R.id.catalog_root)
		catalogHeader = findViewById(R.id.catalog_header)
		catalogScrollView = findViewById(R.id.catalog_scroll_view)
		gridCatalog = findViewById(R.id.grid_catalog)
		layoutLoadingMore = findViewById(R.id.layout_loading_more)
		btnBack = findViewById(R.id.btn_catalog_back)
		btnScrollTop = findViewById(R.id.btn_scroll_top)

		filterMenu = findViewById(R.id.filter_menu)
		filterSemua = findViewById(R.id.filter_semua)
		filterDonghua = findViewById(R.id.filter_donghua)
		textCatalogTitle = findViewById(R.id.text_catalog_title)
		textCatalogSubtitle = findViewById(R.id.text_catalog_subtitle)

		initDefaultCategories()
		applyIncomingMainSectionPreset()

		adapter = CatalogAdapter(displayedList)
		gridCatalog?.adapter = adapter

		applyHeaderGlassStyle()
		setupScrollTopButton()
		setupFilterTabs()
		setupInfiniteScroll()
		updateBottomSpacingForMiniPlayer()
		applyOutfitFontsWithoutBold()

		btnBack?.setOnClickListener { finish() }

		gridCatalog?.setOnTouchListener { _, event ->
			event.action == MotionEvent.ACTION_MOVE
		}

		gridCatalog?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
			if (position < displayedList.size) {
				val item = displayedList[position]
				if (item.containsKey("is_loading")) return@OnItemClickListener

				val slug = safeStr(item, "slug", "")
				if (slug.isNotEmpty()) {
					val watchIntent = Intent(this@CatalogActivity, WatchActivity::class.java).apply {
						putExtra("slug", slug)
					}
					startActivity(watchIntent)
					overridePendingTransition(R.anim.slide_up_enter, R.anim.no_anim)
				} else {
					val intent = Intent(this@CatalogActivity, DetailActivity::class.java).apply {
						putExtra("url", safeStr(item, "url", ""))
						putExtra("title", safeStr(item, "title", ""))
						putExtra("image", safeStr(item, "cover", ""))
					}
					startActivity(intent)
				}
			}
		}

		attachFirebaseViewsListener()

		val cachedJson = DongflixPlayerManager.getInstance().cachedAllCatalogJson
		if (cachedJson.trim().isNotEmpty()) {
			val cachedList = parseCatalogJson(cachedJson)
			if (cachedList.isNotEmpty()) {
				isCatalogLoading = false
				allDataList = cachedList
				rebuildCategoriesFromCatalog(allDataList)
				applyFilterAndResetPagination()
			} else {
				showCatalogLoadingPlaceholders()
			}
		} else {
			showCatalogLoadingPlaceholders()
		}

		req = RequestNetwork(this)
		reqListener = object : RequestNetwork.RequestListener {
			override fun onResponse(tag: String, response: String, headers: HashMap<String, Any>) {
				if (isFinishing || isDestroyed) return
				stopCatalogShimmer()
				DongflixPlayerManager.getInstance().cachedAllCatalogJson = response
				val parsed = parseCatalogJson(response)
				if (parsed.isNotEmpty()) {
					allDataList = parsed
					rebuildCategoriesFromCatalog(allDataList)
					applyFilterAndResetPagination()
				}
			}

			override fun onErrorResponse(tag: String, message: String) {
				if (isFinishing || isDestroyed) return
				stopCatalogShimmer()
			}
		}

		req.startRequestNetwork("GET", CATALOG_URL, "", reqListener)
	}

	private fun applyIncomingMainSectionPreset() {
		val extraTitle = intent.getStringExtra("section_title")
		val extraFilter = intent.getStringExtra("filter_type")

		if (!extraTitle.isNullOrBlank()) {
			baseHeaderTitle = extraTitle.trim()
		}
		textCatalogTitle?.text = baseHeaderTitle

		if (extraFilter.isNullOrBlank()) {
			incomingFilterType = "all"
			return
		}

		incomingFilterType = extraFilter.trim().lowercase(Locale.US)
		when (incomingFilterType) {
			"popular_dongflix" -> activeSortMode = SORT_POPULAR
			"romance" -> selectedCategory = "Romance"
			"cultivation" -> selectedCategory = "Cultivation"
			"completed" -> activeStatusFilter = STATUS_COMPLETED
			"comedy" -> selectedCategory = "Comedy"
			"mystery" -> selectedCategory = "Mystery"
			"movie" -> { /* Ditangani di matchesActiveFilter via incomingFilterType = "movie" */ }
			else -> incomingFilterType = "all"
		}
	}

	private fun parseCatalogJson(json: String): ArrayList<HashMap<String, Any>> {
		return try {
			val type = object : TypeToken<ArrayList<HashMap<String, Any>>>() {}.type
			Gson().fromJson<ArrayList<HashMap<String, Any>>>(json, type) ?: ArrayList()
		} catch (e: Exception) {
			ArrayList()
		}
	}

	private fun attachFirebaseViewsListener() {
		detachFirebaseViewsListener()
		viewsTotalRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("views_total")
		viewsTotalListener = object : ValueEventListener {
			override fun onDataChange(snap: DataSnapshot) {
				if (isFinishing || isDestroyed) return
				viewsTotalMap.clear()
				if (snap.exists()) {
					for (child in snap.children) {
						child.key?.let { k ->
							viewsTotalMap[k] = parseLongSafe(child.value)
						}
					}
				}
				if (!isCatalogLoading && allDataList.isNotEmpty() && activeSortMode == SORT_POPULAR) {
					applyFilterAndResetPagination()
				}
			}

			override fun onCancelled(error: DatabaseError) {}
		}
		viewsTotalRef?.addValueEventListener(viewsTotalListener!!)
	}

	private fun detachFirebaseViewsListener() {
		if (viewsTotalRef != null && viewsTotalListener != null) {
			viewsTotalRef?.removeEventListener(viewsTotalListener!!)
		}
	}

	private fun safeStr(map: Map<String, Any>?, key: String?, fallback: String): String {
		if (map == null || key == null) return fallback
		return map[key]?.toString() ?: fallback
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

	private fun initDefaultCategories() {
		availableCategories.clear()
		availableCategories.add("Semua Kategori")
		Collections.addAll(availableCategories, *BASE_CATEGORIES)
	}

	private fun rebuildCategoriesFromCatalog(items: List<HashMap<String, Any>>?) {
		val unique = LinkedHashSet<String>()
		Collections.addAll(unique, *BASE_CATEGORIES)

		items?.forEach { item ->
			val genresObj = item["genres"]
			if (genresObj is List<*>) {
				for (g in genresObj) {
					val clean = g?.toString()?.trim() ?: ""
					if (clean.isNotEmpty()) unique.add(clean)
				}
			} else if (genresObj != null) {
				val raw = genresObj.toString().replace("[", "").replace("]", "")
				for (part in raw.split(",")) {
					val clean = part.trim()
					if (clean.isNotEmpty()) unique.add(clean)
				}
			}
		}

		val sorted = ArrayList(unique)
		Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER)

		availableCategories.clear()
		availableCategories.add("Semua Kategori")
		availableCategories.addAll(sorted)
	}

	override fun onResume() {
		super.onResume()
		updateBottomSpacingForMiniPlayer()
	}

	private fun setupScrollTopButton() {
		val scrollBtn = btnScrollTop ?: return
		val topBlur = findViewById<BlurView>(R.id.scroll_top_blur_bg)
		val topArrow = findViewById<ImageView>(R.id.img_scroll_top_arrow)

		topArrow?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			scrollBtn.elevation = 16f
		}
		setupGlassBlur(topBlur, catalogRoot, 20f, Color.parseColor("#33FFFFFF"))

		scrollBtn.setOnClickListener {
			catalogScrollView?.smoothScrollTo(0, 0)
		}
	}

	private fun updateScrollTopVisibility(scrollY: Int) {
		val scrollBtn = btnScrollTop ?: return
		val d = resources.displayMetrics.density
		val shouldShow = scrollY > (320 * d).toInt()
		if (shouldShow == isScrollTopVisible) return
		isScrollTopVisible = shouldShow

		if (shouldShow) {
			scrollBtn.visibility = View.VISIBLE
			scrollBtn.animate().alpha(1f).setDuration(160).start()
		} else {
			scrollBtn.animate().alpha(0f).setDuration(140).withEndAction {
				if (!isScrollTopVisible) {
					btnScrollTop?.visibility = View.GONE
				}
			}.start()
		}
	}

	private fun setupFilterTabs() {
		updateFilterVisualState()

		filterMenu?.setOnClickListener { openFilterBottomSheet() }

		filterSemua?.setOnClickListener {
			if (activeFilter != FILTER_SEMUA) {
				activeFilter = FILTER_SEMUA
				updateFilterVisualState()
				applyFilterAndResetPagination()
			}
		}

		filterDonghua?.setOnClickListener {
			if (activeFilter != FILTER_DONGHUA) {
				activeFilter = FILTER_DONGHUA
				updateFilterVisualState()
				applyFilterAndResetPagination()
			}
		}
	}

	private fun openFilterBottomSheet() {
		val d = resources.displayMetrics.density
		val sheet = BottomSheetDialog(this)

		val tempStatus = intArrayOf(activeStatusFilter)
		val tempSort = intArrayOf(activeSortMode)
		val tempCategory = arrayOf(selectedCategory)

		val scroll = ScrollView(this).apply {
			isFillViewport = true
			isVerticalScrollBarEnabled = false
		}

		val container = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding((18 * d).toInt(), (14 * d).toInt(), (18 * d).toInt(), (24 * d).toInt())
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#0D111A"))
				cornerRadii = floatArrayOf(
					24 * d, 24 * d,
					24 * d, 24 * d,
					0f, 0f, 0f, 0f
				)
				setStroke((1 * d).toInt(), Color.parseColor("#24FFFFFF"))
			}
		}

		val handle = View(this).apply {
			layoutParams = LinearLayout.LayoutParams((42 * d).toInt(), (4 * d).toInt()).apply {
				gravity = Gravity.CENTER_HORIZONTAL
				bottomMargin = (14 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#38FFFFFF"))
				cornerRadius = 100f
			}
		}
		container.addView(handle)

		val tvSheetTitle = TextView(this).apply {
			text = "Filter & Kategori"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = (16 * d).toInt()
			}
		}
		container.addView(tvSheetTitle)

		container.addView(buildSheetSectionLabel("Status Tayang", d))
		val statusLabels = arrayOf("Semua Status", "Ongoing", "Completed")
		val statusChips = ArrayList<TextView>()
		val statusScroll = HorizontalScrollView(this).apply {
			isHorizontalScrollBarEnabled = false
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = (16 * d).toInt()
			}
		}
		val statusRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
		}

		for (i in statusLabels.indices) {
			val chip = createSheetChip(statusLabels[i], i == tempStatus[0], d).apply {
				setOnClickListener {
					tempStatus[0] = i
					refreshChipList(statusChips, i)
				}
			}
			statusChips.add(chip)
			statusRow.addView(chip)
		}
		statusScroll.addView(statusRow)
		container.addView(statusScroll)

		container.addView(buildSheetSectionLabel("Urutkan", d))
		val sortLabels = arrayOf("Terbaru", "Judul A - Z", "Judul Z - A", "Populer")
		val sortChips = ArrayList<TextView>()
		val sortScroll = HorizontalScrollView(this).apply {
			isHorizontalScrollBarEnabled = false
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = (16 * d).toInt()
			}
		}
		val sortRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
		}

		for (i in sortLabels.indices) {
			val chip = createSheetChip(sortLabels[i], i == tempSort[0], d).apply {
				setOnClickListener {
					tempSort[0] = i
					refreshChipList(sortChips, i)
				}
			}
			sortChips.add(chip)
			sortRow.addView(chip)
		}
		sortScroll.addView(sortRow)
		container.addView(sortScroll)

		container.addView(buildSheetSectionLabel("Kategori Lengkap", d))
		val categoryChips = ArrayList<TextView>()
		var currentCatRow: LinearLayout? = null

		for (i in availableCategories.indices) {
			val catName = availableCategories[i]
			if (i % 3 == 0) {
				currentCatRow = LinearLayout(this).apply {
					orientation = LinearLayout.HORIZONTAL
					layoutParams = LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
					).apply {
						bottomMargin = (8 * d).toInt()
					}
				}
				container.addView(currentCatRow)
			}

			val catChip = TextView(this).apply {
				text = catName
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
				setTypeface(getOutfitTypeface(), Typeface.NORMAL)
				gravity = Gravity.CENTER
				isSingleLine = true
				setPadding((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt())
				layoutParams = LinearLayout.LayoutParams(0, (36 * d).toInt(), 1f).apply {
					if (i % 3 != 2) rightMargin = (8 * d).toInt()
				}
				styleSheetChip(this, catName.equals(tempCategory[0], ignoreCase = true), d)
				setOnClickListener {
					tempCategory[0] = catName
					for (ch in categoryChips) {
						val match = ch.text.toString().equals(catName, ignoreCase = true)
						styleSheetChip(ch, match, d)
					}
				}
			}

			categoryChips.add(catChip)
			currentCatRow?.addView(catChip)
		}

		val remainder = availableCategories.size % 3
		if (remainder != 0 && currentCatRow != null) {
			val fillers = 3 - remainder
			for (f in 0 until fillers) {
				val spacer = View(this).apply {
					layoutParams = LinearLayout.LayoutParams(0, (36 * d).toInt(), 1f).apply {
						if (f != fillers - 1) rightMargin = (8 * d).toInt()
					}
				}
				currentCatRow.addView(spacer)
			}
		}

		val actionRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = (14 * d).toInt()
			}
		}

		val btnReset = TextView(this).apply {
			text = "Reset"
			setTextColor(Color.parseColor("#CBD5E1"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			gravity = Gravity.CENTER
			layoutParams = LinearLayout.LayoutParams(0, (44 * d).toInt(), 1f).apply {
				rightMargin = (10 * d).toInt()
			}
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#181E2C"))
				cornerRadius = 100f
				setStroke((1 * d).toInt(), Color.parseColor("#24FFFFFF"))
			}
			setOnClickListener {
				incomingFilterType = "all"
				activeStatusFilter = STATUS_ALL
				activeSortMode = SORT_DEFAULT
				selectedCategory = "Semua Kategori"
				textCatalogTitle?.text = "Katalog Donghua"
				updateFilterVisualState()
				applyFilterAndResetPagination()
				sheet.dismiss()
			}
		}

		val btnApply = TextView(this).apply {
			text = "Terapkan"
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			gravity = Gravity.CENTER
			layoutParams = LinearLayout.LayoutParams(0, (44 * d).toInt(), 1f)
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#2563EB"))
				cornerRadius = 100f
			}
			setOnClickListener {
				if (incomingFilterType != "movie") {
					incomingFilterType = "all"
				}
				activeStatusFilter = tempStatus[0]
				activeSortMode = tempSort[0]
				selectedCategory = tempCategory[0]
				updateFilterVisualState()
				applyFilterAndResetPagination()
				sheet.dismiss()
			}
		}

		actionRow.addView(btnReset)
		actionRow.addView(btnApply)
		container.addView(actionRow)

		scroll.addView(container)
		sheet.setContentView(scroll)
		sheet.show()
	}

	private fun buildSheetSectionLabel(label: String, d: Float): TextView {
		return TextView(this).apply {
			text = label
			setTextColor(Color.parseColor("#94A3B8"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = (8 * d).toInt()
			}
		}
	}

	private fun createSheetChip(text: String, selected: Boolean, d: Float): TextView {
		return TextView(this).apply {
			this.text = text
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
			setTypeface(getOutfitTypeface(), Typeface.NORMAL)
			gravity = Gravity.CENTER
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, (34 * d).toInt()
			).apply {
				rightMargin = (8 * d).toInt()
			}
			setPadding((14 * d).toInt(), 0, (14 * d).toInt(), 0)
			styleSheetChip(this, selected, d)
		}
	}

	private fun styleSheetChip(chip: TextView, selected: Boolean, d: Float) {
		val bg = GradientDrawable().apply {
			cornerRadius = 100f
			if (selected) {
				setColor(Color.parseColor("#2563EB"))
				chip.setTextColor(Color.WHITE)
			} else {
				setColor(Color.parseColor("#141926"))
				setStroke((1 * d).toInt(), Color.parseColor("#22FFFFFF"))
				chip.setTextColor(Color.parseColor("#CBD5E1"))
			}
		}
		chip.background = bg
	}

	private fun refreshChipList(chips: List<TextView>, activeIdx: Int) {
		val d = resources.displayMetrics.density
		for (i in chips.indices) {
			styleSheetChip(chips[i], i == activeIdx, d)
		}
	}

	private fun updateFilterVisualState() {
		val d = resources.displayMetrics.density
		val hasExtraFilter = (activeStatusFilter != STATUS_ALL) ||
			(activeSortMode != SORT_DEFAULT) ||
			!"Semua Kategori".equals(selectedCategory, ignoreCase = true) ||
			incomingFilterType == "movie"

		filterMenu?.let { menu ->
			val menuBg = GradientDrawable().apply {
				shape = GradientDrawable.RECTANGLE
				cornerRadius = 100f
				if (hasExtraFilter) {
					setColor(Color.parseColor("#1E3A8A"))
					setStroke((1 * d).toInt(), Color.parseColor("#38BDF8"))
					menu.setTextColor(Color.WHITE)
				} else {
					setColor(Color.parseColor("#141822"))
					setStroke((1 * d).toInt(), Color.parseColor("#24FFFFFF"))
					menu.setTextColor(Color.parseColor("#CBD5E1"))
				}
			}
			menu.background = menuBg
		}

		val tabs = arrayOf(filterSemua, filterDonghua)
		for (i in tabs.indices) {
			val tv = tabs[i] ?: continue
			val selected = (i == activeFilter)

			val bg = GradientDrawable().apply {
				shape = GradientDrawable.RECTANGLE
				cornerRadius = 100f
				if (selected) {
					setColor(Color.parseColor("#2563EB"))
					tv.setTextColor(Color.WHITE)
				} else {
					setColor(Color.parseColor("#141822"))
					setStroke((1 * d).toInt(), Color.parseColor("#24FFFFFF"))
					tv.setTextColor(Color.parseColor("#CBD5E1"))
				}
			}
			tv.background = bg
		}

		textCatalogSubtitle?.let { subtitleView ->
			var sub = when (activeSortMode) {
				SORT_POPULAR -> "Populer"
				SORT_TITLE_ASC -> "Judul A - Z"
				SORT_TITLE_DESC -> "Judul Z - A"
				else -> "Terbaru"
			}
			if (incomingFilterType == "movie") {
				sub += " • Movie"
			} else if (!"Semua Kategori".equals(selectedCategory, ignoreCase = true)) {
				sub += " • $selectedCategory"
			}
			if (activeStatusFilter == STATUS_ONGOING) sub += " • Ongoing"
			else if (activeStatusFilter == STATUS_COMPLETED) sub += " • Completed"
			subtitleView.text = sub
		}
	}

	private fun matchesActiveFilter(item: HashMap<String, Any>): Boolean {
		val statusStr = safeStr(item, "status", "").lowercase(Locale.US)
		val typeStr = safeStr(item, "type", "").lowercase(Locale.US)
		val categoryStr = safeStr(item, "category", "").lowercase(Locale.US)
		val genresStr = safeStr(item, "genres", "").lowercase(Locale.US)
		val titleStr = safeStr(item, "title", "").lowercase(Locale.US)
		val synopsisStr = safeStr(item, "synopsis", "").lowercase(Locale.US)
		val episodeStr = safeStr(item, "episode", "").lowercase(Locale.US)
		val urlStr = safeStr(item, "url", "").lowercase(Locale.US)
		val slugStr = safeStr(item, "slug", "").lowercase(Locale.US)
		val combined = "$typeStr $categoryStr $genresStr $titleStr $synopsisStr $episodeStr $urlStr $slugStr"

		if (activeFilter == FILTER_DONGHUA) {
			val isAnime = combined.contains("anime") || combined.contains("samehadaku")
			val isDonghua = combined.contains("donghua") || combined.contains("anichin") || combined.contains("3d")
			if (!isDonghua && isAnime) return false
		}

		if (incomingFilterType == "movie") {
			if (!combined.contains("movie") && !combined.contains("film")) {
				return false
			}
		}

		if (!"Semua Kategori".equals(selectedCategory, ignoreCase = true)) {
			val targetCat = selectedCategory.lowercase(Locale.US)
			when (targetCat) {
				"romance" -> {
					val matchRomance = genresStr.contains("romance") || genresStr.contains("romantis") ||
						genresStr.contains("romansa") || combined.contains("romance") || combined.contains("romantis")
					if (!matchRomance) return false
				}
				"cultivation" -> {
					val matchCult = combined.contains("kultivasi") || combined.contains("cultivation") ||
						combined.contains("abadi") || combined.contains("immortal")
					if (!matchCult) return false
				}
				"comedy" -> {
					val matchCom = combined.contains("comedy") || combined.contains("komedi") || combined.contains("slice of life")
					if (!matchCom) return false
				}
				"mystery" -> {
					val matchMys = combined.contains("mystery") || combined.contains("misteri") || combined.contains("teki") ||
						combined.contains("thriller") || combined.contains("suspense") || combined.contains("detective")
					if (!matchMys) return false
				}
				else -> {
					if (!genresStr.contains(targetCat) && !categoryStr.contains(targetCat)) {
						return false
					}
				}
			}
		}

		if (activeStatusFilter != STATUS_ALL) {
			if (activeStatusFilter == STATUS_ONGOING && !statusStr.contains("ongoing")) {
				return false
			}
			if (activeStatusFilter == STATUS_COMPLETED && !statusStr.contains("completed") && !statusStr.contains("tamat")) {
				return false
			}
		}
		return true
	}

	private fun applyFilterAndResetPagination() {
		paginationHandler.removeCallbacksAndMessages(null)
		isLoadingMore = false
		lastMeasuredRowCount = -1
		layoutLoadingMore?.visibility = View.GONE

		filteredDataList = ArrayList()
		for (item in allDataList) {
			if (matchesActiveFilter(item)) {
				filteredDataList.add(item)
			}
		}

		if (filteredDataList.isEmpty() && allDataList.isNotEmpty() && incomingFilterType != "all") {
			filteredDataList.addAll(allDataList)
		}

		when (activeSortMode) {
			SORT_POPULAR -> {
				Collections.sort(filteredDataList) { a, b ->
					val sA = safeStr(a, "slug", "")
					val sB = safeStr(b, "slug", "")
					val vA = viewsTotalMap[sA] ?: 0L
					val vB = viewsTotalMap[sB] ?: 0L
					vB.compareTo(vA)
				}
			}
			SORT_TITLE_ASC -> {
				Collections.sort(filteredDataList) { a, b ->
					val tA = cleanText(safeStr(a, "title", ""))
					val tB = cleanText(safeStr(b, "title", ""))
					tA.compareTo(tB, ignoreCase = true)
				}
			}
			SORT_TITLE_DESC -> {
				Collections.sort(filteredDataList) { a, b ->
					val tA = cleanText(safeStr(a, "title", ""))
					val tB = cleanText(safeStr(b, "title", ""))
					tB.compareTo(tA, ignoreCase = true)
				}
			}
		}

		currentLimit = PAGE_SIZE
		updateDisplayedList(true)
		catalogScrollView?.scrollTo(0, 0)
	}

	private fun setupInfiniteScroll() {
		catalogScrollView?.setOnScrollChangeListener(
			NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
				updateScrollTopVisibility(scrollY)

				if (isCatalogLoading || isLoadingMore) return@OnScrollChangeListener
				if (displayedList.size >= filteredDataList.size) return@OnScrollChangeListener

				val child = v.getChildAt(0) ?: return@OnScrollChangeListener
				val diff = child.bottom - (v.height + scrollY)
				if (diff <= 180) {
					triggerLoadMore()
				}
			}
		)
	}

	private fun triggerLoadMore() {
		if (isLoadingMore || displayedList.size >= filteredDataList.size) return
		isLoadingMore = true
		layoutLoadingMore?.visibility = View.VISIBLE

		paginationHandler.postDelayed({
			currentLimit += PAGE_SIZE
			isLoadingMore = false
			layoutLoadingMore?.visibility = View.GONE
			updateDisplayedList(false)
		}, 250)
	}

	private fun applyHeaderGlassStyle() {
		val d = resources.displayMetrics.density
		var statusBarHeight = 0
		val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
		if (resId > 0) statusBarHeight = resources.getDimensionPixelSize(resId)

		catalogHeader?.setPadding(
			(14 * d).toInt(),
			statusBarHeight + (10 * d).toInt(),
			(14 * d).toInt(),
			(10 * d).toInt()
		)
		catalogScrollView?.let { sv ->
			sv.setPadding(
				sv.paddingLeft,
				statusBarHeight + (72 * d).toInt(),
				sv.paddingRight,
				sv.paddingBottom
			)
		}

		val backBlur = findViewById<BlurView>(R.id.catalog_back_blur_bg)
		val headerBlur = findViewById<BlurView>(R.id.catalog_header_blur_bg)
		val backArrow = findViewById<ImageView>(R.id.img_catalog_back_arrow)

		backArrow?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			btnBack?.elevation = 14f
			findViewById<View>(R.id.catalog_header_pill)?.elevation = 14f
		}

		setupGlassBlur(backBlur, catalogRoot, 20f, Color.parseColor("#33FFFFFF"))
		setupGlassBlur(headerBlur, catalogRoot, 20f, Color.parseColor("#33FFFFFF"))
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

		val glassBorder = GradientDrawable().apply {
			setColor(Color.TRANSPARENT)
			cornerRadius = 1000f
			setStroke((1 * d).toInt(), strokeColor)
		}
		blurView.foreground = glassBorder
	}

	private fun updateBottomSpacingForMiniPlayer() {
		val d = resources.displayMetrics.density
		val miniActive = DongflixPlayerManager.getInstance().isMiniPlayerActive &&
			DongflixPlayerManager.getInstance().sharedMediaPlayer != null

		catalogScrollView?.let { sv ->
			sv.setPadding(
				sv.paddingLeft,
				sv.paddingTop,
				sv.paddingRight,
				if (miniActive) (124 * d).toInt() else (40 * d).toInt()
			)
		}
		btnScrollTop?.let { btn ->
			val lp = btn.layoutParams as? FrameLayout.LayoutParams
			if (lp != null) {
				lp.bottomMargin = if (miniActive) (108 * d).toInt() else (28 * d).toInt()
				btn.layoutParams = lp
			}
		}
	}

	private fun showCatalogLoadingPlaceholders() {
		isCatalogLoading = true
		displayedList.clear()
		for (i in 0 until 18) {
			displayedList.add(hashMapOf("is_loading" to true))
		}
		adapter.notifyDataSetChanged()
		adjustGridHeight(gridCatalog, 3)
		startCatalogShimmer()
	}

	private fun createCatalogShimmerDrawable(cornerRadiusPx: Float): Drawable {
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

				val translateX = -w + (2f * w * catalogShimmerPhase)
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

	private fun startCatalogShimmer() {
		stopCatalogShimmerOnly()
		catalogShimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = 1200L
			repeatCount = ValueAnimator.INFINITE
			interpolator = LinearInterpolator()
			addUpdateListener { animation ->
				val grid = gridCatalog ?: return@addUpdateListener
				if (!isCatalogLoading) return@addUpdateListener
				catalogShimmerPhase = animation.animatedValue as Float
				for (i in 0 until grid.childCount) {
					invalidateShimmerRecursive(grid.getChildAt(i))
				}
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

	private fun stopCatalogShimmerOnly() {
		catalogShimmerAnimator?.cancel()
		catalogShimmerAnimator = null
	}

	private fun stopCatalogShimmer() {
		isCatalogLoading = false
		stopCatalogShimmerOnly()
	}

	override fun onDestroy() {
		paginationHandler.removeCallbacksAndMessages(null)
		detachFirebaseViewsListener()
		stopCatalogShimmer()
		super.onDestroy()
	}

	private fun updateDisplayedList(forceMeasure: Boolean) {
		val end = minOf(currentLimit, filteredDataList.size)
		displayedList.clear()
		displayedList.addAll(filteredDataList.subList(0, end))
		adapter.notifyDataSetChanged()
		if (forceMeasure) {
			lastMeasuredRowCount = -1
		}
		adjustGridHeight(gridCatalog, 3)
		updateBottomSpacingForMiniPlayer()
	}

	private fun adjustGridHeight(gridView: GridView?, columns: Int) {
		if (gridView == null || adapter.count == 0) return

		val items = adapter.count
		val rows = ceil(items.toDouble() / columns).toInt()
		if (rows == lastMeasuredRowCount && lastMeasuredItemHeight > 0) {
			val density = resources.displayMetrics.density
			val verticalSpacingPx = (16 * density).toInt()
			val params = gridView.layoutParams
			params.height = (lastMeasuredItemHeight * rows) + (verticalSpacingPx * maxOf(0, rows - 1))
			gridView.layoutParams = params
			return
		}

		gridView.post {
			val gridAdapter = gridView.adapter ?: return@post
			if (gridAdapter.count == 0) return@post

			val density = resources.displayMetrics.density
			val horizontalSpacingPx = (12 * density).toInt()
			val verticalSpacingPx = (16 * density).toInt()
			val paddingHorizontal = gridView.paddingLeft + gridView.paddingRight

			val availableWidth = gridView.width - paddingHorizontal - (horizontalSpacingPx * (columns - 1))
			if (availableWidth <= 0) return@post
			val columnWidth = availableWidth / columns

			val itemView = gridAdapter.getView(0, null, gridView)
			itemView.measure(
				View.MeasureSpec.makeMeasureSpec(columnWidth, View.MeasureSpec.EXACTLY),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
			)

			val exactItemHeight = itemView.measuredHeight
			lastMeasuredItemHeight = exactItemHeight
			lastMeasuredRowCount = rows

			val params = gridView.layoutParams
			params.height = (exactItemHeight * rows) + (verticalSpacingPx * maxOf(0, rows - 1))
			gridView.layoutParams = params
			gridView.requestLayout()
		}
	}

	inner class CatalogAdapter(
		private val _data: ArrayList<HashMap<String, Any>>
	) : BaseAdapter() {

		override fun getCount(): Int = _data.size
		override fun getItem(index: Int): HashMap<String, Any> = _data[index]
		override fun getItemId(index: Int): Long = index.toLong()

		override fun getView(position: Int, v: View?, container: ViewGroup): View {
			val view = v ?: layoutInflater.inflate(R.layout.custom_anime, container, false)
			val d = view.resources.displayMetrics.density
			val tf = getOutfitTypeface()

			val textTitle = view.findViewById<TextView>(R.id.text_title)
			val textStatus = view.findViewById<TextView>(R.id.text_status)
			val textEpisode = view.findViewById<TextView>(R.id.text_episode)
			val imgCover = view.findViewById<ImageView>(R.id.img_cover)
			val view2 = view.findViewById<View>(R.id.view2)

			val item = _data[position]

			if (item.containsKey("is_loading")) {
				textTitle?.apply {
					text = ""
					background = createCatalogShimmerDrawable(6 * d)
				}
				textStatus?.apply {
					text = ""
					background = createCatalogShimmerDrawable(50f)
				}
				textEpisode?.apply {
					text = ""
					background = createCatalogShimmerDrawable(4 * d)
				}
				imgCover?.apply {
					setImageResource(android.R.color.transparent)
					background = createCatalogShimmerDrawable(10 * d)
				}
				view2?.visibility = View.GONE
			} else {
				imgCover?.setBackgroundColor(Color.TRANSPARENT)
				textTitle?.apply {
					setBackgroundColor(Color.TRANSPARENT)
					setTypeface(tf, Typeface.NORMAL)
					includeFontPadding = false
					text = cleanText(safeStr(item, "title", ""))
				}
				textEpisode?.apply {
					setBackgroundColor(Color.TRANSPARENT)
					setTypeface(tf, Typeface.NORMAL)
					text = safeStr(item, "episode", "")
				}
				view2?.apply {
					visibility = View.VISIBLE
					background = GradientDrawable(
						GradientDrawable.Orientation.BOTTOM_TOP,
						intArrayOf(Color.parseColor("#F2000000"), Color.parseColor("#80000000"), Color.TRANSPARENT)
					)
				}

				textStatus?.apply {
					background = GradientDrawable().apply {
						cornerRadius = 50f
						setColor(0xFF3B82F6.toInt())
					}
					setTypeface(tf, Typeface.NORMAL)
					text = safeStr(item, "status", "")
				}

				var cover = safeStr(item, "cover", "").trim()
				if (cover.contains("i.ibb.co.com")) cover = cover.replace("i.ibb.co.com", "i.ibb.co")
				if (cover.startsWith("http://")) cover = cover.replace("http://", "https://")

				if (imgCover != null && cover.isNotEmpty() && !isFinishing && !isDestroyed) {
					Glide.with(this@CatalogActivity).load(Uri.parse(cover)).into(imgCover)
				}
			}
			return view
		}
	}
}
