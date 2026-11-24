package com.dongflix

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.LinkedList
import java.util.Locale
import java.util.Queue

class LoginActivity : AppCompatActivity() {

	companion object {
		private const val DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app"
	}

	private lateinit var mAuth: FirebaseAuth
	private lateinit var usersRef: DatabaseReference

	private var logoBadgeBox: FrameLayout? = null
	private var btnSubmitAuth: FrameLayout? = null
	private var tabSwitchContainer: LinearLayout? = null
	private var authFormCard: LinearLayout? = null
	private var groupRegisterFields: LinearLayout? = null
	private var textAuthHeading: TextView? = null
	private var textAuthSub: TextView? = null
	private var tabModeLogin: TextView? = null
	private var tabModeRegister: TextView? = null
	private var textSubmitAuth: TextView? = null
	private var textToggleHint: TextView? = null
	private var inputRegName: EditText? = null
	private var inputRegUsername: EditText? = null
	private var inputAuthEmail: EditText? = null
	private var inputAuthPassword: EditText? = null
	private var progressAuth: ProgressBar? = null

	private var isRegisterMode = false
	private var isProcessing = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		mAuth = FirebaseAuth.getInstance()
		usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("users")

		if (mAuth.currentUser != null) {
			goToMainActivity()
			return
		}

		setContentView(R.layout.activity_login)
		initViews()
		setupStyle()
		applyCustomFonts()
		setupListeners()
	}

	private fun initViews() {
		logoBadgeBox = findViewById(R.id.logo_badge_box)
		tabSwitchContainer = findViewById(R.id.tab_switch_container)
		authFormCard = findViewById(R.id.auth_form_card)
		groupRegisterFields = findViewById(R.id.group_register_fields)

		textAuthHeading = findViewById(R.id.text_auth_heading)
		textAuthSub = findViewById(R.id.text_auth_sub)
		tabModeLogin = findViewById(R.id.tab_mode_login)
		tabModeRegister = findViewById(R.id.tab_mode_register)
		textSubmitAuth = findViewById(R.id.text_submit_auth)
		textToggleHint = findViewById(R.id.text_toggle_hint)

		inputRegName = findViewById(R.id.input_reg_name)
		inputRegUsername = findViewById(R.id.input_reg_username)
		inputAuthEmail = findViewById(R.id.input_auth_email)
		inputAuthPassword = findViewById(R.id.input_auth_password)

		btnSubmitAuth = findViewById(R.id.btn_submit_auth)
		progressAuth = findViewById(R.id.progress_auth)
	}

	private fun setupStyle() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			window.apply {
				clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
				addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
				statusBarColor = Color.parseColor("#000000")
			}
		}

		val d = resources.displayMetrics.density

		logoBadgeBox?.background = GradientDrawable().apply {
			setColor(Color.parseColor("#3B82F6"))
			cornerRadius = 14 * d
		}

		tabSwitchContainer?.background = GradientDrawable().apply {
			setColor(Color.parseColor("#12141A"))
			cornerRadius = 100f
			setStroke((1 * d).toInt(), Color.parseColor("#1F2430"))
		}

		authFormCard?.background = GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			intArrayOf(Color.parseColor("#12141A"), Color.parseColor("#0B0D11"))
		).apply {
			cornerRadius = 20 * d
			setStroke((1 * d).toInt(), Color.parseColor("#24FFFFFF"))
		}

		val inputs = arrayOf(inputRegName, inputRegUsername, inputAuthEmail, inputAuthPassword)
		for (et in inputs) {
			et?.background = GradientDrawable().apply {
				setColor(Color.parseColor("#181B23"))
				cornerRadius = 12 * d
				setStroke((1 * d).toInt(), Color.parseColor("#26FFFFFF"))
			}
		}

		btnSubmitAuth?.background = GradientDrawable().apply {
			setColor(Color.parseColor("#3B82F6"))
			cornerRadius = 100f
		}

		updateModeUI()
	}

	private fun updateModeUI() {
		val activeTabBg = GradientDrawable().apply {
			setColor(Color.parseColor("#3B82F6"))
			cornerRadius = 100f
		}

		if (isRegisterMode) {
			tabModeRegister?.apply {
				background = activeTabBg
				setTextColor(Color.WHITE)
			}
			tabModeLogin?.apply {
				background = null
				setTextColor(Color.parseColor("#94A3B8"))
			}
			groupRegisterFields?.visibility = View.VISIBLE
			textAuthHeading?.text = "Buat Akun Dongflix"
			textAuthSub?.text = "Daftar dengan email dan password untuk menyimpan favorit & riwayat tontonan kamu."
			textSubmitAuth?.text = "Daftar Sekarang"
			textToggleHint?.text = "Sudah punya akun? Masuk di sini"
		} else {
			tabModeLogin?.apply {
				background = activeTabBg
				setTextColor(Color.WHITE)
			}
			tabModeRegister?.apply {
				background = null
				setTextColor(Color.parseColor("#94A3B8"))
			}
			groupRegisterFields?.visibility = View.GONE
			textAuthHeading?.text = "Selamat Datang Kembali"
			textAuthSub?.text = "Masuk menggunakan email dan password akun Dongflix kamu."
			textSubmitAuth?.text = "Masuk Sekarang"
			textToggleHint?.text = "Belum punya akun? Daftar di sini"
		}
	}

	private fun setupListeners() {
		tabModeLogin?.setOnClickListener {
			if (isProcessing) return@setOnClickListener
			isRegisterMode = false
			updateModeUI()
		}

		tabModeRegister?.setOnClickListener {
			if (isProcessing) return@setOnClickListener
			isRegisterMode = true
			updateModeUI()
		}

		textToggleHint?.setOnClickListener {
			if (isProcessing) return@setOnClickListener
			isRegisterMode = !isRegisterMode
			updateModeUI()
		}

		btnSubmitAuth?.setOnClickListener {
			if (isProcessing) return@setOnClickListener
			handleAuthSubmit()
		}
	}

	private fun setLoadingState(loading: Boolean) {
		isProcessing = loading
		textSubmitAuth?.visibility = if (loading) View.GONE else View.VISIBLE
		progressAuth?.visibility = if (loading) View.VISIBLE else View.GONE
		btnSubmitAuth?.isEnabled = !loading
	}

	private fun emailToFirebaseKey(email: String): String {
		return email.trim().lowercase(Locale.getDefault()).replace(".", ",")
	}

	private fun handleAuthSubmit() {
		val email = inputAuthEmail?.text?.toString()?.trim()?.lowercase(Locale.getDefault()) ?: ""
		val password = inputAuthPassword?.text?.toString() ?: ""

		if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			Toast.makeText(this, "Masukkan alamat email yang valid", Toast.LENGTH_SHORT).show()
			return
		}

		if (password.length < 6) {
			Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
			return
		}

		val regName = inputRegName?.text?.toString()?.trim() ?: ""
		val regUsername = inputRegUsername?.text?.toString()?.trim()?.lowercase(Locale.getDefault()) ?: ""

		if (isRegisterMode) {
			if (regName.isEmpty()) {
				Toast.makeText(this, "Nama tampilan tidak boleh kosong", Toast.LENGTH_SHORT).show()
				return
			}
			if (regUsername.isEmpty()) {
				Toast.makeText(this, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show()
				return
			}
		}

		setLoadingState(true)

		if (!isRegisterMode) {
			mAuth.signInWithEmailAndPassword(email, password)
				.addOnCompleteListener(this) { task ->
					if (!task.isSuccessful) {
						setLoadingState(false)
						val err = task.exception?.message ?: "Login gagal"
						Toast.makeText(this@LoginActivity, err, Toast.LENGTH_SHORT).show()
						return@addOnCompleteListener
					}

					val user = mAuth.currentUser
					val userEmail = user?.email ?: email
					val userKey = emailToFirebaseKey(userEmail)

					usersRef.child(userKey).addListenerForSingleValueEvent(object : ValueEventListener {
						override fun onDataChange(snapshot: DataSnapshot) {
							var displayName = userEmail.split("@")[0]
							if (snapshot.exists() && snapshot.child("name").value != null) {
								displayName = snapshot.child("name").value.toString()
							}
							setLoadingState(false)
							Toast.makeText(this@LoginActivity, "Selamat datang, $displayName!", Toast.LENGTH_SHORT).show()
							goToMainActivity()
						}

						override fun onCancelled(error: DatabaseError) {
							setLoadingState(false)
							goToMainActivity()
						}
					})
				}
		} else {
			mAuth.createUserWithEmailAndPassword(email, password)
				.addOnCompleteListener(this) { task ->
					if (!task.isSuccessful) {
						setLoadingState(false)
						val err = task.exception?.message ?: "Pendaftaran gagal"
						Toast.makeText(this@LoginActivity, err, Toast.LENGTH_SHORT).show()
						return@addOnCompleteListener
					}

					var cleanUsername = regUsername.replace("[^a-z0-9_]".toRegex(), "")
					if (cleanUsername.isEmpty()) cleanUsername = email.split("@")[0]
					val userKey = emailToFirebaseKey(email)

					val profileMap = HashMap<String, Any>().apply {
						put("email", email)
						put("name", regName)
						put("username", cleanUsername)
						put("bio", "Dongflix — Streaming Donghua Sub Indo")
						put("exp", 100)
						put("followersCount", 0)
						put("followingCount", 0)
						put("photoURL", "")
						put("updatedAt", System.currentTimeMillis())
					}

					usersRef.child(userKey).setValue(profileMap)
						.addOnCompleteListener {
							setLoadingState(false)
							Toast.makeText(
								this@LoginActivity,
								"Akun berhasil dibuat, selamat datang $regName!",
								Toast.LENGTH_SHORT
							).show()
							goToMainActivity()
						}
				}
		}
	}

	private fun goToMainActivity() {
		val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
		}
		startActivity(intent)
		finish()
	}

	private fun applyCustomFonts() {
		try {
			val fontReg = Typeface.createFromAsset(assets, "fonts/productsans_reg.ttf")
			val fontMed = Typeface.createFromAsset(assets, "fonts/productsans_med.ttf")
			val fontBold = Typeface.createFromAsset(assets, "fonts/productsans_bold.ttf")

			val rootView = window.decorView.findViewById<View>(android.R.id.content) ?: return
			val viewQueue: Queue<View> = LinkedList()
			viewQueue.add(rootView)

			while (viewQueue.isNotEmpty()) {
				val currentView = viewQueue.poll()
				if (currentView is TextView) {
					val style = currentView.typeface?.style ?: Typeface.NORMAL
					when (style) {
						Typeface.BOLD -> currentView.setTypeface(fontBold, Typeface.NORMAL)
						Typeface.ITALIC -> currentView.setTypeface(fontReg, Typeface.ITALIC)
						else -> currentView.setTypeface(fontMed, Typeface.NORMAL)
					}
				} else if (currentView is ViewGroup) {
					for (k in 0 until currentView.childCount) {
						viewQueue.add(currentView.getChildAt(k))
					}
				}
			}
		} catch (ignored: Exception) {
		}
	}
}
