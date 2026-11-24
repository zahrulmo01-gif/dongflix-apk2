package com.dongflix;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class WatchActivity extends AppCompatActivity {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";
	private static final String CATALOG_URL = "https://dongflix-api1.zahrulmo01.workers.dev/";
	private static final String ADMIN_EMAIL = "zahrulms01@gmail.com";

	private static final String[] DEFAULT_STICKERS = {
		"https://cdn-icons-png.flaticon.com/128/9408/9408175.png",
		"https://media.tenor.com/On7KVXhzuf8AAAAj/chibi-cat.gif",
		"https://cdn-icons-png.flaticon.com/128/9408/9408201.png",
		"https://media.tenor.com/2s_jOsl0e8MAAAAj/dance-happy.gif",
		"https://cdn-icons-png.flaticon.com/128/9408/9408166.png",
		"https://media.tenor.com/kHIdycPoP6AAAAAj/anime-cry.gif",
		"https://cdn-icons-png.flaticon.com/128/9408/9408224.png",
		"https://media.tenor.com/gK2J_J7KkE8AAAAj/cute-love.gif"
	};

	private ArrayList<HashMap<String, Object>> eps = new ArrayList<>();
	private RequestNetwork req_watch;
	private RequestNetwork.RequestListener _req_watch_request_listener;

	private TextureView playerTextureView;
	private WebView fallbackWebView;
	private FrameLayout layoutPlayer;
	private View playerControlsOverlay, gradientTop, gradientBottom, topBar, bottomControls, dividerLineView;
	private ImageView btnBack, btnCast, btnLock, btnSettings, btnPrevEpisode, btnRewind, btnPlay, btnForward, btnNextEpisode, btnFullscreen, imagePoster, iconExpandDetail, iconCommentChevron;
	private TextView textPlayerTitle, textTime, btnFullscreenEpisodes, btnQuality, textJudul, textRating, textYear, textGenre, textActionServer, textSortEpisodes, textCommentPreviewLabel, textCommentCount;
	private LinearLayout animSeekLeft, animSeekRight, btnUnlockScreen, pillSpeedHold, btnFill, btnOpenDetail, btnSortEpisodes, btnActionServer, btnActionDownload, btnActionShare, btnActionReport, btnCommentSection;
	private ProgressBar playerLoading, miniVideoProgressBar;
	private SeekBar playerSeekBar;
	private RecyclerView recyclerEpisode, recyclerRekomendasi;

	private String currentSlug = "";
	private String detailTitle = "";
	private String detailSynopsis = "";
	private String detailStatus = "Ongoing";
	private String detailYear = "2026";
	private String detailFirstGenre = "-";
	private String detailAllGenres = "-";
	private String detailCover = "";
	private ArrayList<String> currentGenresList = new ArrayList<>();

	private boolean isNewestFirst = true;
	private boolean isScreenLocked = false;
	private Runnable hideUnlockBtnRunnable;

	private boolean isHolding2xSpeed = false;

	private ValueAnimator shimmerAnimator;
	private float shimmerPhase = 0f;
	private boolean isDetailLoading = true;
	private boolean isEpisodesLoading = true;
	private boolean isRekomLoading = true;

	public MediaPlayer mediaPlayer;
	public Handler playerHandler;
	public Surface playerSurface;
	public Runnable progressLoop;
	public Runnable hideControlsRunnable;
	public Runnable autoDowngradeRunnable;
	public boolean areControlsVisible = true;
	public String currentPlayingUrl = "";
	public String currentStreamReferer = "";
	public boolean isFullscreen = false;
	public int savedSeekPosition = 0;

	public int currentVideoWidth = 0;
	public int currentVideoHeight = 0;
	public boolean isFillModeActive = false;

	public boolean isAutoQualityMode = true;
	public int selectedQualityIndex = 0;
	public float currentPlaybackSpeed = 1.0f;

	public int currentEpisodeIndex = 0;
	public ArrayList<HashMap<String, Object>> episodeList = new ArrayList<>();

	public ArrayList<String> currentEpisodeServerNames = new ArrayList<>();
	public ArrayList<String> currentEpisodeServerUrls = new ArrayList<>();
	public int selectedServerIndex = 0;

	public ArrayList<String> qualityNames = new ArrayList<>();
	public ArrayList<String> qualityUrls = new ArrayList<>();

	public String dmSessionCookie = "";
	private boolean isWebViewFallbackActive = false;
	private boolean isResumingFromMiniPlayer = false;

	private String pendingHistoryEpisode = "";
	private int pendingHistoryEpisodeIndex = -1;
	private long pendingHistoryPositionMs = 0L;
	private boolean historyLoaded = false;
	private boolean initialEpisodeStarted = false;
	private long lastHistorySyncMs = 0L;
	private boolean hasRewardedWatchExp = false;

	private DatabaseReference commentsRef;
	private ValueEventListener commentsListener;
	private final Map<String, HashMap<String, String>> userProfileMap = new HashMap<>();
	private final List<HashMap<String, String>> allUsersDirectory = new ArrayList<>();
	private final ArrayList<HashMap<String, Object>> mainCommentList = new ArrayList<>();
	private final Map<String, List<HashMap<String, Object>>> repliesByParentMap = new HashMap<>();
	private final ArrayList<String> stickerList = new ArrayList<>();

	private TikTokCommentAdapter activeCommentSheetAdapter;
	private TextView activeCommentSheetHeader;
	private Typeface outfitFont;

	private interface OnStickerPickedListener {
		void onStickerPicked(String url);
	}

	private interface OnReplyClickListener {
		void onReplyRequested(String parentCommentId, String mentionTag);
	}

	private interface OnSheetItemClickListener {
		void onItemClick(int index);
	}

	private static class ScoredAnimeItem {
		final HashMap<String, Object> item;
		final int score;

		ScoredAnimeItem(HashMap<String, Object> item, int score) {
			this.item = item;
			this.score = score;
		}
	}

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_watch);
		DongflixPlayerManager.getInstance().initWithApplication(getApplication());
		initialize(_savedInstanceState);
		initializeLogic();
	}

	private Typeface getOutfitTypeface() {
		if (outfitFont != null) return outfitFont;
		String[] candidates = {
			"fonts/outfit_regular.ttf",
			"fonts/outfit_medium.ttf",
			"fonts/outfit.ttf",
			"fonts/Outfit-Regular.ttf",
			"fonts/productsans_reg.ttf"
		};
		for (String path : candidates) {
			try {
				outfitFont = Typeface.createFromAsset(getAssets(), path);
				if (outfitFont != null) return outfitFont;
			} catch (Exception ignored) {}
		}
		outfitFont = Typeface.DEFAULT;
		return outfitFont;
	}

	private void applyOutfitToAllViews() {
		Typeface tf = getOutfitTypeface();
		View content = getWindow().getDecorView().findViewById(android.R.id.content);
		if (content == null) return;

		Queue<View> queue = new LinkedList<>();
		queue.add(content);
		while (!queue.isEmpty()) {
			View v = queue.poll();
			if (v instanceof TextView) {
				TextView tv = (TextView) v;
				tv.setTypeface(tf, Typeface.NORMAL);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					tv.setLetterSpacing(-0.01f);
				}
			} else if (v instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup) v;
				for (int i = 0; i < vg.getChildCount(); i++) {
					queue.add(vg.getChildAt(i));
				}
			}
		}
	}

	private Drawable resolveVectorDrawable(String resName, Drawable fallback) {
		int id = getResources().getIdentifier(resName, "drawable", getPackageName());
		if (id != 0) {
			Drawable d = ContextCompat.getDrawable(this, id);
			if (d != null) return d.mutate();
		}
		return fallback;
	}

	private Drawable createStickerSvgDrawable(final int color, final float strokePx) {
		return new Drawable() {
			private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Path path = new Path();

			{
				strokePaint.setColor(color);
				strokePaint.setStyle(Paint.Style.STROKE);
				strokePaint.setStrokeWidth(strokePx);
				strokePaint.setStrokeCap(Paint.Cap.ROUND);
				strokePaint.setStrokeJoin(Paint.Join.ROUND);

				fillPaint.setColor(color);
				fillPaint.setStyle(Paint.Style.FILL);
			}

			@Override
			public void draw(@NonNull Canvas canvas) {
				Rect b = getBounds();
				float w = b.width();
				float h = b.height();
				if (w <= 0 || h <= 0) return;

				float cx = b.exactCenterX();
				float cy = b.exactCenterY();
				float r = Math.min(w, h) * 0.40f;

				RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
				path.reset();
				path.addArc(oval, 90f, 270f);
				path.lineTo(cx + r, cy);
				path.quadTo(cx, cy, cx, cy + r);
				canvas.drawPath(path, strokePaint);

				path.reset();
				path.moveTo(cx, cy + r);
				path.lineTo(cx + r, cy);
				canvas.drawPath(path, strokePaint);

				float eyeR = Math.max(1.5f, r * 0.11f);
				canvas.drawCircle(cx - r * 0.34f, cy - r * 0.18f, eyeR, fillPaint);
				canvas.drawCircle(cx + r * 0.24f, cy - r * 0.28f, eyeR, fillPaint);

				RectF smileRect = new RectF(cx - r * 0.42f, cy - r * 0.22f, cx + r * 0.32f, cy + r * 0.36f);
				canvas.drawArc(smileRect, 25f, 120f, false, strokePaint);
			}

			@Override public void setAlpha(int alpha) { strokePaint.setAlpha(alpha); fillPaint.setAlpha(alpha); }
			@Override public void setColorFilter(ColorFilter colorFilter) { strokePaint.setColorFilter(colorFilter); fillPaint.setColorFilter(colorFilter); }
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	private Drawable createSendSvgDrawable(final int color, final float strokePx) {
		return new Drawable() {
			private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Path path = new Path();

			{
				paint.setColor(color);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(strokePx);
				paint.setStrokeCap(Paint.Cap.ROUND);
				paint.setStrokeJoin(Paint.Join.ROUND);
			}

			@Override
			public void draw(@NonNull Canvas canvas) {
				Rect b = getBounds();
				float w = b.width();
				float h = b.height();
				if (w <= 0 || h <= 0) return;

				float p = Math.min(w, h) * 0.18f;
				float x1 = b.left + p;
				float y1 = b.top + p;
				float x2 = b.right - p;
				float y2 = b.bottom - p;
				float midX = b.exactCenterX();
				float midY = b.exactCenterY();

				path.reset();
				path.moveTo(x2, y1);
				path.lineTo(midX + (w * 0.12f), y2);
				path.lineTo(midX - (w * 0.04f), midY + (h * 0.04f));
				path.lineTo(x1, midY - (h * 0.12f));
				path.close();

				path.moveTo(x2, y1);
				path.lineTo(midX - (w * 0.04f), midY + (h * 0.04f));
				canvas.drawPath(path, paint);
			}

			@Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
			@Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	private String formatRelativeOrDateTime(long ts) {
		if (ts <= 0L) return "Baru saja";
		long now = System.currentTimeMillis();
		long diffMs = now - ts;
		if (diffMs < 60_000L) {
			return "Baru saja";
		}
		long minutes = diffMs / 60_000L;
		if (minutes < 60L) {
			return minutes + " menit yang lalu";
		}
		long hours = diffMs / 3_600_000L;
		if (hours < 24L) {
			return hours + " jam yang lalu";
		}
		long days = diffMs / 86_400_000L;
		if (days <= 7L) {
			return days + " hari yang lalu";
		}

		Calendar commentCal = Calendar.getInstance();
		commentCal.setTimeInMillis(ts);
		Calendar nowCal = Calendar.getInstance();
		nowCal.setTimeInMillis(now);

		int month = commentCal.get(Calendar.MONTH) + 1;
		int day = commentCal.get(Calendar.DAY_OF_MONTH);
		if (commentCal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR)) {
			int year2 = commentCal.get(Calendar.YEAR) % 100;
			return String.format(Locale.US, "%d-%d-%02d", month, day, year2);
		}
		return String.format(Locale.US, "%d-%d", month, day);
	}

	private String sanitizeUserKey(String email, String uid) {
		if (email != null && !email.trim().isEmpty()) {
			return email.trim().toLowerCase(Locale.US).replace(".", ",");
		}
		return uid != null ? uid.trim() : "";
	}

	private String getCurrentUserKey() {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user == null) return "";
		return sanitizeUserKey(user.getEmail(), user.getUid());
	}

	private int calculateLevel(long expVal) {
		if (expVal <= 0L) return 1;
		return (int) ((expVal - 1L) / 1000L) + 1;
	}

	private void addExpToCurrentUser(final long amount) {
		String key = getCurrentUserKey();
		if (key.isEmpty() || amount <= 0L) return;

		DatabaseReference expRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("users")
			.child(key)
			.child("exp");

		expRef.runTransaction(new Transaction.Handler() {
			@NonNull
			@Override
			public Transaction.Result doTransaction(@NonNull MutableData currentData) {
				long cur = 0L;
				Object val = currentData.getValue();
				if (val instanceof Number) {
					cur = ((Number) val).longValue();
				} else if (val != null) {
					try { cur = Long.parseLong(val.toString().trim()); } catch (Exception ignored) {}
				}
				currentData.setValue(cur + amount);
				return Transaction.success(currentData);
			}

			@Override
			public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
				if (committed && currentData != null) {
					long updatedExp = parseLongSafe(currentData.getValue(), 0L);
					HashMap<String, String> prof = userProfileMap.get(key);
					if (prof != null) {
						prof.put("exp", String.valueOf(updatedExp));
						if (activeCommentSheetAdapter != null) activeCommentSheetAdapter.notifyDataSetChanged();
					}
				}
			}
		});
	}

	private void loadUsersAndStickersFromFirebase() {
		DatabaseReference rootDb = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference();

		rootDb.child("users").addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (isFinishing() || isDestroyed() || !snap.exists()) return;
				userProfileMap.clear();
				allUsersDirectory.clear();
				Set<String> seenKeys = new HashSet<>();

				for (DataSnapshot child : snap.getChildren()) {
					String key = child.getKey();
					if (key == null) continue;

					String email = child.child("email").getValue() != null ? child.child("email").getValue().toString().trim() : "";
					String username = child.child("username").getValue() != null ? child.child("username").getValue().toString().trim() : "";
					String name = child.child("name").getValue() != null ? child.child("name").getValue().toString().trim() : "";
					String photo = child.child("photoURL").getValue() != null ? child.child("photoURL").getValue().toString().trim() : "";
					long exp = parseLongSafe(child.child("exp").getValue(), 0L);

					String fallbackUser = !username.isEmpty() ? username
						: (!email.isEmpty() && email.contains("@") ? email.substring(0, email.indexOf("@")) : key);
					String displayName = !name.isEmpty() ? name : fallbackUser;

					HashMap<String, String> profile = new HashMap<>();
					profile.put("key", key);
					profile.put("email", email.toLowerCase(Locale.US));
					profile.put("username", fallbackUser);
					profile.put("name", displayName);
					profile.put("photoURL", photo);
					profile.put("exp", String.valueOf(exp));

					userProfileMap.put(key, profile);
					if (!email.isEmpty()) {
						userProfileMap.put(email.toLowerCase(Locale.US).replace(".", ","), profile);
					}

					String dedupKey = fallbackUser.toLowerCase(Locale.US);
					if (!dedupKey.isEmpty() && seenKeys.add(dedupKey)) {
						allUsersDirectory.add(profile);
					}
				}

				if (activeCommentSheetAdapter != null) {
					activeCommentSheetAdapter.notifyDataSetChanged();
				}
			}

			@Override public void onCancelled(@NonNull DatabaseError error) {}
		});

		stickerList.clear();
		Collections.addAll(stickerList, DEFAULT_STICKERS);

		rootDb.child("stickers").addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (snap.exists()) {
					ArrayList<String> loaded = new ArrayList<>();
					for (DataSnapshot child : snap.getChildren()) {
						Object val = child.getValue();
						if (val instanceof String && !val.toString().trim().isEmpty()) {
							loaded.add(val.toString().trim());
						} else if (val instanceof Map) {
							Map<?, ?> m = (Map<?, ?>) val;
							Object u = m.get("url") != null ? m.get("url") : (m.get("link") != null ? m.get("link") : m.get("image"));
							if (u != null && !u.toString().trim().isEmpty()) {
								loaded.add(u.toString().trim());
							}
						}
					}
					if (!loaded.isEmpty()) {
						stickerList.clear();
						stickerList.addAll(loaded);
					}
				}
			}

			@Override public void onCancelled(@NonNull DatabaseError error) {}
		});
	}

	private HashMap<String, String> resolveUserProfile(String email, String uid) {
		String key = sanitizeUserKey(email, uid);
		if (!key.isEmpty() && userProfileMap.containsKey(key)) {
			return userProfileMap.get(key);
		}
		if (uid != null && !uid.trim().isEmpty() && userProfileMap.containsKey(uid.trim())) {
			return userProfileMap.get(uid.trim());
		}
		HashMap<String, String> fallback = new HashMap<>();
		String derived = (email != null && email.contains("@")) ? email.substring(0, email.indexOf("@")) : "User";
		fallback.put("email", email != null ? email.toLowerCase(Locale.US) : "");
		fallback.put("username", derived);
		fallback.put("name", derived);
		fallback.put("photoURL", "");
		fallback.put("exp", "0");
		return fallback;
	}

	private void bindAvatarOrInitial(ImageView avatarView, TextView initialView, String photoUrl, String displayName) {
		String initial = (displayName != null && !displayName.trim().isEmpty())
			? displayName.trim().substring(0, 1).toUpperCase(Locale.US)
			: "U";

		if (photoUrl != null && !photoUrl.trim().isEmpty()) {
			if (avatarView != null) {
				avatarView.setVisibility(View.VISIBLE);
				avatarView.clearColorFilter();
				avatarView.setPadding(0, 0, 0, 0);
				if (!isFinishing() && !isDestroyed()) {
					Glide.with(WatchActivity.this).load(Uri.parse(photoUrl.trim())).circleCrop()
						.placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(avatarView);
				}
			}
			if (initialView != null) initialView.setVisibility(View.GONE);
		} else {
			if (avatarView != null) avatarView.setVisibility(View.GONE);
			if (initialView != null) {
				initialView.setVisibility(View.VISIBLE);
				initialView.setText(initial);
				GradientDrawable circle = new GradientDrawable();
				circle.setShape(GradientDrawable.OVAL);
				circle.setColor(Color.parseColor("#252934"));
				initialView.setBackground(circle);
			}
		}
	}

	private void loadWatchHistoryFromFirebase() {
		String userKey = getCurrentUserKey();
		if (userKey.isEmpty() || currentSlug.isEmpty()) {
			historyLoaded = true;
			tryStartEpisodeFromHistoryIfReady();
			return;
		}

		FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("users")
			.child(userKey)
			.child("history")
			.child(currentSlug)
			.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snap) {
					if (isFinishing() || isDestroyed()) return;
					if (snap.exists()) {
						Object epObj = snap.child("episode").getValue();
						pendingHistoryEpisode = epObj != null ? epObj.toString().trim() : "";
						pendingHistoryEpisodeIndex = (int) parseLongSafe(snap.child("episodeIndex").getValue(), -1L);
						pendingHistoryPositionMs = parseLongSafe(snap.child("positionMs").getValue(), 0L);
						if (pendingHistoryPositionMs <= 0L) {
							pendingHistoryPositionMs = parseLongSafe(snap.child("currentTime").getValue(), 0L) * 1000L;
						}
					}
					historyLoaded = true;
					tryStartEpisodeFromHistoryIfReady();
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					historyLoaded = true;
					tryStartEpisodeFromHistoryIfReady();
				}
			});
	}

	private void tryStartEpisodeFromHistoryIfReady() {
		if (!historyLoaded || initialEpisodeStarted || isResumingFromMiniPlayer) return;
		if (episodeList == null || episodeList.isEmpty()) return;

		initialEpisodeStarted = true;
		int targetIdx = 0;

		if (pendingHistoryEpisodeIndex >= 0 && pendingHistoryEpisodeIndex < episodeList.size()) {
			targetIdx = pendingHistoryEpisodeIndex;
		} else if (!pendingHistoryEpisode.isEmpty()) {
			String cleanTarget = pendingHistoryEpisode.replaceAll("(?i)^ep(isode)?\\s*", "").trim();
			for (int i = 0; i < episodeList.size(); i++) {
				HashMap<String, Object> item = episodeList.get(i);
				String epVal = item.get("episode") != null
					? item.get("episode").toString().replaceAll("(?i)^ep(isode)?\\s*", "").trim()
					: "";
				if (!cleanTarget.isEmpty() && cleanTarget.equalsIgnoreCase(epVal)) {
					targetIdx = i;
					break;
				}
			}
		}

		int resumeMs = (pendingHistoryPositionMs > 0L && pendingHistoryPositionMs < Integer.MAX_VALUE)
			? (int) pendingHistoryPositionMs : 0;

		playEpisodeByIndex(targetIdx, resumeMs);
	}

	private void saveWatchHistoryToFirebase(int posMs, int durMs, boolean force) {
		long now = System.currentTimeMillis();
		if (!force && (now - lastHistorySyncMs) < 5000L) return;

		String userKey = getCurrentUserKey();
		if (userKey.isEmpty() || currentSlug.isEmpty()) return;

		lastHistorySyncMs = now;
		String epLabel = String.valueOf(currentEpisodeIndex + 1);
		if (episodeList != null && currentEpisodeIndex >= 0 && currentEpisodeIndex < episodeList.size()) {
			Object epObj = episodeList.get(currentEpisodeIndex).get("episode");
			if (epObj != null && !epObj.toString().trim().isEmpty()) {
				epLabel = epObj.toString().replace("Ep ", "").replace("Eps ", "").trim();
			}
		}

		long safePos = Math.max(0L, posMs);
		long safeDur = Math.max(0L, durMs);
		long posMin = safePos / 60000L;
		long posSec = (safePos / 1000L) % 60L;
		String remainingText = safeDur > safePos
			? String.format(Locale.US, "Menit %02d:%02d • Sisa %d mnt", posMin, posSec, Math.max(1L, (safeDur - safePos) / 60000L))
			: String.format(Locale.US, "Terakhir di %02d:%02d", posMin, posSec);

		String titleToSave = !detailTitle.isEmpty() ? detailTitle : (textJudul != null ? textJudul.getText().toString() : currentSlug);

		Map<String, Object> data = new HashMap<>();
		data.put("slug", currentSlug);
		data.put("title", titleToSave);
		data.put("cover", detailCover);
		data.put("episode", epLabel);
		data.put("episodeIndex", currentEpisodeIndex);
		data.put("positionMs", safePos);
		data.put("durationMs", safeDur);
		data.put("remainingText", remainingText);
		data.put("updatedAt", now);

		FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("users")
			.child(userKey)
			.child("history")
			.child(currentSlug)
			.updateChildren(data);
	}

	private long parseLongSafe(Object val, long fallback) {
		if (val == null) return fallback;
		if (val instanceof Number) return ((Number) val).longValue();
		try {
			return Long.parseLong(val.toString().trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	private void setupCommentSection() {
		if (btnCommentSection == null) return;
		float d = getResources().getDisplayMetrics().density;

		GradientDrawable pillBg = new GradientDrawable();
		pillBg.setShape(GradientDrawable.RECTANGLE);
		pillBg.setColor(Color.parseColor("#161A24"));
		pillBg.setCornerRadius(1000f);
		pillBg.setStroke((int) (1 * d), Color.parseColor("#1FFFFFFF"));
		btnCommentSection.setBackground(pillBg);

		if (iconCommentChevron != null) {
			iconCommentChevron.setColorFilter(Color.parseColor("#94A3B8"), PorterDuff.Mode.SRC_IN);
		}

		btnCommentSection.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openTikTokCommentSheet();
			}
		});

		attachCommentsListener();
	}

	private void attachCommentsListener() {
		if (currentSlug.isEmpty()) return;
		detachCommentsListener();

		String cleanKey = currentSlug.replaceAll("[.#$\\[\\]/]", "_");
		commentsRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("comments")
			.child(cleanKey);

		commentsListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (isFinishing() || isDestroyed()) return;

				ArrayList<HashMap<String, Object>> allItems = new ArrayList<>();
				if (snap.exists()) {
					for (DataSnapshot child : snap.getChildren()) {
						Object val = child.getValue();
						if (val instanceof Map) {
							HashMap<String, Object> map = new HashMap<>();
							for (Map.Entry<?, ?> e : ((Map<?, ?>) val).entrySet()) {
								if (e.getKey() != null) map.put(e.getKey().toString(), e.getValue());
							}
							map.put("id", child.getKey());
							allItems.add(map);
						}
					}
				}

				mainCommentList.clear();
				repliesByParentMap.clear();

				for (HashMap<String, Object> item : allItems) {
					String id = item.get("id") != null ? item.get("id").toString() : "";
					String parentId = item.get("parentId") != null ? item.get("parentId").toString().trim() : "";
					if (!parentId.isEmpty() && !"null".equalsIgnoreCase(parentId) && !"undefined".equalsIgnoreCase(parentId) && !parentId.equals(id)) {
						List<HashMap<String, Object>> list = repliesByParentMap.get(parentId);
						if (list == null) {
							list = new ArrayList<>();
							repliesByParentMap.put(parentId, list);
						}
						list.add(item);
					} else {
						mainCommentList.add(item);
					}
				}

				Collections.sort(mainCommentList, new Comparator<HashMap<String, Object>>() {
					@Override
					public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
						long tA = parseLongSafe(a.get("timestamp"), 0L);
						long tB = parseLongSafe(b.get("timestamp"), 0L);
						return Long.compare(tB, tA);
					}
				});

				for (List<HashMap<String, Object>> rList : repliesByParentMap.values()) {
					Collections.sort(rList, new Comparator<HashMap<String, Object>>() {
						@Override
						public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
							long tA = parseLongSafe(a.get("timestamp"), 0L);
							long tB = parseLongSafe(b.get("timestamp"), 0L);
							return Long.compare(tA, tB);
						}
					});
				}

				int total = allItems.size();
				if (textCommentCount != null) textCommentCount.setText(String.valueOf(total));
				if (activeCommentSheetHeader != null) activeCommentSheetHeader.setText("Komentar " + total);
				if (activeCommentSheetAdapter != null) activeCommentSheetAdapter.notifyDataSetChanged();
			}

			@Override public void onCancelled(@NonNull DatabaseError error) {}
		};

		commentsRef.addValueEventListener(commentsListener);
	}

	private void detachCommentsListener() {
		if (commentsRef != null && commentsListener != null) {
			commentsRef.removeEventListener(commentsListener);
		}
	}

	private void showDeleteCommentConfirmDialog(final String commentId) {
		if (commentId == null || commentId.trim().isEmpty() || currentSlug.isEmpty()) return;
		float d = getResources().getDisplayMetrics().density;

		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		LinearLayout card = new LinearLayout(this);
		card.setOrientation(LinearLayout.VERTICAL);
		card.setPadding((int) (20 * d), (int) (20 * d), (int) (20 * d), (int) (18 * d));

		GradientDrawable cardBg = new GradientDrawable();
		cardBg.setColor(Color.parseColor("#12141A"));
		cardBg.setCornerRadius(20 * d);
		cardBg.setStroke((int) (1 * d), Color.parseColor("#24FFFFFF"));
		card.setBackground(cardBg);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Hapus komentar?");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
		tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		card.addView(tvTitle);

		TextView tvDesc = new TextView(this);
		tvDesc.setText("Komentar yang dihapus tidak dapat dikembalikan.");
		tvDesc.setTextColor(Color.parseColor("#94A3B8"));
		tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
		tvDesc.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		descLp.topMargin = (int) (6 * d);
		descLp.bottomMargin = (int) (20 * d);
		tvDesc.setLayoutParams(descLp);
		card.addView(tvDesc);

		LinearLayout btnRow = new LinearLayout(this);
		btnRow.setOrientation(LinearLayout.HORIZONTAL);
		btnRow.setGravity(Gravity.END);
		btnRow.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		TextView btnCancel = new TextView(this);
		btnCancel.setText("Batal");
		btnCancel.setTextColor(Color.parseColor("#CBD5E1"));
		btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnCancel.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnCancel.setGravity(Gravity.CENTER);
		btnCancel.setPadding((int) (18 * d), (int) (10 * d), (int) (18 * d), (int) (10 * d));
		LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		cancelLp.rightMargin = (int) (10 * d);
		btnCancel.setLayoutParams(cancelLp);

		GradientDrawable cancelBg = new GradientDrawable();
		cancelBg.setColor(Color.parseColor("#181C26"));
		cancelBg.setCornerRadius(100f);
		cancelBg.setStroke((int) (1 * d), Color.parseColor("#1FFFFFFF"));
		btnCancel.setBackground(cancelBg);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { dialog.dismiss(); }
		});

		TextView btnConfirmDelete = new TextView(this);
		btnConfirmDelete.setText("Hapus");
		btnConfirmDelete.setTextColor(Color.WHITE);
		btnConfirmDelete.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnConfirmDelete.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnConfirmDelete.setGravity(Gravity.CENTER);
		btnConfirmDelete.setPadding((int) (20 * d), (int) (10 * d), (int) (20 * d), (int) (10 * d));

		GradientDrawable delBg = new GradientDrawable();
		delBg.setColor(Color.parseColor("#DC2626"));
		delBg.setCornerRadius(100f);
		btnConfirmDelete.setBackground(delBg);
		btnConfirmDelete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String cleanKey = currentSlug.replaceAll("[.#$\\[\\]/]", "_");
				FirebaseDatabase.getInstance(FIREBASE_DB_URL)
					.getReference("comments")
					.child(cleanKey)
					.child(commentId)
					.removeValue();
				dialog.dismiss();
			}
		});

		btnRow.addView(btnCancel);
		btnRow.addView(btnConfirmDelete);
		card.addView(btnRow);

		dialog.setContentView(card);
		Window win = dialog.getWindow();
		if (win != null) {
			win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			int screenW = getResources().getDisplayMetrics().widthPixels;
			int dialogW = isLandscapeOrFullscreen()
				? Math.min((int) (360 * d), screenW - (int) (48 * d))
				: Math.min((int) (340 * d), screenW - (int) (36 * d));
			win.setLayout(dialogW, ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		dialog.show();
	}

	private void openTikTokCommentSheet() {
		final float d = getResources().getDisplayMetrics().density;
		final int screenH = getResources().getDisplayMetrics().heightPixels;
		final int[] videoBottomHolder = new int[]{(int) (220 * d)};
		if (layoutPlayer != null && layoutPlayer.getHeight() > 0) {
			int[] loc = new int[2];
			layoutPlayer.getLocationOnScreen(loc);
			videoBottomHolder[0] = loc[1] + layoutPlayer.getHeight();
		}
		final int baseSheetHeight = Math.max((int) (280 * d), screenH - videoBottomHolder[0]);

		final BottomSheetDialog sheet = new BottomSheetDialog(this);
		if (sheet.getWindow() != null) {
			sheet.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			sheet.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
				| WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
			);
		}

		final LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, baseSheetHeight));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0B0D11"));
		sheetBg.setCornerRadii(new float[]{22 * d, 22 * d, 22 * d, 22 * d, 0f, 0f, 0f, 0f});
		root.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int) (38 * d), (int) (4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.topMargin = (int) (10 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#1E222B"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		FrameLayout header = new FrameLayout(this);
		header.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (44 * d)));
		header.setPadding((int) (16 * d), 0, (int) (12 * d), 0);

		int totalCount = 0;
		for (List<HashMap<String, Object>> r : repliesByParentMap.values()) totalCount += r.size();
		totalCount += mainCommentList.size();

		TextView tvHeader = new TextView(this);
		tvHeader.setText("Komentar " + totalCount);
		tvHeader.setTextColor(Color.WHITE);
		tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		tvHeader.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvHeader.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
		));
		activeCommentSheetHeader = tvHeader;

		TextView btnClose = new TextView(this);
		btnClose.setText("✕");
		btnClose.setTextColor(Color.parseColor("#CBD5E1"));
		btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		btnClose.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnClose.setGravity(Gravity.CENTER);
		btnClose.setLayoutParams(new FrameLayout.LayoutParams((int) (32 * d), (int) (32 * d), Gravity.END | Gravity.CENTER_VERTICAL));
		GradientDrawable closeBg = new GradientDrawable();
		closeBg.setShape(GradientDrawable.OVAL);
		closeBg.setColor(Color.parseColor("#181B22"));
		btnClose.setBackground(closeBg);
		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { sheet.dismiss(); }
		});

		header.addView(tvHeader);
		header.addView(btnClose);
		root.addView(header);

		final RecyclerView rvComments = new RecyclerView(this);
		rvComments.setLayoutManager(new LinearLayoutManager(this));
		rvComments.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		rvComments.setPadding((int) (14 * d), (int) (6 * d), (int) (14 * d), (int) (8 * d));
		rvComments.setClipToPadding(false);

		final String[] selectedStickerUrl = {""};
		final String[] activeReplyParentId = {""};

		final ImageView ivStickerPreview = new ImageView(this);
		final FrameLayout stickerPreviewBox = new FrameLayout(this);
		stickerPreviewBox.setVisibility(View.GONE);
		LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams((int) (64 * d), (int) (64 * d));
		spLp.leftMargin = (int) (54 * d);
		spLp.bottomMargin = (int) (6 * d);
		stickerPreviewBox.setLayoutParams(spLp);
		ivStickerPreview.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		ivStickerPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);

		TextView btnRemoveSticker = new TextView(this);
		btnRemoveSticker.setText("✕");
		btnRemoveSticker.setTextColor(Color.WHITE);
		btnRemoveSticker.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
		btnRemoveSticker.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnRemoveSticker.setGravity(Gravity.CENTER);
		btnRemoveSticker.setBackgroundColor(Color.parseColor("#EF4444"));
		btnRemoveSticker.setLayoutParams(new FrameLayout.LayoutParams((int) (18 * d), (int) (18 * d), Gravity.TOP | Gravity.END));
		stickerPreviewBox.addView(ivStickerPreview);
		stickerPreviewBox.addView(btnRemoveSticker);

		final LinearLayout replyBanner = new LinearLayout(this);
		replyBanner.setOrientation(LinearLayout.HORIZONTAL);
		replyBanner.setGravity(Gravity.CENTER_VERTICAL);
		replyBanner.setVisibility(View.GONE);
		replyBanner.setPadding((int) (16 * d), (int) (5 * d), (int) (16 * d), (int) (5 * d));
		replyBanner.setBackgroundColor(Color.parseColor("#12151D"));

		final TextView tvReplyBanner = new TextView(this);
		tvReplyBanner.setTextColor(Color.parseColor("#38BDF8"));
		tvReplyBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		tvReplyBanner.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvReplyBanner.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		final EditText etInput = new EditText(this);
		etInput.setHint("Tambahkan komentar...");
		etInput.setHintTextColor(Color.parseColor("#8E8E93"));
		etInput.setTextColor(Color.WHITE);
		etInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		etInput.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		etInput.setMaxLines(3);
		etInput.setBackgroundColor(Color.TRANSPARENT);
		etInput.setPadding((int) (12 * d), (int) (8 * d), (int) (8 * d), (int) (8 * d));
		etInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		TextView btnCancelReply = new TextView(this);
		btnCancelReply.setText("Batal");
		btnCancelReply.setTextColor(Color.parseColor("#94A3B8"));
		btnCancelReply.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		btnCancelReply.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnCancelReply.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activeReplyParentId[0] = "";
				replyBanner.setVisibility(View.GONE);
				etInput.setText("");
			}
		});
		replyBanner.addView(tvReplyBanner);
		replyBanner.addView(btnCancelReply);

		activeCommentSheetAdapter = new TikTokCommentAdapter(
			mainCommentList,
			repliesByParentMap,
			new OnReplyClickListener() {
				@Override
				public void onReplyRequested(String parentCommentId, String mentionTag) {
					activeReplyParentId[0] = parentCommentId;
					replyBanner.setVisibility(View.VISIBLE);
					tvReplyBanner.setText("Balas @" + mentionTag);

					String prefix = "@" + mentionTag + " ";
					etInput.setText(prefix);
					etInput.setSelection(prefix.length());
					etInput.requestFocus();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm != null) imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT);
				}
			}
		);
		rvComments.setAdapter(activeCommentSheetAdapter);
		root.addView(rvComments);

		final RecyclerView rvMentionList = new RecyclerView(this);
		rvMentionList.setLayoutManager(new LinearLayoutManager(this));
		rvMentionList.setVisibility(View.GONE);
		LinearLayout.LayoutParams mListLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (140 * d)
		);
		mListLp.setMargins((int) (14 * d), 0, (int) (14 * d), (int) (6 * d));
		rvMentionList.setLayoutParams(mListLp);
		GradientDrawable mListBg = new GradientDrawable();
		mListBg.setColor(Color.parseColor("#12151E"));
		mListBg.setCornerRadius(14 * d);
		mListBg.setStroke((int) (1 * d), Color.parseColor("#1E222B"));
		rvMentionList.setBackground(mListBg);
		rvMentionList.setPadding((int) (6 * d), (int) (6 * d), (int) (6 * d), (int) (6 * d));

		final ArrayList<HashMap<String, String>> filteredMentions = new ArrayList<>();
		final RecyclerView.Adapter<RecyclerView.ViewHolder> mentionAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
			@NonNull
			@Override
			public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
				LinearLayout row = new LinearLayout(parent.getContext());
				row.setOrientation(LinearLayout.HORIZONTAL);
				row.setGravity(Gravity.CENTER_VERTICAL);
				RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				);
				lp.bottomMargin = (int) (4 * d);
				row.setLayoutParams(lp);
				row.setPadding((int) (8 * d), (int) (6 * d), (int) (10 * d), (int) (6 * d));
				GradientDrawable rBg = new GradientDrawable();
				rBg.setColor(Color.parseColor("#181C26"));
				rBg.setCornerRadius(10 * d);
				row.setBackground(rBg);

				ImageView av = new ImageView(parent.getContext());
				LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams((int) (26 * d), (int) (26 * d));
				avLp.rightMargin = (int) (10 * d);
				av.setLayoutParams(avLp);
				av.setScaleType(ImageView.ScaleType.CENTER_CROP);
				av.setTag("m_av");

				TextView avIni = new TextView(parent.getContext());
				LinearLayout.LayoutParams iniLp = new LinearLayout.LayoutParams((int) (26 * d), (int) (26 * d));
				iniLp.rightMargin = (int) (10 * d);
				avIni.setLayoutParams(iniLp);
				avIni.setGravity(Gravity.CENTER);
				avIni.setTextColor(Color.WHITE);
				avIni.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
				avIni.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
				avIni.setTag("m_ini");

				LinearLayout txtCol = new LinearLayout(parent.getContext());
				txtCol.setOrientation(LinearLayout.VERTICAL);
				txtCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

				TextView tvN = new TextView(parent.getContext());
				tvN.setTextColor(Color.WHITE);
				tvN.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
				tvN.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
				tvN.setTag("m_name");

				TextView tvU = new TextView(parent.getContext());
				tvU.setTextColor(Color.parseColor("#38BDF8"));
				tvU.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
				tvU.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
				tvU.setTag("m_user");

				txtCol.addView(tvN);
				txtCol.addView(tvU);
				row.addView(av);
				row.addView(avIni);
				row.addView(txtCol);
				return new RecyclerView.ViewHolder(row) {};
			}

			@Override
			public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
				HashMap<String, String> u = filteredMentions.get(pos);
				String name = u.get("name") != null ? u.get("name") : "User";
				final String tag = u.get("username") != null && !u.get("username").isEmpty() ? u.get("username") : name;
				String photo = u.get("photoURL");

				ImageView av = holder.itemView.findViewWithTag("m_av");
				TextView avIni = holder.itemView.findViewWithTag("m_ini");
				TextView tvN = holder.itemView.findViewWithTag("m_name");
				TextView tvU = holder.itemView.findViewWithTag("m_user");

				bindAvatarOrInitial(av, avIni, photo, name);
				if (tvN != null) tvN.setText(name);
				if (tvU != null) tvU.setText("@" + tag);

				holder.itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String cur = etInput.getText().toString();
						int cursorPos = Math.min(cur.length(), Math.max(0, etInput.getSelectionStart()));
						int atPos = cur.substring(0, cursorPos).lastIndexOf('@');
						if (atPos >= 0) {
							String next = cur.substring(0, atPos) + "@" + tag + " " + cur.substring(cursorPos);
							etInput.setText(next);
							etInput.setSelection(Math.min(next.length(), atPos + tag.length() + 2));
						}
						rvMentionList.setVisibility(View.GONE);
					}
				});
			}

			@Override
			public int getItemCount() {
				return filteredMentions.size();
			}
		};
		rvMentionList.setAdapter(mentionAdapter);

		root.addView(rvMentionList);
		root.addView(replyBanner);
		root.addView(stickerPreviewBox);

		View inputDivider = new View(this);
		inputDivider.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) (1 * d))
		));
		inputDivider.setBackgroundColor(Color.parseColor("#1E222B"));
		root.addView(inputDivider);

		final LinearLayout bottomBar = new LinearLayout(this);
		bottomBar.setOrientation(LinearLayout.HORIZONTAL);
		bottomBar.setGravity(Gravity.CENTER_VERTICAL);
		bottomBar.setPadding((int) (14 * d), (int) (8 * d), (int) (14 * d), (int) (12 * d));
		bottomBar.setBackgroundColor(Color.parseColor("#0B0D11"));

		FirebaseUser curUser = FirebaseAuth.getInstance().getCurrentUser();
		HashMap<String, String> myProf = curUser != null
			? resolveUserProfile(curUser.getEmail(), curUser.getUid())
			: new HashMap<String, String>();
		String myName = myProf.get("name") != null ? myProf.get("name") : "U";
		String myPhoto = myProf.get("photoURL") != null ? myProf.get("photoURL") : "";

		ImageView ivMyAvatar = new ImageView(this);
		LinearLayout.LayoutParams myAvLp = new LinearLayout.LayoutParams((int) (32 * d), (int) (32 * d));
		myAvLp.rightMargin = (int) (10 * d);
		ivMyAvatar.setLayoutParams(myAvLp);
		ivMyAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);

		TextView tvMyInitial = new TextView(this);
		LinearLayout.LayoutParams myIniLp = new LinearLayout.LayoutParams((int) (32 * d), (int) (32 * d));
		myIniLp.rightMargin = (int) (10 * d);
		tvMyInitial.setLayoutParams(myIniLp);
		tvMyInitial.setGravity(Gravity.CENTER);
		tvMyInitial.setTextColor(Color.WHITE);
		tvMyInitial.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
		tvMyInitial.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

		bindAvatarOrInitial(ivMyAvatar, tvMyInitial, myPhoto, myName);
		bottomBar.addView(ivMyAvatar);
		bottomBar.addView(tvMyInitial);

		LinearLayout inputPill = new LinearLayout(this);
		inputPill.setOrientation(LinearLayout.HORIZONTAL);
		inputPill.setGravity(Gravity.CENTER_VERTICAL);
		inputPill.setPadding((int) (4 * d), (int) (2 * d), (int) (6 * d), (int) (2 * d));
		LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		inputPill.setLayoutParams(pillLp);
		GradientDrawable pillInputBg = new GradientDrawable();
		pillInputBg.setColor(Color.parseColor("#181B22"));
		pillInputBg.setCornerRadius(1000f);
		inputPill.setBackground(pillInputBg);

		ImageView btnSticker = new ImageView(this);
		btnSticker.setLayoutParams(new LinearLayout.LayoutParams((int) (32 * d), (int) (32 * d)));
		btnSticker.setPadding((int) (6 * d), (int) (6 * d), (int) (6 * d), (int) (6 * d));
		btnSticker.setImageDrawable(resolveVectorDrawable("ic_sticker", createStickerSvgDrawable(Color.parseColor("#CBD5E1"), Math.max(1.6f, 1.5f * d))));
		btnSticker.setColorFilter(Color.parseColor("#CBD5E1"), PorterDuff.Mode.SRC_IN);

		final ImageView btnSendIcon = new ImageView(this);
		LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams((int) (36 * d), (int) (36 * d));
		sendLp.leftMargin = (int) (8 * d);
		btnSendIcon.setLayoutParams(sendLp);
		btnSendIcon.setPadding((int) (8 * d), (int) (8 * d), (int) (8 * d), (int) (8 * d));
		btnSendIcon.setImageDrawable(resolveVectorDrawable("ic_send", createSendSvgDrawable(Color.WHITE, Math.max(1.8f, 1.7f * d))));
		btnSendIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		GradientDrawable sendBg = new GradientDrawable();
		sendBg.setShape(GradientDrawable.OVAL);
		sendBg.setColor(Color.parseColor("#2563EB"));
		btnSendIcon.setBackground(sendBg);
		btnSendIcon.setVisibility(View.GONE);

		final Runnable refreshSendVisibility = new Runnable() {
			@Override
			public void run() {
				boolean hasText = !etInput.getText().toString().trim().isEmpty();
				btnSendIcon.setVisibility(hasText ? View.VISIBLE : View.GONE);
			}
		};

		btnSticker.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openStickerPickerDialog(new OnStickerPickedListener() {
					@Override
					public void onStickerPicked(String url) {
						selectedStickerUrl[0] = url;
						ivStickerPreview.setImageURI(null);
						Glide.with(WatchActivity.this).load(Uri.parse(url)).into(ivStickerPreview);
						stickerPreviewBox.setVisibility(View.VISIBLE);
					}
				});
			}
		});

		btnRemoveSticker.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectedStickerUrl[0] = "";
				stickerPreviewBox.setVisibility(View.GONE);
			}
		});

		etInput.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				refreshSendVisibility.run();

				String full = s.toString();
				int cursor = Math.min(full.length(), Math.max(0, etInput.getSelectionStart()));
				String beforeCursor = full.substring(0, cursor);
				int atIdx = beforeCursor.lastIndexOf('@');
				if (atIdx >= 0 && (atIdx == 0 || Character.isWhitespace(beforeCursor.charAt(atIdx - 1)))) {
					String q = beforeCursor.substring(atIdx + 1).toLowerCase(Locale.US);
					filteredMentions.clear();
					for (HashMap<String, String> u : allUsersDirectory) {
						String n = u.get("name") != null ? u.get("name").toLowerCase(Locale.US) : "";
						String un = u.get("username") != null ? u.get("username").toLowerCase(Locale.US) : "";
						if (q.isEmpty() || n.contains(q) || un.contains(q)) {
							filteredMentions.add(u);
							if (filteredMentions.size() >= 8) break;
						}
					}
					mentionAdapter.notifyDataSetChanged();
					rvMentionList.setVisibility(filteredMentions.isEmpty() ? View.GONE : View.VISIBLE);
				} else {
					rvMentionList.setVisibility(View.GONE);
				}
			}
			@Override public void afterTextChanged(Editable s) {}
		});

		inputPill.addView(etInput);
		inputPill.addView(btnSticker);

		btnSendIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String text = etInput.getText().toString().trim();
				String stUrl = selectedStickerUrl[0];
				String pId = activeReplyParentId[0];

				if ((text.isEmpty() && stUrl.isEmpty()) || currentSlug.isEmpty()) return;

				FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
				if (u == null) {
					Toast.makeText(WatchActivity.this, "Silakan login untuk berkomentar", Toast.LENGTH_SHORT).show();
					return;
				}

				String uKey = sanitizeUserKey(u.getEmail(), u.getUid());
				HashMap<String, String> p = resolveUserProfile(u.getEmail(), u.getUid());
				boolean isAdm = u.getEmail() != null && ADMIN_EMAIL.equalsIgnoreCase(u.getEmail().trim());

				Map<String, Object> payload = new HashMap<>();
				payload.put("uid", u.getUid());
				payload.put("email", u.getEmail() != null ? u.getEmail() : "");
				payload.put("text", text);
				payload.put("stickerUrl", stUrl);
				payload.put("timestamp", System.currentTimeMillis());
				payload.put("isAdmin", isAdm);
				if (!pId.isEmpty()) {
					payload.put("parentId", pId);
				}

				String cleanSlugKey = currentSlug.replaceAll("[.#$\\[\\]/]", "_");
				String entryId = String.valueOf(System.currentTimeMillis());
				FirebaseDatabase.getInstance(FIREBASE_DB_URL)
					.getReference("comments")
					.child(cleanSlugKey)
					.child(entryId)
					.setValue(payload);

				addExpToCurrentUser(500L);

				etInput.setText("");
				selectedStickerUrl[0] = "";
				activeReplyParentId[0] = "";
				stickerPreviewBox.setVisibility(View.GONE);
				replyBanner.setVisibility(View.GONE);
				rvMentionList.setVisibility(View.GONE);
				refreshSendVisibility.run();

				if (!uKey.isEmpty() && !userProfileMap.containsKey(uKey)) {
					userProfileMap.put(uKey, p);
				}

				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null) imm.hideSoftInputFromWindow(etInput.getWindowToken(), 0);
			}
		});

		bottomBar.addView(inputPill);
		bottomBar.addView(btnSendIcon);
		root.addView(bottomBar);

		sheet.setContentView(root);

		final BottomSheetBehavior<FrameLayout>[] behaviorRef = new BottomSheetBehavior[1];
		rvComments.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
				if (behaviorRef[0] != null) {
					boolean atTop = !recyclerView.canScrollVertically(-1);
					behaviorRef[0].setDraggable(atTop);
				}
			}
		});

		final View dialogDecor = sheet.getWindow() != null ? sheet.getWindow().getDecorView() : null;
		final ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (dialogDecor == null) return;
				Rect r = new Rect();
				dialogDecor.getWindowVisibleDisplayFrame(r);
				int fullH = dialogDecor.getRootView().getHeight();
				int keypadHeight = Math.max(0, fullH - r.bottom);

				int availableAboveKeyboard = Math.max((int) (180 * d), r.bottom - videoBottomHolder[0]);
				int targetH = keypadHeight > (int) (100 * d)
					? Math.min(baseSheetHeight, availableAboveKeyboard)
					: baseSheetHeight;

				ViewGroup.LayoutParams lp = root.getLayoutParams();
				if (lp != null && lp.height != targetH) {
					lp.height = targetH;
					root.setLayoutParams(lp);
				}
				bottomBar.setPadding(
					(int) (14 * d),
					(int) (8 * d),
					(int) (14 * d),
					keypadHeight > (int) (100 * d) ? (int) (10 * d) : (int) (12 * d)
				);

				FrameLayout bottomSheet = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				if (bottomSheet != null) {
					ViewGroup.LayoutParams bLp = bottomSheet.getLayoutParams();
					if (bLp != null && bLp.height != targetH) {
						bLp.height = targetH;
						bottomSheet.setLayoutParams(bLp);
					}
					BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
					behaviorRef[0] = behavior;
					behavior.setPeekHeight(targetH);
					behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
				}
			}
		};

		if (dialogDecor != null) {
			dialogDecor.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
		}

		sheet.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				FrameLayout bottomSheet = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				if (bottomSheet != null) {
					bottomSheet.setBackgroundColor(Color.TRANSPARENT);
					bottomSheet.setBackgroundTintList(null);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						bottomSheet.setElevation(0f);
					}
					ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
					lp.height = baseSheetHeight;
					bottomSheet.setLayoutParams(lp);
					BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
					behaviorRef[0] = behavior;
					behavior.setPeekHeight(baseSheetHeight);
					behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
					behavior.setSkipCollapsed(true);
					behavior.setDraggable(!rvComments.canScrollVertically(-1));
				}
			}
		});
		sheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				if (dialogDecor != null) {
					dialogDecor.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
				}
				activeCommentSheetAdapter = null;
				activeCommentSheetHeader = null;
			}
		});
		sheet.show();
	}

	private void openStickerPickerDialog(final OnStickerPickedListener listener) {
		final float d = getResources().getDisplayMetrics().density;
		final BottomSheetDialog picker = new BottomSheetDialog(this);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int) (18 * d), (int) (12 * d), (int) (18 * d), (int) (22 * d));

		GradientDrawable bg = new GradientDrawable();
		bg.setColor(Color.parseColor("#0B0D11"));
		bg.setCornerRadii(new float[]{22 * d, 22 * d, 22 * d, 22 * d, 0f, 0f, 0f, 0f});
		bg.setStroke((int) (1 * d), Color.parseColor("#1E222B"));
		root.setBackground(bg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int) (38 * d), (int) (4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int) (12 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#1E222B"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		FrameLayout pHeader = new FrameLayout(this);
		pHeader.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (36 * d)));
		LinearLayout.LayoutParams hLp = (LinearLayout.LayoutParams) pHeader.getLayoutParams();
		hLp.bottomMargin = (int) (10 * d);

		TextView title = new TextView(this);
		title.setText("Pilih Stiker");
		title.setTextColor(Color.WHITE);
		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
		title.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		title.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL
		));

		TextView btnClose = new TextView(this);
		btnClose.setText("✕");
		btnClose.setTextColor(Color.parseColor("#CBD5E1"));
		btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
		btnClose.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnClose.setGravity(Gravity.CENTER);
		btnClose.setLayoutParams(new FrameLayout.LayoutParams((int) (30 * d), (int) (30 * d), Gravity.END | Gravity.CENTER_VERTICAL));
		GradientDrawable cCloseBg = new GradientDrawable();
		cCloseBg.setShape(GradientDrawable.OVAL);
		cCloseBg.setColor(Color.parseColor("#181B22"));
		btnClose.setBackground(cCloseBg);
		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { picker.dismiss(); }
		});

		pHeader.addView(title);
		pHeader.addView(btnClose);
		root.addView(pHeader);

		RecyclerView rvStickers = new RecyclerView(this);
		rvStickers.setLayoutManager(new GridLayoutManager(this, 4));
		rvStickers.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (240 * d)
		));

		rvStickers.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
			@NonNull
			@Override
			public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
				FrameLayout cell = new FrameLayout(parent.getContext());
				RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, (int) (76 * d)
				);
				lp.setMargins((int) (5 * d), (int) (5 * d), (int) (5 * d), (int) (5 * d));
				cell.setLayoutParams(lp);
				GradientDrawable cBg = new GradientDrawable();
				cBg.setColor(Color.parseColor("#12141A"));
				cBg.setCornerRadius(14 * d);
				cBg.setStroke((int) (1 * d), Color.parseColor("#1E222B"));
				cell.setBackground(cBg);

				ImageView iv = new ImageView(parent.getContext());
				iv.setLayoutParams(new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
				));
				iv.setPadding((int) (10 * d), (int) (10 * d), (int) (10 * d), (int) (10 * d));
				iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
				iv.setTag("sticker_img");
				cell.addView(iv);
				return new RecyclerView.ViewHolder(cell) {};
			}

			@Override
			public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
				final String url = stickerList.get(pos);
				ImageView iv = holder.itemView.findViewWithTag("sticker_img");
				if (iv != null && !isFinishing() && !isDestroyed()) {
					Glide.with(WatchActivity.this).load(Uri.parse(url)).into(iv);
				}
				holder.itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						picker.dismiss();
						if (listener != null) listener.onStickerPicked(url);
					}
				});
			}

			@Override
			public int getItemCount() {
				return stickerList.size();
			}
		});

		root.addView(rvStickers);
		picker.setContentView(root);
		picker.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				FrameLayout bottomSheet = picker.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				if (bottomSheet != null) {
					bottomSheet.setBackgroundColor(Color.TRANSPARENT);
					bottomSheet.setBackgroundTintList(null);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						bottomSheet.setElevation(0f);
					}
				}
			}
		});
		picker.show();
	}

	private class TikTokCommentAdapter extends RecyclerView.Adapter<TikTokCommentAdapter.ViewHolder> {
		private final List<HashMap<String, Object>> mainItems;
		private final Map<String, List<HashMap<String, Object>>> repliesMap;
		private final OnReplyClickListener replyListener;
		private final Set<String> expandedParents = new HashSet<>();

		TikTokCommentAdapter(
			List<HashMap<String, Object>> mainItems,
			Map<String, List<HashMap<String, Object>>> repliesMap,
			OnReplyClickListener replyListener
		) {
			this.mainItems = mainItems;
			this.repliesMap = repliesMap;
			this.replyListener = replyListener;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			float d = parent.getResources().getDisplayMetrics().density;

			LinearLayout card = new LinearLayout(parent.getContext());
			card.setOrientation(LinearLayout.HORIZONTAL);
			card.setGravity(Gravity.TOP);
			RecyclerView.LayoutParams cardLp = new RecyclerView.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			cardLp.bottomMargin = (int) (14 * d);
			card.setLayoutParams(cardLp);
			card.setPadding((int) (4 * d), (int) (4 * d), (int) (4 * d), (int) (4 * d));

			ImageView avatar = new ImageView(parent.getContext());
			LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams((int) (34 * d), (int) (34 * d));
			avLp.rightMargin = (int) (10 * d);
			avatar.setLayoutParams(avLp);
			avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
			avatar.setTag("avatar");

			TextView tvInitial = new TextView(parent.getContext());
			LinearLayout.LayoutParams iniLp = new LinearLayout.LayoutParams((int) (34 * d), (int) (34 * d));
			iniLp.rightMargin = (int) (10 * d);
			tvInitial.setLayoutParams(iniLp);
			tvInitial.setGravity(Gravity.CENTER);
			tvInitial.setTextColor(Color.WHITE);
			tvInitial.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
			tvInitial.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvInitial.setTag("initial");

			LinearLayout col = new LinearLayout(parent.getContext());
			col.setOrientation(LinearLayout.VERTICAL);
			col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

			LinearLayout nameLine = new LinearLayout(parent.getContext());
			nameLine.setOrientation(LinearLayout.HORIZONTAL);
			nameLine.setGravity(Gravity.CENTER_VERTICAL);

			TextView tvName = new TextView(parent.getContext());
			tvName.setTextColor(Color.parseColor("#CBD5E1"));
			tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
			tvName.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvName.setTag("name");

			TextView tvAdminBadge = new TextView(parent.getContext());
			tvAdminBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
			tvAdminBadge.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvAdminBadge.setPadding((int) (7 * d), (int) (2 * d), (int) (7 * d), (int) (2 * d));
			LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			aLp.leftMargin = (int) (6 * d);
			tvAdminBadge.setLayoutParams(aLp);
			tvAdminBadge.setTag("admin_badge");

			TextView tvLevelBadge = new TextView(parent.getContext());
			tvLevelBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
			tvLevelBadge.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvLevelBadge.setPadding((int) (7 * d), (int) (2 * d), (int) (7 * d), (int) (2 * d));
			LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			lLp.leftMargin = (int) (5 * d);
			tvLevelBadge.setLayoutParams(lLp);
			tvLevelBadge.setTag("level_badge");

			nameLine.addView(tvName);
			nameLine.addView(tvAdminBadge);
			nameLine.addView(tvLevelBadge);

			TextView tvBody = new TextView(parent.getContext());
			tvBody.setTextColor(Color.WHITE);
			tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
			tvBody.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvBody.setLineSpacing(2 * d, 1.08f);
			LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			bodyLp.topMargin = (int) (3 * d);
			tvBody.setLayoutParams(bodyLp);
			tvBody.setTag("body");

			ImageView ivSticker = new ImageView(parent.getContext());
			LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams((int) (110 * d), (int) (110 * d));
			stLp.topMargin = (int) (6 * d);
			ivSticker.setLayoutParams(stLp);
			ivSticker.setScaleType(ImageView.ScaleType.FIT_CENTER);
			ivSticker.setTag("sticker");

			LinearLayout footerRow = new LinearLayout(parent.getContext());
			footerRow.setOrientation(LinearLayout.HORIZONTAL);
			footerRow.setGravity(Gravity.CENTER_VERTICAL);
			LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			fLp.topMargin = (int) (5 * d);
			footerRow.setLayoutParams(fLp);

			TextView tvTime = new TextView(parent.getContext());
			tvTime.setTextColor(Color.parseColor("#94A3B8"));
			tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
			tvTime.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvTime.setTag("time");

			TextView btnReply = new TextView(parent.getContext());
			btnReply.setText("Balas");
			btnReply.setTextColor(Color.parseColor("#CBD5E1"));
			btnReply.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
			btnReply.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			LinearLayout.LayoutParams rBtnLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			rBtnLp.leftMargin = (int) (14 * d);
			btnReply.setLayoutParams(rBtnLp);
			btnReply.setTag("btn_reply");

			TextView btnDelete = new TextView(parent.getContext());
			btnDelete.setText("Hapus");
			btnDelete.setTextColor(Color.parseColor("#EF4444"));
			btnDelete.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
			btnDelete.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			LinearLayout.LayoutParams dBtnLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			dBtnLp.leftMargin = (int) (14 * d);
			btnDelete.setLayoutParams(dBtnLp);
			btnDelete.setTag("btn_delete");

			footerRow.addView(tvTime);
			footerRow.addView(btnReply);
			footerRow.addView(btnDelete);

			LinearLayout toggleRow = new LinearLayout(parent.getContext());
			toggleRow.setOrientation(LinearLayout.HORIZONTAL);
			toggleRow.setGravity(Gravity.CENTER_VERTICAL);
			LinearLayout.LayoutParams tglBoxLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			tglBoxLp.topMargin = (int) (8 * d);
			toggleRow.setLayoutParams(tglBoxLp);
			toggleRow.setTag("toggle_box");

			View dash = new View(parent.getContext());
			LinearLayout.LayoutParams dashLp = new LinearLayout.LayoutParams((int) (20 * d), Math.max(1, (int) (1 * d)));
			dashLp.rightMargin = (int) (8 * d);
			dash.setLayoutParams(dashLp);
			dash.setBackgroundColor(Color.parseColor("#475569"));

			TextView btnToggleReplies = new TextView(parent.getContext());
			btnToggleReplies.setTextColor(Color.parseColor("#94A3B8"));
			btnToggleReplies.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
			btnToggleReplies.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			btnToggleReplies.setTag("toggle_replies");

			ImageView ivToggleChevron = new ImageView(parent.getContext());
			LinearLayout.LayoutParams chevLp = new LinearLayout.LayoutParams((int) (13 * d), (int) (13 * d));
			chevLp.leftMargin = (int) (4 * d);
			ivToggleChevron.setLayoutParams(chevLp);
			ivToggleChevron.setImageResource(R.drawable.ic_chevron_down);
			ivToggleChevron.setColorFilter(Color.parseColor("#94A3B8"), PorterDuff.Mode.SRC_IN);
			ivToggleChevron.setTag("toggle_chevron");

			toggleRow.addView(dash);
			toggleRow.addView(btnToggleReplies);
			toggleRow.addView(ivToggleChevron);

			LinearLayout repliesWrapper = new LinearLayout(parent.getContext());
			repliesWrapper.setOrientation(LinearLayout.HORIZONTAL);
			LinearLayout.LayoutParams repWrapLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			repWrapLp.topMargin = (int) (8 * d);
			repWrapLp.leftMargin = (int) (2 * d);
			repliesWrapper.setLayoutParams(repWrapLp);
			repliesWrapper.setTag("replies_wrapper");

			View verticalLine = new View(parent.getContext());
			LinearLayout.LayoutParams vLineLp = new LinearLayout.LayoutParams(
				Math.max(2, (int) (1.8f * d)), ViewGroup.LayoutParams.MATCH_PARENT
			);
			vLineLp.rightMargin = (int) (10 * d);
			verticalLine.setLayoutParams(vLineLp);
			verticalLine.setBackgroundColor(Color.parseColor("#27272A"));

			LinearLayout repliesContainer = new LinearLayout(parent.getContext());
			repliesContainer.setOrientation(LinearLayout.VERTICAL);
			repliesContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
			repliesContainer.setTag("replies_container");

			repliesWrapper.addView(verticalLine);
			repliesWrapper.addView(repliesContainer);

			col.addView(nameLine);
			col.addView(tvBody);
			col.addView(ivSticker);
			col.addView(footerRow);
			col.addView(toggleRow);
			col.addView(repliesWrapper);

			card.addView(avatar);
			card.addView(tvInitial);
			card.addView(col);
			return new ViewHolder(card);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
			final HashMap<String, Object> item = mainItems.get(pos);
			final String commentId = item.get("id") != null ? item.get("id").toString() : "";
			String email = item.get("email") != null ? item.get("email").toString() : "";
			String uid = item.get("uid") != null ? item.get("uid").toString() : "";
			String text = item.get("text") != null ? item.get("text").toString() : "";
			String stickerUrl = item.get("stickerUrl") != null ? item.get("stickerUrl").toString().trim() : "";
			long ts = parseLongSafe(item.get("timestamp"), System.currentTimeMillis());

			HashMap<String, String> prof = resolveUserProfile(email, uid);
			final String displayName = prof.get("name");
			final String mentionHandle = prof.get("username") != null && !prof.get("username").isEmpty()
				? prof.get("username") : displayName;
			String photoUrl = prof.get("photoURL");
			long exp = parseLongSafe(prof.get("exp"), 0L);
			boolean isAdmin = !email.isEmpty() && ADMIN_EMAIL.equalsIgnoreCase(email.trim());
			int level = calculateLevel(exp);

			float d = holder.itemView.getResources().getDisplayMetrics().density;
			ImageView avatar = holder.itemView.findViewWithTag("avatar");
			TextView tvInitial = holder.itemView.findViewWithTag("initial");
			TextView tvName = holder.itemView.findViewWithTag("name");
			TextView tvAdminBadge = holder.itemView.findViewWithTag("admin_badge");
			TextView tvLevelBadge = holder.itemView.findViewWithTag("level_badge");
			TextView tvBody = holder.itemView.findViewWithTag("body");
			ImageView ivSticker = holder.itemView.findViewWithTag("sticker");
			TextView tvTime = holder.itemView.findViewWithTag("time");
			TextView btnReply = holder.itemView.findViewWithTag("btn_reply");
			TextView btnDelete = holder.itemView.findViewWithTag("btn_delete");
			View toggleBox = holder.itemView.findViewWithTag("toggle_box");
			TextView btnToggleReplies = holder.itemView.findViewWithTag("toggle_replies");
			ImageView ivToggleChevron = holder.itemView.findViewWithTag("toggle_chevron");
			View repliesWrapper = holder.itemView.findViewWithTag("replies_wrapper");
			LinearLayout repliesContainer = holder.itemView.findViewWithTag("replies_container");

			bindAvatarOrInitial(avatar, tvInitial, photoUrl, displayName);

			if (tvName != null) {
				tvName.setText(displayName);
			}
			if (tvAdminBadge != null) {
				if (isAdmin) {
					tvAdminBadge.setVisibility(View.VISIBLE);
					GradientDrawable aBg = new GradientDrawable();
					aBg.setCornerRadius(100f);
					aBg.setColor(Color.parseColor("#451A03"));
					aBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#F59E0B"));
					tvAdminBadge.setBackground(aBg);
					tvAdminBadge.setTextColor(Color.parseColor("#FBBF24"));
					tvAdminBadge.setText("★ ADMIN");
				} else {
					tvAdminBadge.setVisibility(View.GONE);
				}
			}
			if (tvLevelBadge != null) {
				GradientDrawable lBg = new GradientDrawable();
				lBg.setCornerRadius(100f);
				lBg.setColor(Color.parseColor("#0C2438"));
				lBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#38BDF8"));
				tvLevelBadge.setBackground(lBg);
				tvLevelBadge.setTextColor(Color.parseColor("#38BDF8"));
				tvLevelBadge.setText(isAdmin && exp == 0L ? "Lv999+" : "Lv" + level);
			}
			if (tvBody != null) {
				tvBody.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
				tvBody.setText(text);
			}
			if (ivSticker != null) {
				if (!stickerUrl.isEmpty() && !isFinishing() && !isDestroyed()) {
					ivSticker.setVisibility(View.VISIBLE);
					Glide.with(WatchActivity.this).load(Uri.parse(stickerUrl)).into(ivSticker);
				} else {
					ivSticker.setVisibility(View.GONE);
				}
			}
			if (tvTime != null) {
				tvTime.setText(formatRelativeOrDateTime(ts));
			}

			FirebaseUser curUser = FirebaseAuth.getInstance().getCurrentUser();
			if (btnReply != null) {
				btnReply.setVisibility(curUser != null ? View.VISIBLE : View.GONE);
				btnReply.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (replyListener != null) {
							replyListener.onReplyRequested(commentId, mentionHandle);
						}
					}
				});
			}

			if (btnDelete != null) {
				boolean canDelete = false;
				if (curUser != null) {
					String curEmail = curUser.getEmail() != null ? curUser.getEmail().trim() : "";
					if (ADMIN_EMAIL.equalsIgnoreCase(curEmail)) {
						canDelete = true;
					} else if (!email.isEmpty() && curEmail.equalsIgnoreCase(email.trim())) {
						canDelete = true;
					} else if (!uid.isEmpty() && uid.equals(curUser.getUid())) {
						canDelete = true;
					}
				}
				btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
				btnDelete.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showDeleteCommentConfirmDialog(commentId);
					}
				});
			}

			final List<HashMap<String, Object>> replies = repliesMap.get(commentId);
			final boolean isExpanded = expandedParents.contains(commentId);
			if (toggleBox != null && btnToggleReplies != null && repliesWrapper != null && repliesContainer != null) {
				if (replies != null && !replies.isEmpty()) {
					toggleBox.setVisibility(View.VISIBLE);
					btnToggleReplies.setText(isExpanded
						? "Sembunyikan balasan"
						: "Lihat " + replies.size() + " balasan");
					if (ivToggleChevron != null) {
						ivToggleChevron.setRotation(isExpanded ? 180f : 0f);
					}
					repliesWrapper.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
					repliesContainer.removeAllViews();

					if (isExpanded) {
						for (HashMap<String, Object> rItem : replies) {
							repliesContainer.addView(buildReplyRowView(repliesContainer.getContext(), rItem, d));
						}
					}

					toggleBox.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (expandedParents.contains(commentId)) {
								expandedParents.remove(commentId);
							} else {
								expandedParents.add(commentId);
							}
							notifyItemChanged(holder.getAdapterPosition());
						}
					});
				} else {
					toggleBox.setVisibility(View.GONE);
					repliesWrapper.setVisibility(View.GONE);
					repliesContainer.removeAllViews();
				}
			}
		}

		private View buildReplyRowView(Context ctx, HashMap<String, Object> rItem, float d) {
			String rEmail = rItem.get("email") != null ? rItem.get("email").toString() : "";
			String rUid = rItem.get("uid") != null ? rItem.get("uid").toString() : "";
			String rText = rItem.get("text") != null ? rItem.get("text").toString() : "";
			String rSticker = rItem.get("stickerUrl") != null ? rItem.get("stickerUrl").toString().trim() : "";
			long rTs = parseLongSafe(rItem.get("timestamp"), System.currentTimeMillis());

			HashMap<String, String> rProf = resolveUserProfile(rEmail, rUid);
			String rName = rProf.get("name");
			String rPhoto = rProf.get("photoURL");
			long rExp = parseLongSafe(rProf.get("exp"), 0L);
			boolean rAdmin = !rEmail.isEmpty() && ADMIN_EMAIL.equalsIgnoreCase(rEmail.trim());
			int rLevel = calculateLevel(rExp);

			LinearLayout row = new LinearLayout(ctx);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setGravity(Gravity.TOP);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			lp.bottomMargin = (int) (10 * d);
			row.setLayoutParams(lp);
			row.setPadding((int) (2 * d), (int) (2 * d), (int) (2 * d), (int) (2 * d));

			ImageView av = new ImageView(ctx);
			LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams((int) (24 * d), (int) (24 * d));
			avLp.rightMargin = (int) (8 * d);
			av.setLayoutParams(avLp);
			av.setScaleType(ImageView.ScaleType.CENTER_CROP);

			TextView avIni = new TextView(ctx);
			LinearLayout.LayoutParams iniLp = new LinearLayout.LayoutParams((int) (24 * d), (int) (24 * d));
			iniLp.rightMargin = (int) (8 * d);
			avIni.setLayoutParams(iniLp);
			avIni.setGravity(Gravity.CENTER);
			avIni.setTextColor(Color.WHITE);
			avIni.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
			avIni.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

			bindAvatarOrInitial(av, avIni, rPhoto, rName);

			LinearLayout col = new LinearLayout(ctx);
			col.setOrientation(LinearLayout.VERTICAL);
			col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

			LinearLayout nRow = new LinearLayout(ctx);
			nRow.setOrientation(LinearLayout.HORIZONTAL);
			nRow.setGravity(Gravity.CENTER_VERTICAL);

			TextView tvN = new TextView(ctx);
			tvN.setText(rName);
			tvN.setTextColor(Color.parseColor("#CBD5E1"));
			tvN.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
			tvN.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			nRow.addView(tvN);

			if (rAdmin) {
				TextView tvAdm = new TextView(ctx);
				tvAdm.setText("★ ADMIN");
				tvAdm.setTextColor(Color.parseColor("#FBBF24"));
				tvAdm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
				tvAdm.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
				tvAdm.setPadding((int) (6 * d), (int) (1 * d), (int) (6 * d), (int) (1 * d));
				LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				);
				aLp.leftMargin = (int) (6 * d);
				tvAdm.setLayoutParams(aLp);
				GradientDrawable aBg = new GradientDrawable();
				aBg.setCornerRadius(100f);
				aBg.setColor(Color.parseColor("#451A03"));
				aBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#F59E0B"));
				tvAdm.setBackground(aBg);
				nRow.addView(tvAdm);
			}

			TextView tvLvl = new TextView(ctx);
			tvLvl.setText(rAdmin && rExp == 0L ? "Lv999+" : "Lv" + rLevel);
			tvLvl.setTextColor(Color.parseColor("#38BDF8"));
			tvLvl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
			tvLvl.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvLvl.setPadding((int) (6 * d), (int) (1 * d), (int) (6 * d), (int) (1 * d));
			LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			lLp.leftMargin = (int) (5 * d);
			tvLvl.setLayoutParams(lLp);
			GradientDrawable lBg = new GradientDrawable();
			lBg.setCornerRadius(100f);
			lBg.setColor(Color.parseColor("#0C2438"));
			lBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#38BDF8"));
			tvLvl.setBackground(lBg);
			nRow.addView(tvLvl);

			TextView tvB = new TextView(ctx);
			tvB.setText(rText);
			tvB.setTextColor(Color.WHITE);
			tvB.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
			tvB.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvB.setVisibility(rText.isEmpty() ? View.GONE : View.VISIBLE);

			ImageView ivS = new ImageView(ctx);
			LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams((int) (92 * d), (int) (92 * d));
			sLp.topMargin = (int) (4 * d);
			ivS.setLayoutParams(sLp);
			ivS.setScaleType(ImageView.ScaleType.FIT_CENTER);
			if (!rSticker.isEmpty() && !isFinishing() && !isDestroyed()) {
				ivS.setVisibility(View.VISIBLE);
				Glide.with(WatchActivity.this).load(Uri.parse(rSticker)).into(ivS);
			} else {
				ivS.setVisibility(View.GONE);
			}

			TextView tvT = new TextView(ctx);
			tvT.setText(formatRelativeOrDateTime(rTs));
			tvT.setTextColor(Color.parseColor("#94A3B8"));
			tvT.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
			tvT.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

			col.addView(nRow);
			col.addView(tvB);
			col.addView(ivS);
			col.addView(tvT);

			row.addView(av);
			row.addView(avIni);
			row.addView(col);
			return row;
		}

		@Override
		public int getItemCount() {
			return mainItems.size();
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			ViewHolder(@NonNull View itemView) { super(itemView); }
		}
	}

	private void initialize(Bundle _savedInstanceState) {
		req_watch = new RequestNetwork(this);

		playerTextureView = findViewById(R.id.playerTextureView);
		layoutPlayer = findViewById(R.id.layoutPlayer);
		playerControlsOverlay = findViewById(R.id.playerControlsOverlay);
		gradientTop = findViewById(R.id.gradientTop);
		gradientBottom = findViewById(R.id.gradientBottom);
		topBar = findViewById(R.id.topBar);
		bottomControls = findViewById(R.id.bottomControls);
		dividerLineView = findViewById(R.id.dividerContainer);
		miniVideoProgressBar = findViewById(R.id.miniVideoProgressBar);

		animSeekLeft = findViewById(R.id.animSeekLeft);
		animSeekRight = findViewById(R.id.animSeekRight);
		btnUnlockScreen = findViewById(R.id.btnUnlockScreen);
		pillSpeedHold = findViewById(R.id.pillSpeedHold);

		btnBack = findViewById(R.id.btnBack);
		btnCast = findViewById(R.id.btnCast);
		btnLock = findViewById(R.id.btnLock);
		btnSettings = findViewById(R.id.btnSettings);
		btnPrevEpisode = findViewById(R.id.btnPrevEpisode);
		btnRewind = findViewById(R.id.btnRewind);
		btnPlay = findViewById(R.id.btnPlay);
		btnForward = findViewById(R.id.btnForward);
		btnNextEpisode = findViewById(R.id.btnNextEpisode);
		btnFullscreen = findViewById(R.id.btnFullscreen);
		imagePoster = findViewById(R.id.imagePoster);
		iconExpandDetail = findViewById(R.id.iconExpandDetail);

		textPlayerTitle = findViewById(R.id.textPlayerTitle);
		textTime = findViewById(R.id.textTime);
		btnFullscreenEpisodes = findViewById(R.id.btnFullscreenEpisodes);
		btnQuality = findViewById(R.id.btnQuality);
		textJudul = findViewById(R.id.textJudul);
		textRating = findViewById(R.id.textRating);
		textYear = findViewById(R.id.textYear);
		textGenre = findViewById(R.id.textGenre);
		textActionServer = findViewById(R.id.textActionServer);
		textSortEpisodes = findViewById(R.id.textSortEpisodes);

		btnCommentSection = findViewById(R.id.btnCommentSection);
		textCommentPreviewLabel = findViewById(R.id.textCommentPreviewLabel);
		textCommentCount = findViewById(R.id.textCommentCount);
		iconCommentChevron = findViewById(R.id.iconCommentChevron);

		btnFill = findViewById(R.id.btnFill);
		btnOpenDetail = findViewById(R.id.btnOpenDetail);
		btnSortEpisodes = findViewById(R.id.btnSortEpisodes);
		btnActionServer = findViewById(R.id.btnActionServer);
		btnActionDownload = findViewById(R.id.btnActionDownload);
		btnActionShare = findViewById(R.id.btnActionShare);
		btnActionReport = findViewById(R.id.btnActionReport);

		playerLoading = findViewById(R.id.playerLoading);
		playerSeekBar = findViewById(R.id.playerSeekBar);
		recyclerEpisode = findViewById(R.id.recyclerEpisode);
		recyclerRekomendasi = findViewById(R.id.recyclerRekomendasi);

		_req_watch_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				if (_tag.equals("slug")) {
					DongflixPlayerManager.getInstance().cachedEpisodesJson = _response;
					handleEpisodesJson(_response, false);
				} else {
					DongflixPlayerManager.getInstance().cachedAllCatalogJson = _response;
					handleCatalogJson(_response);
				}
			}

			@Override
			public void onErrorResponse(String _param1, String _param2) {
				if (_param1.equals("slug")) isEpisodesLoading = false;
				else {
					isDetailLoading = false;
					isRekomLoading = false;
					clearDetailShimmer();
				}
				checkStopShimmerIfDone();
			}
		};
	}

	private void handleEpisodesJson(String _response, boolean fromCache) {
		isEpisodesLoading = false;
		eps = new Gson().fromJson(_response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
		if (eps != null && !eps.isEmpty()) {
			Collections.reverse(eps);
			isNewestFirst = true;
			if (textSortEpisodes != null) textSortEpisodes.setText("Terbaru");

			if (recyclerEpisode != null) {
				recyclerEpisode.setAdapter(new RecyclerEpisodeAdapter(eps));
			}
			episodeList = eps;

			if (fromCache || (isResumingFromMiniPlayer && mediaPlayer != null)) {
				DongflixPlayerManager mgr = DongflixPlayerManager.getInstance();
				currentEpisodeIndex = mgr.activeEpisodeIndex;
				if (recyclerEpisode != null) {
					recyclerEpisode.scrollToPosition(currentEpisodeIndex);
					if (recyclerEpisode.getAdapter() != null) {
						recyclerEpisode.getAdapter().notifyDataSetChanged();
					}
				}
				updateEpisodeNavButtons();
			} else {
				tryStartEpisodeFromHistoryIfReady();
			}
		}
		checkStopShimmerIfDone();
	}

	private void handleCatalogJson(String _response) {
		isDetailLoading = false;
		isRekomLoading = false;
		clearDetailShimmer();

		String targetSlug = currentSlug;
		if (targetSlug != null && !targetSlug.isEmpty()) {
			try {
				JSONArray jsonArray = new JSONArray(_response);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject obj = jsonArray.getJSONObject(i);
					if (targetSlug.equals(obj.optString("slug"))) {
						detailTitle = obj.optString("title");
						detailSynopsis = obj.optString("synopsis")
								.replaceAll("\\[[^\\x20-\\x7E]+\\]", "")
								.replaceAll("[^\\x20-\\x7E\\n]", "")
								.trim();
						detailStatus = obj.optString("status", "Ongoing");
						if (detailStatus.isEmpty()) detailStatus = "Ongoing";

						detailCover = obj.optString("cover").trim();
						if (detailCover.contains("i.ibb.co.com")) {
							detailCover = detailCover.replace("i.ibb.co.com", "i.ibb.co");
						}
						if (detailCover.startsWith("http://")) {
							detailCover = detailCover.replace("http://", "https://");
						}
						String released = obj.optString("released");

						detailFirstGenre = "-";
						currentGenresList.clear();
						StringBuilder allGenresSb = new StringBuilder();
						JSONArray genresArray = obj.optJSONArray("genres");
						if (genresArray != null && genresArray.length() > 0) {
							detailFirstGenre = genresArray.optString(0);
							for (int g = 0; g < genresArray.length(); g++) {
								String gStr = genresArray.optString(g).trim();
								if (!gStr.isEmpty()) {
									currentGenresList.add(gStr.toLowerCase());
									if (allGenresSb.length() > 0) allGenresSb.append(", ");
									allGenresSb.append(gStr);
								}
							}
						}
						detailAllGenres = allGenresSb.length() > 0 ? allGenresSb.toString() : detailFirstGenre;

						if (released.length() >= 4) {
							detailYear = released.substring(released.length() - 4);
						}

						if (textJudul != null) textJudul.setText(detailTitle);
						if (textRating != null) textRating.setText(detailStatus);
						if (textYear != null) textYear.setText(detailYear);
						if (textGenre != null) textGenre.setText(detailFirstGenre);

						if (imagePoster != null && !detailCover.isEmpty()) {
							Glide.with(getApplicationContext()).load(Uri.parse(detailCover)).into(imagePoster);
						}

						DongflixPlayerManager mgr = DongflixPlayerManager.getInstance();
						mgr.activeTitle = detailTitle;
						mgr.activeCoverUrl = detailCover;
						break;
					}
				}

				ArrayList<HashMap<String, Object>> allList = new Gson().fromJson(_response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
				if (allList != null && recyclerRekomendasi != null) {
					ArrayList<HashMap<String, Object>> smartRekomList = getSmartRecommendations(allList, targetSlug, 6);
					recyclerRekomendasi.setAdapter(new RecyclerRekomendasiAdapter(smartRekomList));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		applyOutfitToAllViews();
		checkStopShimmerIfDone();
	}

	private Set<String> extractKeywords(String text) {
		Set<String> keywords = new HashSet<>();
		if (text == null || text.isEmpty()) return keywords;

		Set<String> stopWords = new HashSet<>(Arrays.asList(
			"yang", "dan", "di", "ke", "dari", "ini", "itu", "untuk", "dengan", "pada", "adalah",
			"sebagai", "dalam", "tidak", "akan", "oleh", "ia", "dia", "mereka", "nya", "atau",
			"the", "of", "and", "in", "to", "a", "is", "for", "with", "on", "as", "by", "at",
			"sub", "indo", "episode", "season", "donghua", "anime", "cerita", "kisah", "dunia"
		));

		String[] tokens = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
		for (String t : tokens) {
			if (t.length() >= 3 && !stopWords.contains(t)) {
				keywords.add(t);
			}
		}
		return keywords;
	}

	private ArrayList<HashMap<String, Object>> getSmartRecommendations(
			ArrayList<HashMap<String, Object>> allList,
			String currentSlug,
			int maxCount) {

		final Set<String> baseTitleWords = extractKeywords(detailTitle);
		final Set<String> baseSynopsisWords = extractKeywords(detailSynopsis);
		final Set<String> baseGenres = new HashSet<>(currentGenresList);

		ArrayList<ScoredAnimeItem> scoredItems = new ArrayList<>();

		for (HashMap<String, Object> item : allList) {
			String slug = item.get("slug") != null ? item.get("slug").toString() : "";
			if (slug.isEmpty() || slug.equals(currentSlug)) continue;

			int score = 0;

			Object rawGenres = item.get("genres");
			if (rawGenres instanceof List) {
				for (Object gObj : (List<?>) rawGenres) {
					if (gObj != null && baseGenres.contains(gObj.toString().trim().toLowerCase())) {
						score += 12;
					}
				}
			} else if (rawGenres != null) {
				String gStr = rawGenres.toString().toLowerCase();
				for (String bg : baseGenres) {
					if (gStr.contains(bg)) score += 12;
				}
			}

			String itemTitle = item.get("title") != null ? item.get("title").toString() : "";
			Set<String> itemTitleWords = extractKeywords(itemTitle);
			for (String w : itemTitleWords) {
				if (baseTitleWords.contains(w)) {
					score += 15;
				}
			}

			String itemSynopsis = item.get("synopsis") != null ? item.get("synopsis").toString() : "";
			Set<String> itemSynopsisWords = extractKeywords(itemSynopsis);
			for (String sw : itemSynopsisWords) {
				if (baseSynopsisWords.contains(sw)) {
					score += 4;
				}
			}

			String itemStatus = item.get("status") != null ? item.get("status").toString() : "";
			if (!detailStatus.isEmpty() && detailStatus.equalsIgnoreCase(itemStatus)) {
				score += 2;
			}

			scoredItems.add(new ScoredAnimeItem(item, score));
		}

		Collections.sort(scoredItems, new Comparator<ScoredAnimeItem>() {
			@Override
			public int compare(ScoredAnimeItem a, ScoredAnimeItem b) {
				return Integer.compare(b.score, a.score);
			}
		});

		ArrayList<HashMap<String, Object>> result = new ArrayList<>();
		for (int i = 0; i < Math.min(maxCount, scoredItems.size()); i++) {
			result.add(scoredItems.get(i).item);
		}
		return result;
	}

	private Drawable createShimmerDrawable(final float cornerRadiusPx) {
		return new Drawable() {
			private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Matrix shaderMatrix = new Matrix();
			private LinearGradient gradient;
			private int lastWidth = -1;

			{
				basePaint.setColor(Color.parseColor("#12141A"));
			}

			@Override
			public void draw(Canvas canvas) {
				Rect b = getBounds();
				int w = b.width();
				int h = b.height();
				if (w <= 0 || h <= 0) return;

				RectF rect = new RectF(b);
				canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, basePaint);

				if (gradient == null || lastWidth != w) {
					lastWidth = w;
					gradient = new LinearGradient(
						0, 0, w, 0,
						new int[]{
							Color.TRANSPARENT,
							Color.parseColor("#1AFFFFFF"),
							Color.parseColor("#33FFFFFF"),
							Color.parseColor("#1AFFFFFF"),
							Color.TRANSPARENT
						},
						new float[]{0f, 0.35f, 0.5f, 0.65f, 1f},
						Shader.TileMode.CLAMP
					);
					shimmerPaint.setShader(gradient);
				}

				float translateX = -w + (2f * w * shimmerPhase);
				shaderMatrix.setTranslate(translateX, 0);
				gradient.setLocalMatrix(shaderMatrix);
				canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shimmerPaint);
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	private void startLoadingShimmer() {
		float d = getResources().getDisplayMetrics().density;

		if (imagePoster != null) {
			imagePoster.setImageDrawable(createShimmerDrawable(32 * d));
		}
		if (textJudul != null) {
			textJudul.setText("                    ");
			textJudul.setBackground(createShimmerDrawable(6 * d));
		}
		if (textRating != null) {
			textRating.setText("        ");
			textRating.setBackground(createShimmerDrawable(4 * d));
		}
		if (textYear != null) {
			textYear.setText("      ");
			textYear.setBackground(createShimmerDrawable(4 * d));
		}
		if (textGenre != null) {
			textGenre.setText("        ");
			textGenre.setBackground(createShimmerDrawable(4 * d));
		}

		if (recyclerEpisode != null) {
			recyclerEpisode.setAdapter(new ShimmerEpisodeAdapter(6));
		}

		if (recyclerRekomendasi != null) {
			recyclerRekomendasi.setAdapter(new ShimmerRekomendasiAdapter(6));
		}

		shimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
		shimmerAnimator.setDuration(1250);
		shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
		shimmerAnimator.setInterpolator(new LinearInterpolator());
		shimmerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				shimmerPhase = (float) animation.getAnimatedValue();
				if (isDetailLoading) {
					if (imagePoster != null) imagePoster.invalidate();
					if (textJudul != null) textJudul.invalidate();
					if (textRating != null) textRating.invalidate();
					if (textYear != null) textYear.invalidate();
					if (textGenre != null) textGenre.invalidate();
				}
				if (isEpisodesLoading && recyclerEpisode != null) {
					for (int i = 0; i < recyclerEpisode.getChildCount(); i++) {
						recyclerEpisode.getChildAt(i).invalidate();
					}
				}
				if (isRekomLoading && recyclerRekomendasi != null) {
					for (int i = 0; i < recyclerRekomendasi.getChildCount(); i++) {
						View child = recyclerRekomendasi.getChildAt(i);
						child.invalidate();
						if (child instanceof ViewGroup) {
							ViewGroup vg = (ViewGroup) child;
							for (int j = 0; j < vg.getChildCount(); j++) {
								vg.getChildAt(j).invalidate();
							}
						}
					}
				}
			}
		});
		shimmerAnimator.start();
	}

	private void clearDetailShimmer() {
		if (textJudul != null) textJudul.setBackground(null);
		if (textRating != null) textRating.setBackground(null);
		if (textYear != null) textYear.setBackground(null);
		if (textGenre != null) textGenre.setBackground(null);
	}

	private void checkStopShimmerIfDone() {
		if (!isDetailLoading && !isEpisodesLoading && !isRekomLoading) {
			if (shimmerAnimator != null) {
				shimmerAnimator.cancel();
				shimmerAnimator = null;
			}
		}
	}

	private void initializeLogic() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(0xFF000000);
		}

		View rootWatch = findViewById(R.id.rootWatchLayout);
		if (rootWatch != null) {
			rootWatch.setPadding(0, 0, 0, 0);
		}

		if (btnLock != null) {
			btnLock.setVisibility(View.GONE);
		}

		String slug = getIntent().getStringExtra("slug");
		currentSlug = slug != null ? slug.trim() : "";

		_WatchUI();
		setupCommentSection();
		updateBottomDividerState();
		loadUsersAndStickersFromFirebase();

		if (recyclerEpisode != null) {
			recyclerEpisode.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
		}

		if (recyclerRekomendasi != null) {
			recyclerRekomendasi.setLayoutManager(new GridLayoutManager(this, 3));
			recyclerRekomendasi.setNestedScrollingEnabled(false);
		}

		DongflixPlayerManager mgr = DongflixPlayerManager.getInstance();
		boolean resumeFlag = getIntent().getBooleanExtra("resume_mini_player", false);

		if ((resumeFlag || (mgr.isMiniPlayerActive && currentSlug.equals(mgr.activeSlug))) && mgr.sharedMediaPlayer != null) {
			isResumingFromMiniPlayer = true;
			initialEpisodeStarted = true;
			mediaPlayer = mgr.sharedMediaPlayer;
			mgr.isMiniPlayerActive = false;

			currentPlayingUrl = mgr.activeStreamUrl;
			currentEpisodeIndex = mgr.activeEpisodeIndex;
			selectedServerIndex = mgr.activeSelectedServerIndex;
			selectedQualityIndex = mgr.activeSelectedQualityIndex;
			isAutoQualityMode = mgr.activeIsAutoQuality;

			if (!mgr.activeEpisodeList.isEmpty()) {
				episodeList = new ArrayList<>(mgr.activeEpisodeList);
			}
			if (!mgr.activeServerNames.isEmpty()) {
				currentEpisodeServerNames = new ArrayList<>(mgr.activeServerNames);
				currentEpisodeServerUrls = new ArrayList<>(mgr.activeServerUrls);
			}
			if (!mgr.activeQualityNames.isEmpty()) {
				qualityNames = new ArrayList<>(mgr.activeQualityNames);
				qualityUrls = new ArrayList<>(mgr.activeQualityUrls);
				updateQualityBadgeText();
			}
			if (textActionServer != null && !mgr.activeServerName.isEmpty()) {
				textActionServer.setText(mgr.activeServerName);
			}

			if (!mgr.cachedAllCatalogJson.isEmpty()) {
				handleCatalogJson(mgr.cachedAllCatalogJson);
			} else {
				req_watch.startRequestNetwork("GET", CATALOG_URL, "", _req_watch_request_listener);
			}

			if (!mgr.cachedEpisodesJson.isEmpty()) {
				handleEpisodesJson(mgr.cachedEpisodesJson, true);
			} else {
				req_watch.startRequestNetwork("GET", CATALOG_URL + "?slug=" + currentSlug, "slug", _req_watch_request_listener);
			}
		} else {
			mgr.stopAndCloseEverything(this);
			startLoadingShimmer();
			loadWatchHistoryFromFirebase();
			req_watch.startRequestNetwork("GET", CATALOG_URL, "", _req_watch_request_listener);
			req_watch.startRequestNetwork("GET", CATALOG_URL + "?slug=" + currentSlug, "slug", _req_watch_request_listener);
		}

		_initPlayerViews();
		applyOutfitToAllViews();

		if (isResumingFromMiniPlayer && mediaPlayer != null) {
			try {
				int dur = mediaPlayer.getDuration();
				if (playerSeekBar != null && dur > 0) playerSeekBar.setMax(dur);
				if (btnPlay != null) {
					btnPlay.setEnabled(true);
					btnPlay.setAlpha(1.0f);
					btnPlay.setImageResource(mediaPlayer.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play1);
				}
				int vw = mediaPlayer.getVideoWidth();
				int vh = mediaPlayer.getVideoHeight();
				if (vw > 0 && vh > 0) {
					currentVideoWidth = vw;
					currentVideoHeight = vh;
					adjustVideoAspectRatio();
				}
				if (playerHandler != null && progressLoop != null) {
					playerHandler.removeCallbacks(progressLoop);
					playerHandler.post(progressLoop);
				}
			} catch (Exception ignored) {}
		}
	}

	public void _WatchUI() {
		GradientDrawable topGrad = new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{0xCC000000, 0x00000000}
		);
		if (gradientTop != null) gradientTop.setBackground(topGrad);

		GradientDrawable botGrad = new GradientDrawable(
			GradientDrawable.Orientation.BOTTOM_TOP,
			new int[]{0xCC000000, 0x00000000}
		);
		if (gradientBottom != null) gradientBottom.setBackground(botGrad);

		View[] seekPills = {animSeekLeft, animSeekRight, pillSpeedHold};
		for (View pill : seekPills) {
			if (pill != null) {
				GradientDrawable pillBg = new GradientDrawable();
				pillBg.setCornerRadius(100f);
				pillBg.setColor(0xB3000000);
				pill.setBackground(pillBg);
			}
		}

		if (btnUnlockScreen != null) {
			GradientDrawable lockBg = new GradientDrawable();
			lockBg.setCornerRadius(100f);
			lockBg.setColor(0xCC0B0D11);
			btnUnlockScreen.setBackground(lockBg);
		}

		if (btnFullscreenEpisodes != null) {
			btnFullscreenEpisodes.setBackgroundColor(Color.TRANSPARENT);
		}

		if (iconExpandDetail != null) {
			GradientDrawable expandBg = new GradientDrawable();
			expandBg.setShape(GradientDrawable.OVAL);
			expandBg.setColor(Color.parseColor("#12141A"));
			iconExpandDetail.setBackground(expandBg);
		}

		if (btnSortEpisodes != null) {
			GradientDrawable sortBg = new GradientDrawable();
			sortBg.setCornerRadius(100f);
			sortBg.setColor(Color.parseColor("#12141A"));
			btnSortEpisodes.setBackground(sortBg);
		}

		View[] actionBtns = {btnActionServer, btnActionDownload, btnActionShare, btnActionReport};
		for (View btn : actionBtns) {
			if (btn != null) {
				GradientDrawable btnBg = new GradientDrawable();
				btnBg.setCornerRadius(100f);
				btnBg.setColor(Color.parseColor("#12141A"));
				btn.setBackground(btnBg);
			}
		}
	}

	private void updateBottomDividerState() {
		if (dividerLineView == null || miniVideoProgressBar == null) return;

		if (isFullscreen) {
			dividerLineView.setVisibility(View.GONE);
		} else {
			dividerLineView.setVisibility(View.VISIBLE);
			miniVideoProgressBar.setVisibility(areControlsVisible ? View.INVISIBLE : View.VISIBLE);
		}
	}

	private void applyPlaybackSpeedValue(float speed) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
			try {
				boolean wasPlaying = mediaPlayer.isPlaying();
				PlaybackParams params = mediaPlayer.getPlaybackParams();
				if (params == null) params = new PlaybackParams();
				params.setSpeed(speed);
				mediaPlayer.setPlaybackParams(params);
				if (!wasPlaying) mediaPlayer.pause();
			} catch (Exception ignored) {}
		}
	}

	private void startHold2xSpeed() {
		if (isScreenLocked || mediaPlayer == null || !mediaPlayer.isPlaying()) return;
		isHolding2xSpeed = true;
		applyPlaybackSpeedValue(2.0f);
		if (pillSpeedHold != null) {
			pillSpeedHold.setVisibility(View.VISIBLE);
			pillSpeedHold.bringToFront();
		}
	}

	private void stopHold2xSpeed() {
		if (!isHolding2xSpeed) return;
		isHolding2xSpeed = false;
		applyPlaybackSpeedValue(currentPlaybackSpeed);
		if (pillSpeedHold != null) {
			pillSpeedHold.setVisibility(View.GONE);
		}
	}

	public void adjustVideoAspectRatio() {
		if (playerTextureView == null || layoutPlayer == null) return;
		if (currentVideoWidth <= 0 || currentVideoHeight <= 0) return;

		layoutPlayer.post(new Runnable() {
			@Override
			public void run() {
				int containerWidth = layoutPlayer.getWidth();
				int containerHeight = layoutPlayer.getHeight();
				if (containerWidth <= 0 || containerHeight <= 0) return;

				float videoAspect = (float) currentVideoWidth / currentVideoHeight;
				float containerAspect = (float) containerWidth / containerHeight;

				int targetWidth;
				int targetHeight;

				if (isFillModeActive) {
					if (videoAspect > containerAspect) {
						targetHeight = containerHeight;
						targetWidth = Math.round(containerHeight * videoAspect);
					} else {
						targetWidth = containerWidth;
						targetHeight = Math.round(containerWidth / videoAspect);
					}
				} else {
					if (videoAspect > containerAspect) {
						targetWidth = containerWidth;
						targetHeight = Math.round(containerWidth / videoAspect);
					} else {
						targetHeight = containerHeight;
						targetWidth = Math.round(containerHeight * videoAspect);
					}
				}

				FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) playerTextureView.getLayoutParams();
				lp.width = targetWidth;
				lp.height = targetHeight;
				lp.gravity = Gravity.CENTER;
				playerTextureView.setLayoutParams(lp);
				playerTextureView.setScaleX(1.0f);
				playerTextureView.setScaleY(1.0f);
			}
		});
	}

	private void showSeekAnimation(final View animView) {
		if (animView == null) return;
		animView.animate().cancel();
		animView.setVisibility(View.VISIBLE);
		animView.setAlpha(1f);
		animView.animate()
			.alpha(0f)
			.setStartDelay(450)
			.setDuration(250)
			.withEndAction(new Runnable() {
				@Override
				public void run() {
					animView.setVisibility(View.GONE);
				}
			})
			.start();
	}

	private void lockScreenControls() {
		isScreenLocked = true;
		hideControls();
		showUnlockPillTemporarily();
		Toast.makeText(this, "Layar dikunci", Toast.LENGTH_SHORT).show();
	}

	private void unlockScreenControls() {
		isScreenLocked = false;
		if (btnUnlockScreen != null) {
			btnUnlockScreen.setVisibility(View.GONE);
		}
		if (playerHandler != null && hideUnlockBtnRunnable != null) {
			playerHandler.removeCallbacks(hideUnlockBtnRunnable);
		}
		showControls();
		Toast.makeText(this, "Kunci layar dibuka", Toast.LENGTH_SHORT).show();
	}

	private void showUnlockPillTemporarily() {
		if (btnUnlockScreen == null) return;
		btnUnlockScreen.setVisibility(View.VISIBLE);
		btnUnlockScreen.bringToFront();
		if (playerHandler != null && hideUnlockBtnRunnable != null) {
			playerHandler.removeCallbacks(hideUnlockBtnRunnable);
			playerHandler.postDelayed(hideUnlockBtnRunnable, 2500);
		}
	}

	public void toggleSortEpisodes() {
		if (episodeList == null || episodeList.size() <= 1) return;

		HashMap<String, Object> currentlyPlayingItem = episodeList.get(currentEpisodeIndex);
		Collections.reverse(episodeList);
		isNewestFirst = !isNewestFirst;

		for (int i = 0; i < episodeList.size(); i++) {
			if (episodeList.get(i) == currentlyPlayingItem) {
				currentEpisodeIndex = i;
				break;
			}
		}

		if (textSortEpisodes != null) {
			textSortEpisodes.setText(isNewestFirst ? "Terbaru" : "Terlama");
		}

		if (recyclerEpisode != null && recyclerEpisode.getAdapter() != null) {
			recyclerEpisode.getAdapter().notifyDataSetChanged();
			recyclerEpisode.scrollToPosition(currentEpisodeIndex);
		}
		updateEpisodeNavButtons();
	}

	private boolean isLandscapeOrFullscreen() {
		return isFullscreen || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
	}

	private Dialog createAdaptiveSheetDialog(View contentView) {
		return createAdaptiveSheetDialog(contentView, false);
	}

	private Dialog createAdaptiveSheetDialog(View contentView, final boolean allowVideoInteraction) {
		float d = getResources().getDisplayMetrics().density;

		if (isLandscapeOrFullscreen()) {
			final Dialog sideDialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
			sideDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			GradientDrawable panelBg = new GradientDrawable();
			panelBg.setColor(Color.parseColor("#0B0D11"));
			panelBg.setCornerRadii(new float[]{18 * d, 18 * d, 0, 0, 0, 0, 18 * d, 18 * d});
			contentView.setBackground(panelBg);

			if (contentView instanceof ViewGroup && ((ViewGroup) contentView).getChildCount() > 0) {
				View firstChild = ((ViewGroup) contentView).getChildAt(0);
				if (!(firstChild instanceof LinearLayout)) {
					firstChild.setVisibility(View.GONE);
				}
			}
			contentView.setPadding((int)(22 * d), (int)(20 * d), (int)(22 * d), (int)(20 * d));

			sideDialog.setContentView(contentView);
			sideDialog.setCanceledOnTouchOutside(true);

			Window window = sideDialog.getWindow();
			if (window != null) {
				window.setLayout((int)(340 * d), ViewGroup.LayoutParams.MATCH_PARENT);
				window.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
				window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				window.setDimAmount(0.45f);
				window.getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				);
			}

			sideDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if (isFullscreen) {
						getWindow().getDecorView().setSystemUiVisibility(
							View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						);
					}
				}
			});

			sideDialog.show();
			return sideDialog;
		} else {
			final BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
			final BottomSheetBehavior<FrameLayout>[] sheetBehaviorHolder = new BottomSheetBehavior[1];
			final ScrollView[] scrollableChildHolder = new ScrollView[1];

			if (contentView instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup) contentView;
				for (int i = 0; i < vg.getChildCount(); i++) {
					View child = vg.getChildAt(i);
					if (child instanceof ScrollView) {
						final ScrollView sv = (ScrollView) child;
						scrollableChildHolder[0] = sv;
						sv.setNestedScrollingEnabled(true);
						sv.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
							@Override
							public void onScrollChanged() {
								if (sheetBehaviorHolder[0] != null) {
									sheetBehaviorHolder[0].setDraggable(sv.getScrollY() <= 0);
								}
							}
						});
					}
				}
			}

			sheetDialog.setContentView(contentView);

			if (allowVideoInteraction) {
				sheetDialog.setCanceledOnTouchOutside(false);
				Window sheetWindow = sheetDialog.getWindow();
				if (sheetWindow != null) {
					sheetWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
					sheetWindow.setDimAmount(0f);
					sheetWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
					sheetWindow.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
				}
			}

			sheetDialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					BottomSheetDialog dialogObj = (BottomSheetDialog) dialog;
					FrameLayout bottomSheet = dialogObj.findViewById(com.google.android.material.R.id.design_bottom_sheet);
					if (bottomSheet != null) {
						bottomSheet.setBackgroundColor(Color.TRANSPARENT);
						bottomSheet.setBackgroundTintList(null);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
							bottomSheet.setElevation(0f);
						}

						Rect visibleFrame = new Rect();
						getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleFrame);

						int playerBottom = 0;
						if (layoutPlayer != null) {
							int[] loc = new int[2];
							layoutPlayer.getLocationOnScreen(loc);
							playerBottom = loc[1] + layoutPlayer.getHeight();
						}

						int targetSheetHeight = visibleFrame.bottom - playerBottom;
						if (targetSheetHeight > (int)(200 * getResources().getDisplayMetrics().density)) {
							ViewGroup.LayoutParams sheetLp = bottomSheet.getLayoutParams();
							sheetLp.height = targetSheetHeight;
							bottomSheet.setLayoutParams(sheetLp);
						}

						BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
						sheetBehaviorHolder[0] = behavior;
						behavior.setPeekHeight(targetSheetHeight);
						behavior.setSkipCollapsed(true);
						behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

						boolean isAtTop = scrollableChildHolder[0] == null || scrollableChildHolder[0].getScrollY() <= 0;
						behavior.setDraggable(isAtTop);
					}
				}
			});
			sheetDialog.show();
			return sheetDialog;
		}
	}

	private LinearLayout buildAdaptiveHeader(String titleText, final Dialog[] holderDialog) {
		float d = getResources().getDisplayMetrics().density;

		LinearLayout header = new LinearLayout(this);
		header.setOrientation(LinearLayout.HORIZONTAL);
		header.setGravity(Gravity.CENTER_VERTICAL);
		header.setPadding(0, 0, 0, (int)(16 * d));

		TextView tvTitle = new TextView(this);
		tvTitle.setText(titleText);
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(19f);
		tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		header.addView(tvTitle);

		TextView btnClose = new TextView(this);
		LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams((int)(32 * d), (int)(32 * d));
		btnClose.setLayoutParams(closeLp);
		btnClose.setGravity(Gravity.CENTER);
		btnClose.setText("✕");
		btnClose.setTextColor(Color.parseColor("#CBD5E1"));
		btnClose.setTextSize(14f);
		btnClose.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

		GradientDrawable closeBg = new GradientDrawable();
		closeBg.setShape(GradientDrawable.OVAL);
		closeBg.setColor(Color.parseColor("#181B22"));
		btnClose.setBackground(closeBg);

		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (holderDialog[0] != null) holderDialog[0].dismiss();
			}
		});
		header.addView(btnClose);

		return header;
	}

	public void showSettingsBottomSheet() {
		final Dialog[] dialogHolder = new Dialog[1];
		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int)(20 * d), (int)(12 * d), (int)(20 * d), (int)(24 * d));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0B0D11"));
		sheetBg.setCornerRadii(new float[]{22 * d, 22 * d, 22 * d, 22 * d, 0, 0, 0, 0});
		root.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int)(38 * d), (int)(4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int)(12 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#1E222B"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		root.addView(buildAdaptiveHeader("Pengaturan Pemutaran", dialogHolder));

		String currentQualityLabel = (btnQuality != null) ? btnQuality.getText().toString() : "Auto";
		String currentSpeedLabel = (Math.abs(currentPlaybackSpeed - 1.0f) < 0.01f) ? "Normal" : currentPlaybackSpeed + "x";
		String currentServerLabel = (textActionServer != null) ? textActionServer.getText().toString() : "Default";

		String[] menuTitles = new String[]{"Kualitas", "Kecepatan Putar", "Server Streaming"};
		String[] menuValues = new String[]{currentQualityLabel, currentSpeedLabel, currentServerLabel};

		for (int i = 0; i < menuTitles.length; i++) {
			final int idx = i;
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setGravity(Gravity.CENTER_VERTICAL);
			row.setPadding((int)(4 * d), (int)(16 * d), (int)(4 * d), (int)(16 * d));

			TextView tvLeft = new TextView(this);
			tvLeft.setText(menuTitles[i]);
			tvLeft.setTextColor(Color.WHITE);
			tvLeft.setTextSize(15f);
			tvLeft.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvLeft.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
			row.addView(tvLeft);

			TextView tvRight = new TextView(this);
			tvRight.setText(menuValues[i] + "  >");
			tvRight.setTextColor(Color.parseColor("#D1D5DB"));
			tvRight.setTextSize(14f);
			tvRight.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			row.addView(tvRight);

			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (dialogHolder[0] != null) dialogHolder[0].dismiss();
					if (idx == 0) showQualityDialog();
					else if (idx == 1) showSpeedDialog();
					else if (idx == 2) showServerDialog();
				}
			});

			root.addView(row);
		}

		dialogHolder[0] = createAdaptiveSheetDialog(root);
	}

	public void showDetailBottomSheet() {
		final Dialog[] dialogHolder = new Dialog[1];
		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int)(20 * d), (int)(12 * d), (int)(20 * d), (int)(28 * d));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0B0D11"));
		sheetBg.setCornerRadii(new float[]{22 * d, 22 * d, 22 * d, 22 * d, 0, 0, 0, 0});
		root.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int)(38 * d), (int)(4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int)(12 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#1E222B"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		root.addView(buildAdaptiveHeader("Sinopsis", dialogHolder));

		ScrollView scrollView = new ScrollView(this);
		scrollView.setVerticalScrollBarEnabled(false);

		LinearLayout content = new LinearLayout(this);
		content.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(content);

		LinearLayout infoHeaderRow = new LinearLayout(this);
		infoHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
		infoHeaderRow.setGravity(Gravity.TOP);
		LinearLayout.LayoutParams infoRowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		infoRowLp.bottomMargin = (int)(18 * d);
		infoHeaderRow.setLayoutParams(infoRowLp);

		CardView posterCard = new CardView(this);
		LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams((int)(96 * d), (int)(134 * d));
		cardLp.rightMargin = (int)(16 * d);
		posterCard.setLayoutParams(cardLp);
		posterCard.setRadius(8 * d);
		posterCard.setCardElevation(0f);
		posterCard.setCardBackgroundColor(Color.parseColor("#12141A"));

		ImageView ivSheetPoster = new ImageView(this);
		ivSheetPoster.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		ivSheetPoster.setScaleType(ImageView.ScaleType.CENTER_CROP);
		if (!detailCover.isEmpty()) {
			Glide.with(getApplicationContext()).load(Uri.parse(detailCover)).into(ivSheetPoster);
		}
		posterCard.addView(ivSheetPoster);
		infoHeaderRow.addView(posterCard);

		LinearLayout rightCol = new LinearLayout(this);
		rightCol.setOrientation(LinearLayout.VERTICAL);
		rightCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		TextView tvSheetTitle = new TextView(this);
		tvSheetTitle.setText(detailTitle.isEmpty() ? textJudul.getText().toString() : detailTitle);
		tvSheetTitle.setTextColor(Color.WHITE);
		tvSheetTitle.setTextSize(18f);
		tvSheetTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvSheetTitle.setMaxLines(2);
		tvSheetTitle.setEllipsize(TextUtils.TruncateAt.END);
		rightCol.addView(tvSheetTitle);

		TextView tvSubGenres = new TextView(this);
		tvSubGenres.setText(detailAllGenres);
		tvSubGenres.setTextColor(Color.parseColor("#9CA3AF"));
		tvSubGenres.setTextSize(13f);
		tvSubGenres.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvSubGenres.setMaxLines(1);
		tvSubGenres.setEllipsize(TextUtils.TruncateAt.END);
		tvSubGenres.setPadding(0, (int)(6 * d), 0, (int)(10 * d));
		rightCol.addView(tvSubGenres);

		LinearLayout badgeRow = new LinearLayout(this);
		badgeRow.setOrientation(LinearLayout.HORIZONTAL);
		badgeRow.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams badgeRowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		badgeRowLp.bottomMargin = (int)(12 * d);
		badgeRow.setLayoutParams(badgeRowLp);

		String[] badgeItems = new String[]{detailStatus, detailYear, detailFirstGenre};
		for (int b = 0; b < badgeItems.length; b++) {
			TextView bView = new TextView(this);
			bView.setText(badgeItems[b]);
			bView.setTextSize(12f);
			bView.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			bView.setTextColor(Color.parseColor("#CBD5E1"));
			bView.setPadding((int)(8 * d), (int)(4 * d), (int)(8 * d), (int)(4 * d));

			GradientDrawable bBg = new GradientDrawable();
			bBg.setCornerRadius(5 * d);
			bBg.setColor(Color.parseColor("#12141A"));
			bView.setBackground(bBg);

			LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			if (b < badgeItems.length - 1) bLp.rightMargin = (int)(8 * d);
			bView.setLayoutParams(bLp);
			badgeRow.addView(bView);
		}
		rightCol.addView(badgeRow);

		LinearLayout studioRow = new LinearLayout(this);
		studioRow.setOrientation(LinearLayout.HORIZONTAL);
		studioRow.setGravity(Gravity.CENTER_VERTICAL);

		CardView miniThumb = new CardView(this);
		LinearLayout.LayoutParams miniLp = new LinearLayout.LayoutParams((int)(22 * d), (int)(22 * d));
		miniLp.rightMargin = (int)(8 * d);
		miniThumb.setLayoutParams(miniLp);
		miniThumb.setRadius(11 * d);
		miniThumb.setCardElevation(0f);
		miniThumb.setCardBackgroundColor(Color.parseColor("#12141A"));

		ImageView ivMini = new ImageView(this);
		ivMini.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		ivMini.setScaleType(ImageView.ScaleType.CENTER_CROP);
		if (!detailCover.isEmpty()) {
			Glide.with(getApplicationContext()).load(Uri.parse(detailCover)).into(ivMini);
		}
		miniThumb.addView(ivMini);
		studioRow.addView(miniThumb);

		TextView tvEpSummary = new TextView(this);
		int epTotal = (episodeList != null) ? episodeList.size() : 0;
		tvEpSummary.setText("Dongflix • " + epTotal + " Episode");
		tvEpSummary.setTextColor(Color.parseColor("#E5E7EB"));
		tvEpSummary.setTextSize(13f);
		tvEpSummary.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		studioRow.addView(tvEpSummary);

		rightCol.addView(studioRow);
		infoHeaderRow.addView(rightCol);
		content.addView(infoHeaderRow);

		TextView tvSinopsisBody = new TextView(this);
		tvSinopsisBody.setText(detailSynopsis.isEmpty() ? "Sinopsis belum tersedia untuk donghua ini." : detailSynopsis);
		tvSinopsisBody.setTextColor(Color.parseColor("#9CA3AF"));
		tvSinopsisBody.setTextSize(13.5f);
		tvSinopsisBody.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvSinopsisBody.setLineSpacing(5 * d, 1f);
		content.addView(tvSinopsisBody);

		root.addView(scrollView);
		dialogHolder[0] = createAdaptiveSheetDialog(root, true);
	}

	private void showCleanOptionBottomSheet(String title, ArrayList<String> optionTitles, int activeIdx, final OnSheetItemClickListener listener) {
		final Dialog[] dialogHolder = new Dialog[1];
		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int)(20 * d), (int)(12 * d), (int)(20 * d), (int)(24 * d));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0B0D11"));
		sheetBg.setCornerRadii(new float[]{22 * d, 22 * d, 22 * d, 22 * d, 0, 0, 0, 0});
		root.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int)(38 * d), (int)(4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int)(12 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#1E222B"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		root.addView(buildAdaptiveHeader(title, dialogHolder));

		ScrollView scrollView = new ScrollView(this);
		scrollView.setVerticalScrollBarEnabled(false);
		LinearLayout listContainer = new LinearLayout(this);
		listContainer.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(listContainer);

		for (int i = 0; i < optionTitles.size(); i++) {
			final int index = i;
			boolean isSelected = (i == activeIdx);

			LinearLayout itemRow = new LinearLayout(this);
			itemRow.setOrientation(LinearLayout.HORIZONTAL);
			itemRow.setGravity(Gravity.CENTER_VERTICAL);
			itemRow.setPadding((int)(16 * d), (int)(14 * d), (int)(16 * d), (int)(14 * d));

			LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			);
			rowLp.bottomMargin = (int)(4 * d);
			itemRow.setLayoutParams(rowLp);

			if (isSelected) {
				GradientDrawable selectedBg = new GradientDrawable();
				selectedBg.setCornerRadius(12 * d);
				selectedBg.setColor(Color.parseColor("#1E3A8A"));
				itemRow.setBackground(selectedBg);
			} else {
				itemRow.setBackgroundColor(Color.TRANSPARENT);
			}

			TextView tvName = new TextView(this);
			tvName.setText(optionTitles.get(i));
			tvName.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#E5E7EB"));
			tvName.setTextSize(15f);
			tvName.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
			tvName.setLayoutParams(nameLp);
			itemRow.addView(tvName);

			if (isSelected) {
				TextView checkCircle = new TextView(this);
				LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams((int)(20 * d), (int)(20 * d));
				checkCircle.setLayoutParams(checkLp);
				checkCircle.setGravity(Gravity.CENTER);
				checkCircle.setText("✓");
				checkCircle.setTextColor(Color.parseColor("#0B0D11"));
				checkCircle.setTextSize(11f);
				checkCircle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

				GradientDrawable circleBg = new GradientDrawable();
				circleBg.setShape(GradientDrawable.OVAL);
				circleBg.setColor(Color.parseColor("#38BDF8"));
				checkCircle.setBackground(circleBg);

				itemRow.addView(checkCircle);
			}

			itemRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (dialogHolder[0] != null) dialogHolder[0].dismiss();
					listener.onItemClick(index);
				}
			});

			listContainer.addView(itemRow);
		}

		root.addView(scrollView);
		dialogHolder[0] = createAdaptiveSheetDialog(root);
	}

	public void showAllEpisodesBottomSheet() {
		if (episodeList == null || episodeList.isEmpty()) {
			Toast.makeText(WatchActivity.this, "Daftar episode belum dimuat", Toast.LENGTH_SHORT).show();
			return;
		}

		final Dialog[] dialogHolder = new Dialog[1];
		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int)(20 * d), (int)(12 * d), (int)(20 * d), (int)(24 * d));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0B0D11"));
		sheetBg.setCornerRadii(new float[]{22 * d, 22 * d, 22 * d, 22 * d, 0, 0, 0, 0});
		root.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int)(38 * d), (int)(4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int)(12 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#1E222B"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		root.addView(buildAdaptiveHeader("Semua Episode (" + episodeList.size() + ")", dialogHolder));

		ScrollView scrollView = new ScrollView(this);
		scrollView.setVerticalScrollBarEnabled(false);

		LinearLayout gridContainer = new LinearLayout(this);
		gridContainer.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(gridContainer);

		int columns = isLandscapeOrFullscreen() ? 4 : 5;
		int total = episodeList.size();
		int rows = (int) Math.ceil((double) total / columns);

		for (int r = 0; r < rows; r++) {
			LinearLayout rowLayout = new LinearLayout(this);
			rowLayout.setOrientation(LinearLayout.HORIZONTAL);
			LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			);
			rowLp.bottomMargin = (int)(10 * d);
			rowLayout.setLayoutParams(rowLp);

			for (int c = 0; c < columns; c++) {
				final int epIdx = (r * columns) + c;
				LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, (int)(46 * d), 1f);
				if (c < columns - 1) cellLp.rightMargin = (int)(8 * d);

				if (epIdx < total) {
					boolean isActive = (epIdx == currentEpisodeIndex);
					HashMap<String, Object> itemData = episodeList.get(epIdx);

					String epLabel = String.valueOf(epIdx + 1);
					if (itemData.containsKey("episode") && itemData.get("episode") != null) {
						epLabel = itemData.get("episode").toString().replace("Ep ", "").replace("Eps ", "").trim();
					} else if (itemData.containsKey("title") && itemData.get("title") != null) {
						String parsedNum = itemData.get("title").toString().replaceAll("[^0-9]", "").trim();
						if (!parsedNum.isEmpty()) epLabel = parsedNum;
					}

					TextView cellBtn = new TextView(this);
					cellBtn.setLayoutParams(cellLp);
					cellBtn.setGravity(Gravity.CENTER);
					cellBtn.setText(epLabel);
					cellBtn.setTextSize(14f);
					cellBtn.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
					cellBtn.setTextColor(isActive ? Color.WHITE : Color.parseColor("#CBD5E1"));

					GradientDrawable cellBg = new GradientDrawable();
					cellBg.setCornerRadius(10 * d);
					cellBg.setColor(isActive ? Color.parseColor("#3B82F6") : Color.parseColor("#12141A"));
					cellBtn.setBackground(cellBg);

					cellBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (dialogHolder[0] != null) dialogHolder[0].dismiss();
							if (currentEpisodeIndex != epIdx || mediaPlayer == null || !mediaPlayer.isPlaying()) {
								playEpisodeByIndex(epIdx, 0);
							}
						}
					});

					rowLayout.addView(cellBtn);
				} else {
					View spacer = new View(this);
					spacer.setLayoutParams(cellLp);
					rowLayout.addView(spacer);
				}
			}
			gridContainer.addView(rowLayout);
		}

		root.addView(scrollView);
		dialogHolder[0] = createAdaptiveSheetDialog(root);
	}

	public void showServerDialog() {
		if (currentEpisodeServerNames == null || currentEpisodeServerNames.isEmpty()) {
			Toast.makeText(WatchActivity.this, "Daftar server belum siap", Toast.LENGTH_SHORT).show();
			return;
		}

		showCleanOptionBottomSheet(
			"Server",
			currentEpisodeServerNames,
			selectedServerIndex,
			new OnSheetItemClickListener() {
				@Override
				public void onItemClick(int which) {
					selectedServerIndex = which;
					String chosenName = currentEpisodeServerNames.get(which);
					String chosenUrl = currentEpisodeServerUrls.get(which);

					if (textActionServer != null) {
						textActionServer.setText(chosenName);
					}
					int curPos = 0;
					if (mediaPlayer != null) {
						try { curPos = mediaPlayer.getCurrentPosition(); } catch (Exception ignored) {}
					}
					extractStream(chosenName, chosenUrl, curPos);
				}
			}
		);
	}

	public void showQualityDialog() {
		if (qualityNames == null || qualityNames.isEmpty()) {
			Toast.makeText(WatchActivity.this, "Pilihan resolusi belum siap", Toast.LENGTH_SHORT).show();
			return;
		}

		ArrayList<String> titles = new ArrayList<>();
		String currentActiveRes = qualityNames.get(selectedQualityIndex);

		if (currentActiveRes.equalsIgnoreCase("Auto") || currentActiveRes.equalsIgnoreCase("Default")) {
			titles.add("Auto");
		} else {
			titles.add("Auto (" + currentActiveRes.toLowerCase() + ")");
		}

		for (String q : qualityNames) {
			if (!q.equalsIgnoreCase("Auto")) {
				titles.add(q.toLowerCase());
			}
		}

		int activeSheetIdx = 0;
		if (!isAutoQualityMode) {
			String chosenLabel = qualityNames.get(selectedQualityIndex).toLowerCase();
			for (int i = 1; i < titles.size(); i++) {
				if (titles.get(i).equalsIgnoreCase(chosenLabel)) {
					activeSheetIdx = i;
					break;
				}
			}
		}

		final ArrayList<String> finalTitles = titles;

		showCleanOptionBottomSheet(
			"Kualitas",
			finalTitles,
			activeSheetIdx,
			new OnSheetItemClickListener() {
				@Override
				public void onItemClick(int which) {
					int currentPos = 0;
					if (mediaPlayer != null) {
						try {
							currentPos = mediaPlayer.getCurrentPosition();
						} catch (Exception ignored) {}
					}

					if (which == 0) {
						isAutoQualityMode = true;
						int recIdx = getRecommendedQualityIndex();
						boolean needReload = (recIdx != selectedQualityIndex);
						selectedQualityIndex = recIdx;
						updateQualityBadgeText();

						if (needReload) {
							playVideoUrl(qualityUrls.get(recIdx), currentPos);
						}
					} else {
						isAutoQualityMode = false;
						String targetRes = finalTitles.get(which);
						int realIdx = 0;
						for (int k = 0; k < qualityNames.size(); k++) {
							if (qualityNames.get(k).equalsIgnoreCase(targetRes)) {
								realIdx = k;
								break;
							}
						}
						selectedQualityIndex = realIdx;
						updateQualityBadgeText();
						playVideoUrl(qualityUrls.get(realIdx), currentPos);
					}
				}
			}
		);
	}

	public void showSpeedDialog() {
		final ArrayList<String> speedLabels = new ArrayList<>();
		speedLabels.add("0.5x");
		speedLabels.add("0.75x");
		speedLabels.add("Normal (1x)");
		speedLabels.add("1.25x");
		speedLabels.add("1.5x");
		speedLabels.add("1.75x");
		speedLabels.add("2x");

		final float[] speedVals = new float[]{0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
		int activeIdx = 2;
		for (int i = 0; i < speedVals.length; i++) {
			if (Math.abs(speedVals[i] - currentPlaybackSpeed) < 0.01f) {
				activeIdx = i;
				break;
			}
		}

		showCleanOptionBottomSheet(
			"Kecepatan Putar",
			speedLabels,
			activeIdx,
			new OnSheetItemClickListener() {
				@Override
				public void onItemClick(int which) {
					float selectedSpeed = speedVals[which];
					currentPlaybackSpeed = selectedSpeed;
					applyPlaybackSpeedValue(selectedSpeed);
				}
			}
		);
	}

	private void minimizeToMiniPlayer() {
		if (mediaPlayer != null) {
			try {
				saveWatchHistoryToFirebase(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration(), true);
			} catch (Exception ignored) {}
		}

		if (mediaPlayer != null && !currentPlayingUrl.isEmpty()) {
			DongflixPlayerManager mgr = DongflixPlayerManager.getInstance();
			mgr.sharedMediaPlayer = mediaPlayer;
			mgr.isMiniPlayerActive = true;
			mgr.activeSlug = currentSlug;
			mgr.activeTitle = detailTitle.isEmpty() ? (textJudul != null ? textJudul.getText().toString() : "Dongflix") : detailTitle;
			mgr.activeCoverUrl = detailCover;
			mgr.activeStreamUrl = currentPlayingUrl;
			mgr.activeEpisodeIndex = currentEpisodeIndex;
			mgr.activeEpisodeList = new ArrayList<>(episodeList);

			mgr.activeServerName = (textActionServer != null) ? textActionServer.getText().toString() : "";
			mgr.activeSelectedServerIndex = selectedServerIndex;
			mgr.activeServerNames = new ArrayList<>(currentEpisodeServerNames);
			mgr.activeServerUrls = new ArrayList<>(currentEpisodeServerUrls);
			mgr.activeQualityNames = new ArrayList<>(qualityNames);
			mgr.activeQualityUrls = new ArrayList<>(qualityUrls);
			mgr.activeSelectedQualityIndex = selectedQualityIndex;
			mgr.activeIsAutoQuality = isAutoQualityMode;

			if (episodeList != null && currentEpisodeIndex >= 0 && currentEpisodeIndex < episodeList.size()) {
				Object epObj = episodeList.get(currentEpisodeIndex).get("episode");
				mgr.activeEpisodeLabel = epObj != null ? epObj.toString().replace("Ep ", "").replace("Eps ", "").trim() : String.valueOf(currentEpisodeIndex + 1);
			}

			mgr.updatePlaybackNotification(this);
			mediaPlayer = null;
		}
		finish();
		overridePendingTransition(R.anim.no_anim, R.anim.slide_down_exit);
	}

	@Override
	public void onDestroy() {
		detachCommentsListener();
		if (shimmerAnimator != null) {
			shimmerAnimator.cancel();
			shimmerAnimator = null;
		}
		if (playerHandler != null) {
			if (progressLoop != null) playerHandler.removeCallbacks(progressLoop);
			if (hideControlsRunnable != null) playerHandler.removeCallbacks(hideControlsRunnable);
			if (autoDowngradeRunnable != null) playerHandler.removeCallbacks(autoDowngradeRunnable);
			if (hideUnlockBtnRunnable != null) playerHandler.removeCallbacks(hideUnlockBtnRunnable);
			playerHandler.removeCallbacksAndMessages(null);
		}

		if (mediaPlayer != null && !DongflixPlayerManager.getInstance().isMiniPlayerActive) {
			try {
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.stop();
				}
				mediaPlayer.reset();
				mediaPlayer.release();
			} catch (Exception ignored) {}
			mediaPlayer = null;
		}

		if (fallbackWebView != null) {
			try {
				fallbackWebView.stopLoading();
				fallbackWebView.destroy();
			} catch (Exception ignored) {}
			fallbackWebView = null;
		}

		if (playerSurface != null) {
			playerSurface.release();
			playerSurface = null;
		}

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		super.onDestroy();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopHold2xSpeed();
		if (mediaPlayer != null) {
			try {
				saveWatchHistoryToFirebase(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration(), true);
			} catch (Exception ignored) {}
			if (mediaPlayer.isPlaying() && !DongflixPlayerManager.getInstance().isMiniPlayerActive) {
				mediaPlayer.pause();
				if (btnPlay != null) {
					btnPlay.setImageResource(R.drawable.ic_play1);
				}
			}
		}
		if (fallbackWebView != null && isWebViewFallbackActive) {
			fallbackWebView.onPause();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (fallbackWebView != null && isWebViewFallbackActive) {
			fallbackWebView.onResume();
		}
	}

	@Override
	public void onBackPressed() {
		if (isScreenLocked) {
			showUnlockPillTemporarily();
			return;
		}
		if (isFullscreen) {
			toggleFullscreen();
		} else if (!isFinishing()) {
			minimizeToMiniPlayer();
		}
	}

	public int getRecommendedQualityIndex() {
		if (qualityNames == null || qualityNames.isEmpty()) return 0;

		int targetHeight = 480;
		try {
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
					if (caps != null) {
						if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
							int downMbps = caps.getLinkDownstreamBandwidthKbps() / 1000;
							if (downMbps >= 15) targetHeight = 1080;
							else if (downMbps >= 5) targetHeight = 720;
							else targetHeight = 480;
						} else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
							int downMbps = caps.getLinkDownstreamBandwidthKbps() / 1000;
							if (downMbps >= 10) targetHeight = 720;
							else if (downMbps >= 2) targetHeight = 480;
							else targetHeight = 360;
						}
					}
				} else {
					NetworkInfo info = cm.getActiveNetworkInfo();
					if (info != null && info.isConnected()) {
						if (info.getType() == ConnectivityManager.TYPE_WIFI) targetHeight = 720;
						else targetHeight = 480;
					}
				}
			}
		} catch (Exception ignored) {}

		for (int i = 0; i < qualityNames.size(); i++) {
			if (qualityNames.get(i).equalsIgnoreCase("Auto")) return i;
		}

		int bestIdx = 0;
		int smallestDiff = Integer.MAX_VALUE;
		for (int i = 0; i < qualityNames.size(); i++) {
			String numStr = qualityNames.get(i).replaceAll("[^0-9]", "");
			if (!numStr.isEmpty()) {
				int resVal = Integer.parseInt(numStr);
				int diff = Math.abs(resVal - targetHeight);
				if (diff < smallestDiff) {
					smallestDiff = diff;
					bestIdx = i;
				}
			}
		}
		return bestIdx;
	}

	public void triggerAutoDowngrade() {
		if (!isAutoQualityMode || qualityNames == null || qualityNames.size() <= 1) return;

		int currentRes = 9999;
		String curNum = qualityNames.get(selectedQualityIndex).replaceAll("[^0-9]", "");
		if (!curNum.isEmpty()) currentRes = Integer.parseInt(curNum);

		int lowerIdx = -1;
		int closestLowerRes = -1;

		for (int i = 0; i < qualityNames.size(); i++) {
			if (i == selectedQualityIndex) continue;
			String numStr = qualityNames.get(i).replaceAll("[^0-9]", "");
			if (!numStr.isEmpty()) {
				int resVal = Integer.parseInt(numStr);
				if (resVal < currentRes && resVal > closestLowerRes) {
					closestLowerRes = resVal;
					lowerIdx = i;
				}
			}
		}

		if (lowerIdx != -1) {
			selectedQualityIndex = lowerIdx;
			int currentPos = 0;
			if (mediaPlayer != null) {
				try {
					currentPos = mediaPlayer.getCurrentPosition();
				} catch (Exception ignored) {}
			}
			updateQualityBadgeText();
			Toast.makeText(WatchActivity.this, "Jaringan lambat, otomatis ke " + qualityNames.get(lowerIdx), Toast.LENGTH_SHORT).show();
			playVideoUrl(qualityUrls.get(lowerIdx), currentPos);
		}
	}

	public void updateQualityBadgeText() {
		if (btnQuality == null || qualityNames.isEmpty()) return;
		if (isAutoQualityMode) {
			String activeRes = qualityNames.get(selectedQualityIndex);
			if (activeRes.equalsIgnoreCase("Auto") || activeRes.equalsIgnoreCase("Default")) {
				btnQuality.setText("Auto");
			} else {
				btnQuality.setText("Auto (" + activeRes + ")");
			}
		} else {
			btnQuality.setText(qualityNames.get(selectedQualityIndex));
		}
	}

	public void resetControlsTimer() {
		if (playerHandler != null && hideControlsRunnable != null) {
			playerHandler.removeCallbacks(hideControlsRunnable);
			playerHandler.postDelayed(hideControlsRunnable, 3500);
		}
	}

	public void showControls() {
		if (isScreenLocked) {
			showUnlockPillTemporarily();
			return;
		}
		if (playerControlsOverlay != null) {
			playerControlsOverlay.setVisibility(View.VISIBLE);
			playerControlsOverlay.bringToFront();
		}
		areControlsVisible = true;
		updateBottomDividerState();
		resetControlsTimer();
	}

	public void hideControls() {
		if (playerControlsOverlay != null) {
			playerControlsOverlay.setVisibility(View.GONE);
		}
		if (btnUnlockScreen != null) {
			btnUnlockScreen.setVisibility(View.GONE);
		}
		areControlsVisible = false;
		updateBottomDividerState();
		if (playerHandler != null && hideControlsRunnable != null) {
			playerHandler.removeCallbacks(hideControlsRunnable);
		}
	}

	public void toggleControls() {
		if (isScreenLocked) {
			if (btnUnlockScreen != null && btnUnlockScreen.getVisibility() == View.VISIBLE) {
				btnUnlockScreen.setVisibility(View.GONE);
			} else {
				showUnlockPillTemporarily();
			}
			return;
		}
		if (areControlsVisible) {
			hideControls();
		} else {
			showControls();
		}
	}

	public void updateEpisodeNavButtons() {
		int totalItems = (recyclerEpisode != null && recyclerEpisode.getAdapter() != null) ? recyclerEpisode.getAdapter().getItemCount() : (episodeList != null ? episodeList.size() : 0);

		if (btnPrevEpisode != null) {
			btnPrevEpisode.setEnabled(currentEpisodeIndex > 0);
			btnPrevEpisode.setAlpha(currentEpisodeIndex > 0 ? 1.0f : 0.35f);
		}

		if (btnNextEpisode != null) {
			boolean hasNext = currentEpisodeIndex < (totalItems - 1) && totalItems > 1;
			btnNextEpisode.setEnabled(hasNext);
			btnNextEpisode.setAlpha(hasNext ? 1.0f : 0.35f);
		}
	}

	public void playEpisodeByIndex(final int targetIndex) {
		playEpisodeByIndex(targetIndex, 0);
	}

	public void playEpisodeByIndex(final int targetIndex, final int initialSeekMs) {
		if (episodeList == null || episodeList.isEmpty()) return;
		if (targetIndex < 0 || targetIndex >= episodeList.size()) return;

		currentEpisodeIndex = targetIndex;
		savedSeekPosition = Math.max(0, initialSeekMs);
		updateEpisodeNavButtons();

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (recyclerEpisode != null) {
					recyclerEpisode.scrollToPosition(targetIndex);
					if (recyclerEpisode.getAdapter() != null) {
						recyclerEpisode.getAdapter().notifyDataSetChanged();
					}
				}

				if (playerLoading != null) playerLoading.setVisibility(View.VISIBLE);
				if (btnPlay != null) {
					btnPlay.setEnabled(false);
					btnPlay.setAlpha(0.5f);
				}

				HashMap<String, Object> item = episodeList.get(targetIndex);
				if (!item.containsKey("servers")) {
					if (playerLoading != null) playerLoading.setVisibility(View.GONE);
					if (btnPlay != null) {
						btnPlay.setEnabled(true);
						btnPlay.setAlpha(1.0f);
					}
					return;
				}

				currentEpisodeServerNames.clear();
				currentEpisodeServerUrls.clear();

				try {
					Object rawServers = item.get("servers");
					JSONArray sArray;
					if (rawServers instanceof JSONArray) {
						sArray = (JSONArray) rawServers;
					} else if (rawServers instanceof ArrayList) {
						sArray = new JSONArray((ArrayList<?>) rawServers);
					} else {
						sArray = new JSONArray(rawServers.toString());
					}

					for (int i = 0; i < sArray.length(); i++) {
						JSONObject sObj;
						Object itemObj = sArray.get(i);
						if (itemObj instanceof HashMap) {
							sObj = new JSONObject((HashMap<?, ?>) itemObj);
						} else {
							sObj = sArray.getJSONObject(i);
						}

						String sName = sObj.optString("name", "Server " + (i + 1));
						String sLink = sObj.optString("link");
						if (!sLink.isEmpty()) {
							currentEpisodeServerNames.add(sName);
							currentEpisodeServerUrls.add(sLink);
						}
					}
				} catch (Exception ignored) {
					if (playerLoading != null) playerLoading.setVisibility(View.GONE);
					if (btnPlay != null) {
						btnPlay.setEnabled(true);
						btnPlay.setAlpha(1.0f);
					}
					return;
				}

				if (currentEpisodeServerUrls.isEmpty()) {
					if (playerLoading != null) playerLoading.setVisibility(View.GONE);
					if (btnPlay != null) {
						btnPlay.setEnabled(true);
						btnPlay.setAlpha(1.0f);
					}
					return;
				}

				int targetServerIdx = 0;
				for (int i = 0; i < currentEpisodeServerNames.size(); i++) {
					String name = currentEpisodeServerNames.get(i).toLowerCase();
					if (name.contains("ok") || name.contains("odnoklassniki")) {
						targetServerIdx = i;
						break;
					}
				}

				selectedServerIndex = targetServerIdx;
				String chosenServerName = currentEpisodeServerNames.get(targetServerIdx);
				String chosenServerUrl = currentEpisodeServerUrls.get(targetServerIdx);

				if (textActionServer != null) {
					textActionServer.setText(chosenServerName);
				}

				saveWatchHistoryToFirebase(savedSeekPosition, 0, true);
				extractStream(chosenServerName, chosenServerUrl, savedSeekPosition);
			}
		});
	}

	private void hideFallbackWebView() {
		isWebViewFallbackActive = false;
		if (fallbackWebView != null) {
			try {
				fallbackWebView.stopLoading();
				fallbackWebView.loadUrl("about:blank");
				fallbackWebView.setVisibility(View.GONE);
			} catch (Exception ignored) {}
		}
		if (playerTextureView != null) {
			playerTextureView.setVisibility(View.VISIBLE);
		}
	}

	private void playViaWebViewInterceptor(final String embedUrl) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					if (mediaPlayer != null) {
						try { mediaPlayer.stop(); mediaPlayer.reset(); mediaPlayer.release(); } catch (Exception ignored) {}
						mediaPlayer = null;
					}

					if (fallbackWebView == null && layoutPlayer != null) {
						fallbackWebView = new WebView(WatchActivity.this);
						FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.MATCH_PARENT
						);
						fallbackWebView.setLayoutParams(lp);
						fallbackWebView.setBackgroundColor(Color.BLACK);
						layoutPlayer.addView(fallbackWebView, 0);
					}

					if (fallbackWebView != null) {
						WebSettings ws = fallbackWebView.getSettings();
						ws.setJavaScriptEnabled(true);
						ws.setDomStorageEnabled(true);
						ws.setMediaPlaybackRequiresUserGesture(false);
						ws.setAllowFileAccess(true);
						ws.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

						fallbackWebView.setWebChromeClient(new WebChromeClient());
						fallbackWebView.setWebViewClient(new WebViewClient() {
							@Override
							public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null && request.getUrl() != null) {
									final String reqUrl = request.getUrl().toString();
									String low = reqUrl.toLowerCase();
									if ((low.contains(".m3u8") || low.contains(".mp4")) && !low.contains("ads") && !low.contains("analytics") && !low.contains("tracker")) {
										runOnUiThread(new Runnable() {
											@Override
											public void run() {
												if (isWebViewFallbackActive) {
													hideFallbackWebView();
													qualityNames.clear();
													qualityUrls.clear();
													qualityNames.add("Auto");
													qualityUrls.add(reqUrl);
													isAutoQualityMode = true;
													selectedQualityIndex = 0;
													updateQualityBadgeText();
													playVideoUrl(reqUrl, savedSeekPosition);
												}
											}
										});
									}
								}
								return super.shouldInterceptRequest(view, request);
							}

							@Override
							public void onPageFinished(WebView view, String url) {
								if (playerLoading != null) playerLoading.setVisibility(View.GONE);
								if (btnPlay != null) {
									btnPlay.setEnabled(true);
									btnPlay.setAlpha(1.0f);
								}
							}
						});

						isWebViewFallbackActive = true;
						if (playerTextureView != null) playerTextureView.setVisibility(View.GONE);
						fallbackWebView.setVisibility(View.VISIBLE);
						if (playerControlsOverlay != null) playerControlsOverlay.bringToFront();

						Map<String, String> extraHeaders = new HashMap<>();
						extraHeaders.put("Referer", "https://anichin.cafe/");
						fallbackWebView.loadUrl(embedUrl, extraHeaders);
					}
				} catch (Exception e) {
					if (playerLoading != null) playerLoading.setVisibility(View.GONE);
				}
			}
		});
	}

	public void playVideoUrl(final String rawUrl, final int targetSeek) {
		if (rawUrl == null || rawUrl.trim().isEmpty()) return;

		hideFallbackWebView();

		String normalized = rawUrl.trim();
		if (normalized.startsWith("Https://")) {
			normalized = "https://" + normalized.substring(8);
		} else if (normalized.startsWith("Http://")) {
			normalized = "http://" + normalized.substring(7);
		}
		final String cleanUrl = normalized;
		currentPlayingUrl = cleanUrl;
		savedSeekPosition = targetSeek;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (playerLoading != null) playerLoading.setVisibility(View.VISIBLE);
				if (btnPlay != null) {
					btnPlay.setEnabled(false);
					btnPlay.setAlpha(0.5f);
				}

				if (playerHandler != null && autoDowngradeRunnable != null) {
					playerHandler.removeCallbacks(autoDowngradeRunnable);
					if (isAutoQualityMode) {
						playerHandler.postDelayed(autoDowngradeRunnable, 5000);
					}
				}

				try {
					if (mediaPlayer != null) {
						try { mediaPlayer.stop(); } catch (Exception ignored) {}
						try {
							mediaPlayer.reset();
							mediaPlayer.release();
						} catch (Exception ignored) {}
						mediaPlayer = null;
					}

					mediaPlayer = new MediaPlayer();

					if (playerTextureView != null && playerTextureView.isAvailable()) {
						SurfaceTexture st = playerTextureView.getSurfaceTexture();
						if (st != null) {
							if (playerSurface != null) playerSurface.release();
							playerSurface = new Surface(st);
							mediaPlayer.setSurface(playerSurface);
						}
					} else if (playerSurface != null && playerSurface.isValid()) {
						mediaPlayer.setSurface(playerSurface);
					}

					Map<String, String> headers = new HashMap<>();
					headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

					if (cleanUrl.contains("ok.ru") || cleanUrl.contains("odnoklassniki")) {
						headers.put("Referer", "https://ok.ru/");
					} else if (cleanUrl.contains("dailymotion") || cleanUrl.contains("dmcdn")) {
						headers.put("Referer", "https://www.dailymotion.com/");
						headers.put("Origin", "https://www.dailymotion.com");
						if (dmSessionCookie != null && !dmSessionCookie.isEmpty()) {
							headers.put("Cookie", dmSessionCookie);
						}
					} else if (cleanUrl.contains("rumble")) {
						headers.put("Referer", "https://rumble.com/");
					} else if (cleanUrl.contains("odysee") || cleanUrl.contains("odycdn")) {
						headers.put("Referer", "https://odysee.com/");
					} else if (currentStreamReferer != null && !currentStreamReferer.isEmpty()) {
						headers.put("Referer", currentStreamReferer);
					}

					headers.put("Accept", "*/*");

					mediaPlayer.setDataSource(WatchActivity.this, Uri.parse(cleanUrl), headers);
					mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

					mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
						@Override
						public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
							if (width > 0 && height > 0) {
								currentVideoWidth = width;
								currentVideoHeight = height;
								adjustVideoAspectRatio();
							}
						}
					});

					mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
						@Override
						public void onBufferingUpdate(MediaPlayer mp, int percent) {
							try {
								int duration = mp.getDuration();
								if (duration > 0 && playerSeekBar != null) {
									int cacheMillis = (int) ((duration * (long) percent) / 100);
									playerSeekBar.setSecondaryProgress(cacheMillis);
									playerSeekBar.invalidate();
								}
								if (miniVideoProgressBar != null) {
									miniVideoProgressBar.setSecondaryProgress(percent * 10);
									miniVideoProgressBar.invalidate();
								}
							} catch (Exception ignored) {}
						}
					});

					mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
						@Override
						public boolean onInfo(MediaPlayer mp, int what, int extra) {
							if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
								if (playerLoading != null) playerLoading.setVisibility(View.VISIBLE);
								if (btnPlay != null) {
									btnPlay.setEnabled(false);
									btnPlay.setAlpha(0.5f);
								}
								if (playerHandler != null && autoDowngradeRunnable != null && isAutoQualityMode) {
									playerHandler.removeCallbacks(autoDowngradeRunnable);
									playerHandler.postDelayed(autoDowngradeRunnable, 2500);
								}
							} else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END || what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
								if (playerHandler != null && autoDowngradeRunnable != null) {
									playerHandler.removeCallbacks(autoDowngradeRunnable);
								}
								if (playerLoading != null) playerLoading.setVisibility(View.GONE);
								if (btnPlay != null) {
									btnPlay.setEnabled(true);
									btnPlay.setAlpha(1.0f);
								}
							}
							return false;
						}
					});

					mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
						@Override
						public void onPrepared(MediaPlayer mp) {
							if (playerHandler != null && autoDowngradeRunnable != null) {
								playerHandler.removeCallbacks(autoDowngradeRunnable);
							}
							if (playerLoading != null) playerLoading.setVisibility(View.GONE);
							if (btnPlay != null) {
								btnPlay.setEnabled(true);
								btnPlay.setAlpha(1.0f);
								btnPlay.setImageResource(R.drawable.ic_pause);
							}

							if (playerTextureView != null && playerTextureView.isAvailable() && playerSurface == null) {
								playerSurface = new Surface(playerTextureView.getSurfaceTexture());
								mp.setSurface(playerSurface);
							}

							int vw = mp.getVideoWidth();
							int vh = mp.getVideoHeight();
							if (vw > 0 && vh > 0) {
								currentVideoWidth = vw;
								currentVideoHeight = vh;
								adjustVideoAspectRatio();
							}

							int duration = mp.getDuration();
							if (playerSeekBar != null) playerSeekBar.setMax(duration);

							if (savedSeekPosition > 0 && savedSeekPosition < duration) {
								mp.seekTo(savedSeekPosition);
								savedSeekPosition = 0;
							}
							mp.start();

							if (!hasRewardedWatchExp) {
								hasRewardedWatchExp = true;
								addExpToCurrentUser(100L);
							}

							float targetSpeed = isHolding2xSpeed ? 2.0f : currentPlaybackSpeed;
							if (targetSpeed != 1.0f) {
								applyPlaybackSpeedValue(targetSpeed);
							}

							if (playerHandler != null && progressLoop != null) {
								playerHandler.removeCallbacks(progressLoop);
								playerHandler.post(progressLoop);
							}
							updateEpisodeNavButtons();
							resetControlsTimer();
						}
					});

					mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
						@Override
						public boolean onError(MediaPlayer mp, int what, int extra) {
							if (playerHandler != null && autoDowngradeRunnable != null) {
								playerHandler.removeCallbacks(autoDowngradeRunnable);
							}
							if (cleanUrl.contains("ipfs.io/ipfs/")) {
								String altIpfs = cleanUrl.replace("https://ipfs.io/ipfs/", "https://dweb.link/ipfs/");
								playVideoUrl(altIpfs, savedSeekPosition);
								return true;
							}
							if (playerLoading != null) playerLoading.setVisibility(View.GONE);
							if (btnPlay != null) {
								btnPlay.setEnabled(true);
								btnPlay.setAlpha(1.0f);
								btnPlay.setImageResource(R.drawable.ic_play1);
							}
							Toast.makeText(WatchActivity.this, "Gagal memutar stream, coba server lain", Toast.LENGTH_SHORT).show();
							return true;
						}
					});

					mediaPlayer.prepareAsync();

				} catch (Exception e) {
					if (playerLoading != null) playerLoading.setVisibility(View.GONE);
					if (btnPlay != null) {
						btnPlay.setEnabled(true);
						btnPlay.setAlpha(1.0f);
					}
				}
			}
		});
	}

	public void toggleFullscreen() {
		final Window window = getWindow();
		final View decorView = window.getDecorView();
		final View rootWatch = findViewById(R.id.rootWatchLayout);
		final View scrollContent = findViewById(R.id.vscroll16);
		final FrameLayout playContainer = findViewById(R.id.frame_layout5);
		final float density = getResources().getDisplayMetrics().density;

		if (!isFullscreen) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

			if (rootWatch != null) {
				rootWatch.setPadding(0, 0, 0, 0);
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				WindowManager.LayoutParams lp = window.getAttributes();
				lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
				window.setAttributes(lp);
			}

			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

			decorView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			);

			if (scrollContent != null) scrollContent.setVisibility(View.GONE);

			if (layoutPlayer != null) {
				ViewGroup.LayoutParams lp = layoutPlayer.getLayoutParams();
				lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
				layoutPlayer.setLayoutParams(lp);
			}

			if (textPlayerTitle != null && textJudul != null) {
				textPlayerTitle.setText(textJudul.getText().toString());
				textPlayerTitle.setTextSize(16f);
				textPlayerTitle.setVisibility(View.VISIBLE);
			}

			if (btnLock != null) {
				btnLock.setVisibility(View.VISIBLE);
			}

			if (btnFullscreenEpisodes != null) {
				btnFullscreenEpisodes.setBackgroundColor(Color.TRANSPARENT);
				btnFullscreenEpisodes.setVisibility(View.VISIBLE);
			}

			int sideMarginFull = (int) (28 * density);
			if (btnPrevEpisode != null) {
				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) btnPrevEpisode.getLayoutParams();
				lp.width = (int) (38 * density);
				lp.height = (int) (38 * density);
				lp.setMargins(sideMarginFull, 0, 0, 0);
				btnPrevEpisode.setLayoutParams(lp);
			}

			if (btnNextEpisode != null) {
				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) btnNextEpisode.getLayoutParams();
				lp.width = (int) (38 * density);
				lp.height = (int) (38 * density);
				lp.setMargins(0, 0, sideMarginFull, 0);
				btnNextEpisode.setLayoutParams(lp);
			}

			int gapCenterFull = (int) (36 * density);
			if (btnRewind != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnRewind.getLayoutParams();
				lp.width = (int) (42 * density);
				lp.height = (int) (42 * density);
				btnRewind.setLayoutParams(lp);
			}

			if (playContainer != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) playContainer.getLayoutParams();
				lp.width = (int) (74 * density);
				lp.height = (int) (74 * density);
				lp.setMargins(gapCenterFull, 0, gapCenterFull, 0);
				playContainer.setLayoutParams(lp);
			}

			if (playerLoading != null) {
				ViewGroup.LayoutParams lp = playerLoading.getLayoutParams();
				lp.width = (int) (74 * density);
				lp.height = (int) (74 * density);
				playerLoading.setLayoutParams(lp);
			}

			if (btnPlay != null) {
				ViewGroup.LayoutParams lp = btnPlay.getLayoutParams();
				lp.width = (int) (60 * density);
				lp.height = (int) (60 * density);
				btnPlay.setLayoutParams(lp);
				int padPlay = (int) (15 * density);
				btnPlay.setPadding(padPlay, padPlay, padPlay, padPlay);
			}

			if (btnForward != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnForward.getLayoutParams();
				lp.width = (int) (42 * density);
				lp.height = (int) (42 * density);
				btnForward.setLayoutParams(lp);
			}

			if (btnBack != null) {
				ViewGroup.LayoutParams lp = btnBack.getLayoutParams();
				lp.width = (int) (28 * density);
				lp.height = (int) (28 * density);
				btnBack.setLayoutParams(lp);
			}
			if (btnFullscreen != null) {
				btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit);
				ViewGroup.LayoutParams lp = btnFullscreen.getLayoutParams();
				lp.width = (int) (26 * density);
				lp.height = (int) (26 * density);
				btnFullscreen.setLayoutParams(lp);
			}
			if (btnQuality != null) btnQuality.setTextSize(14f);
			if (textTime != null) textTime.setTextSize(13.5f);

			int padHoriz = (int) (28 * density);
			int padTop = (int) (18 * density);
			int padBottom = (int) (12 * density);
			if (topBar != null) topBar.setPadding(padHoriz, padTop, padHoriz, (int)(6 * density));
			if (bottomControls != null) bottomControls.setPadding(padHoriz, 0, padHoriz, padBottom);

			isFullscreen = true;
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

			if (rootWatch != null) {
				rootWatch.setPadding(0, 0, 0, 0);
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				WindowManager.LayoutParams lp = window.getAttributes();
				lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
				window.setAttributes(lp);
			}

			window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

			int heightPx = (int) (220 * density);
			if (layoutPlayer != null) {
				ViewGroup.LayoutParams lp = layoutPlayer.getLayoutParams();
				lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
				lp.height = heightPx;
				layoutPlayer.setLayoutParams(lp);
			}

			if (scrollContent != null) scrollContent.setVisibility(View.VISIBLE);
			if (textPlayerTitle != null) textPlayerTitle.setVisibility(View.GONE);
			if (btnLock != null) btnLock.setVisibility(View.GONE);
			if (btnFullscreenEpisodes != null) btnFullscreenEpisodes.setVisibility(View.GONE);

			int sideMarginNormal = (int) (12 * density);
			if (btnPrevEpisode != null) {
				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) btnPrevEpisode.getLayoutParams();
				lp.width = (int) (32 * density);
				lp.height = (int) (32 * density);
				lp.setMargins(sideMarginNormal, 0, 0, 0);
				btnPrevEpisode.setLayoutParams(lp);
			}

			if (btnNextEpisode != null) {
				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) btnNextEpisode.getLayoutParams();
				lp.width = (int) (32 * density);
				lp.height = (int) (32 * density);
				lp.setMargins(0, 0, sideMarginNormal, 0);
				btnNextEpisode.setLayoutParams(lp);
			}

			int gapCenterNormal = (int) (22 * density);
			if (btnRewind != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnRewind.getLayoutParams();
				lp.width = (int) (36 * density);
				lp.height = (int) (36 * density);
				btnRewind.setLayoutParams(lp);
			}

			if (playContainer != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) playContainer.getLayoutParams();
				lp.width = (int) (64 * density);
				lp.height = (int) (64 * density);
				lp.setMargins(gapCenterNormal, 0, gapCenterNormal, 0);
				playContainer.setLayoutParams(lp);
			}

			if (playerLoading != null) {
				ViewGroup.LayoutParams lp = playerLoading.getLayoutParams();
				lp.width = (int) (64 * density);
				lp.height = (int) (64 * density);
				playerLoading.setLayoutParams(lp);
			}

			if (btnPlay != null) {
				ViewGroup.LayoutParams lp = btnPlay.getLayoutParams();
				lp.width = (int) (54 * density);
				lp.height = (int) (54 * density);
				btnPlay.setLayoutParams(lp);
				int padPlay = (int) (14 * density);
				btnPlay.setPadding(padPlay, padPlay, padPlay, padPlay);
			}

			if (btnForward != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnForward.getLayoutParams();
				lp.width = (int) (36 * density);
				lp.height = (int) (36 * density);
				btnForward.setLayoutParams(lp);
			}

			if (btnBack != null) {
				ViewGroup.LayoutParams lp = btnBack.getLayoutParams();
				lp.width = (int) (28 * density);
				lp.height = (int) (28 * density);
				btnBack.setLayoutParams(lp);
			}
			if (btnFullscreen != null) {
				btnFullscreen.setImageResource(R.drawable.ic_fullscreen);
				ViewGroup.LayoutParams lp = btnFullscreen.getLayoutParams();
				lp.width = (int) (24 * density);
				lp.height = (int) (24 * density);
				btnFullscreen.setLayoutParams(lp);
			}
			if (btnQuality != null) btnQuality.setTextSize(13f);
			if (textTime != null) textTime.setTextSize(13f);

			int defPadX = (int) (16 * density);
			int defPadTop = (int) (14 * density);
			int defPadBottom = (int) (6 * density);
			if (topBar != null) topBar.setPadding(defPadX, defPadTop, defPadX, (int)(6 * density));
			if (bottomControls != null) bottomControls.setPadding(0, 0, 0, defPadBottom);

			isFullscreen = false;
		}
		updateBottomDividerState();
		adjustVideoAspectRatio();
		resetControlsTimer();
	}

	public void _initPlayerViews() {
		final boolean[] isUserTrackingSeekBar = new boolean[]{false};
		playerHandler = new Handler(Looper.getMainLooper());

		autoDowngradeRunnable = new Runnable() {
			@Override
			public void run() {
				triggerAutoDowngrade();
			}
		};

		hideUnlockBtnRunnable = new Runnable() {
			@Override
			public void run() {
				if (btnUnlockScreen != null) {
					btnUnlockScreen.setVisibility(View.GONE);
				}
			}
		};

		GradientDrawable playCircle = new GradientDrawable();
		playCircle.setShape(GradientDrawable.OVAL);
		playCircle.setColor(0xFFFFFFFF);
		if (btnPlay != null) btnPlay.setBackground(playCircle);

		final float density = getResources().getDisplayMetrics().density;
		final int barHeight = (int) (4 * density);
		final int cornerRadius = (int) (2 * density);
		final int gapSize = (int) (4 * density);

		Drawable customTrackDrawable = new Drawable() {
			private final Paint paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint paintCache = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint paintProgress = new Paint(Paint.ANTI_ALIAS_FLAG);

			{
				paintBg.setColor(0x33FFFFFF);
				paintCache.setColor(0x80CCCCCC);
				paintProgress.setColor(0xFF3B82F6);
			}

			@Override
			public void draw(Canvas canvas) {
				Rect bounds = getBounds();
				float width = bounds.width();
				float top = bounds.centerY() - (barHeight / 2f);
				float bottom = bounds.centerY() + (barHeight / 2f);

				int max = playerSeekBar.getMax();
				if (max <= 0) max = 1;

				float progressRatio = (float) playerSeekBar.getProgress() / max;
				float cacheRatio = (float) playerSeekBar.getSecondaryProgress() / max;

				float progressRight = bounds.left + (width * progressRatio);
				float cacheRight = bounds.left + (width * cacheRatio);

				if (cacheRight > bounds.left) {
					canvas.drawRoundRect(new RectF(bounds.left, top, cacheRight, bottom), cornerRadius, cornerRadius, paintCache);
				}

				if (progressRight > bounds.left) {
					canvas.drawRoundRect(new RectF(bounds.left, top, progressRight, bottom), cornerRadius, cornerRadius, paintProgress);
				}

				float bgStart = Math.max(cacheRight, progressRight) + gapSize;
				if (bgStart < bounds.right) {
					canvas.drawRoundRect(new RectF(bgStart, top, bounds.right, bottom), cornerRadius, cornerRadius, paintBg);
				}
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};

		Drawable miniTrackDrawable = new Drawable() {
			private final Paint paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint paintCache = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint paintProgress = new Paint(Paint.ANTI_ALIAS_FLAG);

			{
				paintBg.setColor(0x33FFFFFF);
				paintCache.setColor(0x80CCCCCC);
				paintProgress.setColor(0xFF3B82F6);
			}

			@Override
			public void draw(Canvas canvas) {
				if (miniVideoProgressBar == null) return;
				Rect bounds = getBounds();
				float width = bounds.width();
				float top = bounds.top;
				float bottom = bounds.bottom;

				int max = miniVideoProgressBar.getMax();
				if (max <= 0) max = 1000;

				float progressRatio = (float) miniVideoProgressBar.getProgress() / max;
				float cacheRatio = (float) miniVideoProgressBar.getSecondaryProgress() / max;

				float progressRight = bounds.left + (width * progressRatio);
				float cacheRight = bounds.left + (width * cacheRatio);

				if (cacheRight > bounds.left) {
					canvas.drawRect(bounds.left, top, cacheRight, bottom, paintCache);
				}

				if (progressRight > bounds.left) {
					canvas.drawRect(bounds.left, top, progressRight, bottom, paintProgress);
				}

				float bgStart = Math.max(cacheRight, progressRight) + gapSize;
				if (bgStart < bounds.right) {
					canvas.drawRect(bgStart, top, bounds.right, bottom, paintBg);
				}
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};

		if (miniVideoProgressBar != null) {
			miniVideoProgressBar.setProgressDrawable(miniTrackDrawable);
		}

		if (playerSeekBar != null) {
			playerSeekBar.setProgressDrawable(customTrackDrawable);

			final int normalThumbSize = (int) (12 * density);
			final int activeThumbSize = (int) (16 * density);

			final GradientDrawable customThumb = new GradientDrawable();
			customThumb.setShape(GradientDrawable.OVAL);
			customThumb.setColor(0xFF3B82F6);
			customThumb.setSize(normalThumbSize, normalThumbSize);

			playerSeekBar.setThumb(customThumb);
			playerSeekBar.setThumbOffset(normalThumbSize / 2);

			playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					seekBar.invalidate();
					if (fromUser && textTime != null) {
						int curSec = (progress / 1000) % 60;
						int curMin = (progress / (1000 * 60)) % 60;
						int total = (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
						int totSec = (total / 1000) % 60;
						int totMin = (total / (1000 * 60)) % 60;
						textTime.setText(String.format("%02d:%02d / %02d:%02d", curMin, curSec, totMin, totSec));
					}
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					isUserTrackingSeekBar[0] = true;
					customThumb.setSize(activeThumbSize, activeThumbSize);
					seekBar.setThumb(customThumb);
					seekBar.setThumbOffset(activeThumbSize / 2);

					if (playerHandler != null && hideControlsRunnable != null) {
						playerHandler.removeCallbacks(hideControlsRunnable);
					}
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					if (mediaPlayer != null) {
						mediaPlayer.seekTo(seekBar.getProgress());
						saveWatchHistoryToFirebase(seekBar.getProgress(), mediaPlayer.getDuration(), true);
					}
					customThumb.setSize(normalThumbSize, normalThumbSize);
					seekBar.setThumb(customThumb);
					seekBar.setThumbOffset(normalThumbSize / 2);

					isUserTrackingSeekBar[0] = false;
					resetControlsTimer();
				}
			});
		}

		hideControlsRunnable = new Runnable() {
			@Override
			public void run() {
				hideControls();
			}
		};

		final GestureDetector playerGestureDetector = new GestureDetector(WatchActivity.this, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				toggleControls();
				return true;
			}

			@Override
			public void onLongPress(MotionEvent e) {
				startHold2xSpeed();
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				if (isScreenLocked) {
					showUnlockPillTemporarily();
					return true;
				}
				if (mediaPlayer == null || layoutPlayer == null) return false;

				float touchX = e.getX();
				int viewWidth = layoutPlayer.getWidth();

				if (touchX < (viewWidth / 2f)) {
					int targetPos = Math.max(0, mediaPlayer.getCurrentPosition() - 10000);
					mediaPlayer.seekTo(targetPos);
					if (playerSeekBar != null) {
						playerSeekBar.setProgress(targetPos);
						playerSeekBar.invalidate();
					}
					showSeekAnimation(animSeekLeft);
				} else {
					int targetPos = Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + 10000);
					mediaPlayer.seekTo(targetPos);
					if (playerSeekBar != null) {
						playerSeekBar.setProgress(targetPos);
						playerSeekBar.invalidate();
					}
					showSeekAnimation(animSeekRight);
				}
				resetControlsTimer();
				return true;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}
		});

		View.OnTouchListener playerTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					resetControlsTimer();
				}
				if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
					if (isHolding2xSpeed) {
						stopHold2xSpeed();
						return true;
					}
				}
				return playerGestureDetector.onTouchEvent(event);
			}
		};

		if (layoutPlayer != null) {
			layoutPlayer.setOnTouchListener(playerTouchListener);
		}

		if (playerControlsOverlay != null) {
			playerControlsOverlay.setOnTouchListener(playerTouchListener);
		}

		if (playerTextureView != null) {
			playerTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
				@Override
				public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
					playerSurface = new Surface(surfaceTexture);
					if (mediaPlayer != null) {
						try {
							mediaPlayer.setSurface(playerSurface);
						} catch (Exception ignored) {}
					}
					adjustVideoAspectRatio();
				}
				@Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
				@Override
				public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
					if (playerSurface != null) {
						playerSurface.release();
						playerSurface = null;
					}
					if (mediaPlayer != null && !DongflixPlayerManager.getInstance().isMiniPlayerActive) {
						mediaPlayer.release();
						mediaPlayer = null;
					}
					return true;
				}
				@Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
			});
		}

		progressLoop = new Runnable() {
			@Override
			public void run() {
				try {
					if (mediaPlayer != null && mediaPlayer.isPlaying() && !isUserTrackingSeekBar[0]) {
						int cur = mediaPlayer.getCurrentPosition();
						int tot = mediaPlayer.getDuration();

						if (playerSeekBar != null) {
							playerSeekBar.setProgress(cur);
							playerSeekBar.invalidate();
						}

						if (miniVideoProgressBar != null && tot > 0) {
							int permille = (int) ((cur * 1000L) / tot);
							miniVideoProgressBar.setProgress(permille);
							miniVideoProgressBar.invalidate();
						}

						if (textTime != null) {
							int curSec = (cur / 1000) % 60;
							int curMin = (cur / (1000 * 60)) % 60;
							int totSec = (tot / 1000) % 60;
							int totMin = (tot / (1000 * 60)) % 60;
							textTime.setText(String.format("%02d:%02d / %02d:%02d", curMin, curSec, totMin, totSec));
						}

						saveWatchHistoryToFirebase(cur, tot, false);
					}
				} catch (Exception ignored) {}

				if (playerHandler != null) {
					playerHandler.postDelayed(this, 500);
				}
			}
		};

		if (btnPlay != null) {
			btnPlay.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					if (mediaPlayer != null) {
						if (mediaPlayer.isPlaying()) {
							mediaPlayer.pause();
							btnPlay.setImageResource(R.drawable.ic_play1);
							getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
							saveWatchHistoryToFirebase(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration(), true);
						} else {
							mediaPlayer.start();
							btnPlay.setImageResource(R.drawable.ic_pause);
							getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
						}
					}
				}
			});
		}

		if (btnRewind != null) {
			btnRewind.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					if (mediaPlayer != null) {
						int targetPos = Math.max(0, mediaPlayer.getCurrentPosition() - 10000);
						mediaPlayer.seekTo(targetPos);
						if (playerSeekBar != null) {
							playerSeekBar.setProgress(targetPos);
							playerSeekBar.invalidate();
						}
						showSeekAnimation(animSeekLeft);
					}
				}
			});
		}

		if (btnForward != null) {
			btnForward.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					if (mediaPlayer != null) {
						int targetPos = Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + 10000);
						mediaPlayer.seekTo(targetPos);
						if (playerSeekBar != null) {
							playerSeekBar.setProgress(targetPos);
							playerSeekBar.invalidate();
						}
						showSeekAnimation(animSeekRight);
					}
				}
			});
		}

		if (btnPrevEpisode != null) {
			btnPrevEpisode.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					if (currentEpisodeIndex > 0) {
						playEpisodeByIndex(currentEpisodeIndex - 1, 0);
					}
				}
			});
		}

		if (btnNextEpisode != null) {
			btnNextEpisode.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					int totalItems = (recyclerEpisode != null && recyclerEpisode.getAdapter() != null) ? recyclerEpisode.getAdapter().getItemCount() : 0;
					if (currentEpisodeIndex < (totalItems - 1)) {
						playEpisodeByIndex(currentEpisodeIndex + 1, 0);
					}
				}
			});
		}

		if (btnLock != null) {
			btnLock.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					lockScreenControls();
				}
			});
		}

		if (btnUnlockScreen != null) {
			btnUnlockScreen.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					unlockScreenControls();
				}
			});
		}

		if (btnCast != null) {
			btnCast.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					try {
						startActivity(new Intent(Settings.ACTION_CAST_SETTINGS));
					} catch (Exception e) {
						Toast.makeText(WatchActivity.this, "Fitur Cast tidak tersedia di perangkat ini", Toast.LENGTH_SHORT).show();
					}
				}
			});
		}

		if (btnSettings != null) {
			btnSettings.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					showSettingsBottomSheet();
				}
			});
		}

		if (btnQuality != null) {
			btnQuality.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					showQualityDialog();
				}
			});
		}

		if (btnFullscreenEpisodes != null) {
			btnFullscreenEpisodes.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					showAllEpisodesBottomSheet();
				}
			});
		}

		if (btnOpenDetail != null) {
			btnOpenDetail.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showDetailBottomSheet();
				}
			});
		}

		if (btnSortEpisodes != null) {
			btnSortEpisodes.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleSortEpisodes();
				}
			});
		}

		TextView btnSeeAllEpisodes = findViewById(R.id.textview85);
		if (btnSeeAllEpisodes != null) {
			btnSeeAllEpisodes.setTextColor(Color.parseColor("#94A3B8"));
			btnSeeAllEpisodes.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showAllEpisodesBottomSheet();
				}
			});
		}

		if (btnFill != null) {
			btnFill.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					isFillModeActive = !isFillModeActive;
					adjustVideoAspectRatio();
					Toast.makeText(WatchActivity.this, isFillModeActive ? "Mode: Fill" : "Mode: Original", Toast.LENGTH_SHORT).show();
				}
			});
		}

		if (btnActionServer != null) {
			btnActionServer.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showServerDialog();
				}
			});
		}

		if (btnFullscreen != null) {
			btnFullscreen.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					toggleFullscreen();
				}
			});
		}

		if (btnBack != null) {
			btnBack.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isFullscreen) {
						toggleFullscreen();
					} else {
						minimizeToMiniPlayer();
					}
				}
			});
		}

		updateEpisodeNavButtons();
	}

	private String fetchHttpString(String targetUrl, String referer, boolean saveDmCookie) throws Exception {
		String currentUrl = targetUrl;
		HttpURLConnection conn = null;

		for (int redirectCount = 0; redirectCount < 6; redirectCount++) {
			URL url = new URL(currentUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
			conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8");
			conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,id;q=0.8");
			conn.setRequestProperty("Sec-Fetch-Dest", "iframe");
			conn.setRequestProperty("Sec-Fetch-Mode", "navigate");
			if (referer != null && !referer.isEmpty()) {
				conn.setRequestProperty("Referer", referer);
			}

			if (saveDmCookie) {
				Map<String, List<String>> headerFields = conn.getHeaderFields();
				List<String> cookiesHeader = headerFields.get("Set-Cookie");
				if (cookiesHeader != null && !cookiesHeader.isEmpty()) {
					StringBuilder cookieSb = new StringBuilder();
					for (String c : cookiesHeader) {
						String mainPart = c.split(";")[0];
						if (cookieSb.length() > 0) cookieSb.append("; ");
						cookieSb.append(mainPart);
					}
					dmSessionCookie = cookieSb.toString();
				}
			}

			int code = conn.getResponseCode();
			if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
				String loc = conn.getHeaderField("Location");
				conn.disconnect();
				if (loc == null || loc.isEmpty()) break;
				if (loc.startsWith("//")) loc = "https:" + loc;
				else if (loc.startsWith("/")) loc = url.getProtocol() + "://" + url.getHost() + loc;
				currentUrl = loc;
				continue;
			}
			break;
		}

		if (conn == null) return "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append("\n");
		}
		reader.close();
		conn.disconnect();
		return sb.toString();
	}

	private String postJsonHttp(String apiUrl, String jsonPayload) throws Exception {
		URL url = new URL(apiUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(10000);
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0 Safari/537.36");

		OutputStream os = conn.getOutputStream();
		os.write(jsonPayload.getBytes("UTF-8"));
		os.flush();
		os.close();

		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		conn.disconnect();
		return sb.toString();
	}

	private String unpackDeanEdwards(String html) {
		try {
			Pattern p = Pattern.compile("eval\\(function\\(p,a,c,k,e,[dr]\\)\\{.*?\\}\\('(.*?)',(\\d+),(\\d+),'(.*?)'\\.split\\('\\|'\\)", Pattern.DOTALL);
			Matcher m = p.matcher(html);
			if (m.find()) {
				String payload = m.group(1);
				int radix = Integer.parseInt(m.group(2));
				String[] symtab = m.group(4).split("\\|");

				Pattern wordPat = Pattern.compile("\\b\\w+\\b");
				Matcher wm = wordPat.matcher(payload);
				StringBuffer sb = new StringBuffer();
				while (wm.find()) {
					String word = wm.group();
					try {
						int idx = Integer.parseInt(word, radix);
						if (idx >= 0 && idx < symtab.length && !symtab[idx].isEmpty()) {
							wm.appendReplacement(sb, Matcher.quoteReplacement(symtab[idx]));
						}
					} catch (Exception ignored) {}
				}
				wm.appendTail(sb);
				return sb.toString();
			}
		} catch (Exception ignored) {}
		return "";
	}

	public void extractStream(final String server, final String targetUrl) {
		extractStream(server, targetUrl, 0);
	}

	public void extractStream(final String server, final String targetUrl, final int seekOnReadyMs) {
		if (targetUrl == null || targetUrl.trim().isEmpty()) return;

		String cleanUrl = targetUrl.trim();
		if (cleanUrl.startsWith("//")) {
			cleanUrl = "https:" + cleanUrl;
		} else if (!cleanUrl.startsWith("http")) {
			cleanUrl = "https://" + cleanUrl;
		}
		final String finalTargetUrl = cleanUrl;
		final String serverKey = (server != null) ? server.trim().toLowerCase() : "";
		final String lowerUrl = finalTargetUrl.toLowerCase();
		currentStreamReferer = finalTargetUrl;

		if (serverKey.contains("ok") || serverKey.contains("odnoklassniki") || lowerUrl.contains("ok.ru")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String html = fetchHttpString(finalTargetUrl, "https://ok.ru/", false);
						String foundOptions = "";
						int idx = html.indexOf("data-options=");
						if (idx != -1) {
							int qStart = idx + 13;
							char quote = html.charAt(qStart);
							int qEnd = html.indexOf(quote, qStart + 1);
							if (qEnd != -1) {
								foundOptions = html.substring(qStart + 1, qEnd);
							}
						}

						final ArrayList<String> tempNames = new ArrayList<>();
						final ArrayList<String> tempUrls = new ArrayList<>();

						if (!foundOptions.isEmpty()) {
							String cleanJson = foundOptions
								.replace("&quot;", "\"")
								.replace("&amp;", "&")
								.replace("&#39;", "'")
								.replace("&lt;", "<")
								.replace("&gt;", ">");

							JSONObject optJson = new JSONObject(cleanJson);
							JSONObject metadata = null;
							if (optJson.has("flashvars")) {
								JSONObject fv = optJson.getJSONObject("flashvars");
								if (fv.has("metadata")) {
									Object mObj = fv.get("metadata");
									metadata = (mObj instanceof JSONObject) ? (JSONObject) mObj : new JSONObject(mObj.toString());
								}
							} else if (optJson.has("metadata")) {
								Object mObj = optJson.get("metadata");
								metadata = (mObj instanceof JSONObject) ? (JSONObject) mObj : new JSONObject(mObj.toString());
							}

							if (metadata != null && metadata.has("videos")) {
								JSONArray videos = metadata.getJSONArray("videos");
								for (int k = 0; k < videos.length(); k++) {
									JSONObject v = videos.getJSONObject(k);
									if (v.has("url")) {
										String rawUrl = v.getString("url").replace("\\/", "/").replace("\\u0026", "&");
										String rName = v.optString("name", "Resolusi " + (k + 1)).toUpperCase();

										if (rName.equalsIgnoreCase("mobile")) rName = "144P";
										if (rName.equalsIgnoreCase("lowest")) rName = "240P";
										if (rName.equalsIgnoreCase("low")) rName = "360P";
										if (rName.equalsIgnoreCase("medium")) rName = "480P";
										if (rName.equalsIgnoreCase("high")) rName = "720P";
										if (rName.equalsIgnoreCase("hd")) rName = "1080P";

										tempNames.add(rName);
										tempUrls.add(rawUrl);
									}
								}
							}
						}

						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!tempUrls.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.addAll(tempNames);
									qualityUrls.addAll(tempUrls);

									isAutoQualityMode = true;
									selectedQualityIndex = getRecommendedQualityIndex();
									updateQualityBadgeText();

									playVideoUrl(qualityUrls.get(selectedQualityIndex), seekOnReadyMs);
								} else {
									playViaWebViewInterceptor(finalTargetUrl);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();

		} else if (serverKey.contains("daily") || lowerUrl.contains("dailymotion") || lowerUrl.contains("dai.ly")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String dailyId = "";
						Uri parsedUri = Uri.parse(finalTargetUrl);
						if (parsedUri.getQueryParameter("id") != null && !parsedUri.getQueryParameter("id").isEmpty()) {
							dailyId = parsedUri.getQueryParameter("id");
						} else if (parsedUri.getQueryParameter("video") != null && !parsedUri.getQueryParameter("video").isEmpty()) {
							dailyId = parsedUri.getQueryParameter("video");
						} else if (finalTargetUrl.contains("/video/")) {
							String[] parts = finalTargetUrl.split("/video/");
							if (parts.length > 1) dailyId = parts[1].split("[?&#_]")[0];
						} else if (finalTargetUrl.contains("dai.ly/")) {
							String[] parts = finalTargetUrl.split("dai.ly/");
							if (parts.length > 1) dailyId = parts[1].split("[?&#]")[0];
						} else {
							String cPath = finalTargetUrl.split("\\?")[0];
							dailyId = cPath.substring(cPath.lastIndexOf('/') + 1);
						}

						if (dailyId == null || dailyId.trim().isEmpty()) {
							throw new Exception("ID Dailymotion kosong");
						}

						final String cleanDmId = dailyId.trim();
						String dmApiUrl = "https://www.dailymotion.com/player/metadata/video/" + cleanDmId + "?embedder=https%3A%2F%2Fwww.dailymotion.com%2F&app=com.dailymotion.neon";
						String resBody = fetchHttpString(dmApiUrl, "https://www.dailymotion.com/", true);

						final ArrayList<String> dmNames = new ArrayList<>();
						final ArrayList<String> dmUrls = new ArrayList<>();

						if (!resBody.isEmpty()) {
							JSONObject metaObj = new JSONObject(resBody);
							if (metaObj.has("qualities")) {
								JSONObject qualities = metaObj.getJSONObject("qualities");
								String[] checkKeys = new String[]{"1080", "720", "480", "380", "240", "auto"};

								for (String qKey : checkKeys) {
									if (qualities.has(qKey)) {
										JSONArray qArr = qualities.optJSONArray(qKey);
										if (qArr != null) {
											String chosenUrl = "";
											for (int idx = 0; idx < qArr.length(); idx++) {
												JSONObject itemQ = qArr.getJSONObject(idx);
												String mime = itemQ.optString("type", "");
												String u = itemQ.optString("url", "");
												if (!u.isEmpty() && mime.contains("mp4")) {
													chosenUrl = u;
													break;
												}
											}
											if (chosenUrl.isEmpty() && qArr.length() > 0) {
												chosenUrl = qArr.getJSONObject(0).optString("url", "");
											}
											if (!chosenUrl.isEmpty()) {
												String label = qKey.equalsIgnoreCase("auto") ? "Auto" : qKey + "P";
												dmNames.add(label);
												dmUrls.add(chosenUrl);
											}
										}
									}
								}
							}
						}

						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!dmUrls.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.addAll(dmNames);
									qualityUrls.addAll(dmUrls);

									isAutoQualityMode = true;
									selectedQualityIndex = getRecommendedQualityIndex();
									updateQualityBadgeText();

									playVideoUrl(qualityUrls.get(selectedQualityIndex), seekOnReadyMs);
								} else {
									playViaWebViewInterceptor("https://geo.dailymotion.com/player.html?video=" + cleanDmId);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();

		} else if (serverKey.contains("d-tube") || serverKey.contains("dtube") || lowerUrl.contains("d.tube")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String streamFound = "";

						Pattern pDirectQm = Pattern.compile("(Qm[a-zA-Z0-9]{44})");
						Matcher mDirectQm = pDirectQm.matcher(finalTargetUrl);
						if (mDirectQm.find()) {
							streamFound = "https://dweb.link/ipfs/" + mDirectQm.group(1);
						}

						if (streamFound.isEmpty() && finalTargetUrl.contains("/v/")) {
							String afterV = finalTargetUrl.substring(finalTargetUrl.indexOf("/v/") + 3).split("[?#]")[0];
							String[] parts = afterV.split("/");
							if (parts.length >= 2) {
								String author = parts[0].trim();
								String permlink = parts[1].trim();
								String rpcBody = "{\"jsonrpc\":\"2.0\",\"method\":\"condenser_api.get_content\",\"params\":[\"" + author + "\",\"" + permlink + "\"],\"id\":1}";
								String rpcRes = postJsonHttp("https://api.hive.blog", rpcBody);
								JSONObject rpcJson = new JSONObject(rpcRes);
								if (rpcJson.has("result")) {
									JSONObject resObj = rpcJson.getJSONObject("result");
									String metaStr = resObj.optString("json_metadata", "");
									if (!metaStr.isEmpty()) {
										JSONObject metaJson = new JSONObject(metaStr);
										if (metaJson.has("video")) {
											JSONObject videoObj = metaJson.getJSONObject("video");
											if (videoObj.has("files")) {
												JSONObject filesObj = videoObj.getJSONObject("files");
												if (filesObj.has("ipfs")) {
													JSONObject ipfsObj = filesObj.getJSONObject("ipfs");
													if (ipfsObj.has("vid")) {
														JSONObject vidObj = ipfsObj.getJSONObject("vid");
														String hash = vidObj.optString("720", vidObj.optString("480", vidObj.optString("src", "")));
														if (!hash.isEmpty()) {
															streamFound = "https://dweb.link/ipfs/" + hash;
														}
													}
												}
											}
										}
									}
								}
							}
						}

						if (streamFound.isEmpty()) {
							String html = fetchHttpString(finalTargetUrl, "https://emb.d.tube/", false);
							Matcher mQm = pDirectQm.matcher(html);
							if (mQm.find()) {
								streamFound = "https://dweb.link/ipfs/" + mQm.group(1);
							}
						}

						final String finalDtubeUrl = streamFound;
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!finalDtubeUrl.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.add("Default");
									qualityUrls.add(finalDtubeUrl);
									isAutoQualityMode = true;
									selectedQualityIndex = 0;
									updateQualityBadgeText();

									playVideoUrl(finalDtubeUrl, seekOnReadyMs);
								} else {
									playViaWebViewInterceptor(finalTargetUrl);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();

		} else if (serverKey.contains("rumble") || lowerUrl.contains("rumble.com")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String embedId = "";
						if (finalTargetUrl.contains("/embed/")) {
							String[] parts = finalTargetUrl.split("/embed/");
							if (parts.length > 1) embedId = parts[1].split("[/?#]")[0];
						}

						final ArrayList<String> rNames = new ArrayList<>();
						final ArrayList<String> rUrls = new ArrayList<>();

						if (embedId.isEmpty()) {
							String pageHtml = fetchHttpString(finalTargetUrl, "https://rumble.com/", false);
							Pattern pEmbed = Pattern.compile("rumble\\.com/embed/([a-zA-Z0-9]+)");
							Matcher mEmbed = pEmbed.matcher(pageHtml);
							if (mEmbed.find()) {
								embedId = mEmbed.group(1);
							}
						}

						if (!embedId.isEmpty()) {
							String apiUrl = "https://rumble.com/embedJS/u3/?request=video&ver=2&v=" + embedId;
							String jsonStr = fetchHttpString(apiUrl, "https://rumble.com/", false);
							JSONObject rootObj = new JSONObject(jsonStr);

							if (rootObj.has("ua")) {
								JSONObject uaObj = rootObj.getJSONObject("ua");
								String[] resKeys = new String[]{"1080", "720", "480", "360", "240"};

								if (uaObj.has("mp4")) {
									JSONObject mp4Obj = uaObj.getJSONObject("mp4");
									for (String rk : resKeys) {
										if (mp4Obj.has(rk)) {
											String u = mp4Obj.getJSONObject(rk).optString("url");
											if (!u.isEmpty()) {
												rNames.add(rk + "P");
												rUrls.add(u);
											}
										}
									}
								}

								if (rUrls.isEmpty()) {
									for (String rk : resKeys) {
										if (uaObj.has(rk)) {
											JSONObject resObj = uaObj.getJSONObject(rk);
											if (resObj != null) {
												String u = resObj.optString("url");
												if (!u.isEmpty()) {
													rNames.add(rk + "P");
													rUrls.add(u);
												}
											}
										}
									}
								}

								if (rUrls.isEmpty() && uaObj.has("hls")) {
									JSONObject hlsObj = uaObj.getJSONObject("hls");
									if (hlsObj.has("auto")) {
										String u = hlsObj.getJSONObject("auto").optString("url");
										if (!u.isEmpty()) {
											rNames.add("Auto");
											rUrls.add(u);
										}
									}
								}
							}
						}

						if (rUrls.isEmpty()) {
							String html = fetchHttpString(finalTargetUrl, "https://rumble.com/", false);
							Pattern pMp4 = Pattern.compile("(https?://[a-zA-Z0-9.-]+\\.rumble\\.com/[^\"'\\s]+\\.mp4)");
							Matcher mMp4 = pMp4.matcher(html);
							if (mMp4.find()) {
								rNames.add("Default");
								rUrls.add(mMp4.group(1));
							}
						}

						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!rUrls.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.addAll(rNames);
									qualityUrls.addAll(rUrls);

									isAutoQualityMode = true;
									selectedQualityIndex = getRecommendedQualityIndex();
									updateQualityBadgeText();

									playVideoUrl(qualityUrls.get(selectedQualityIndex), seekOnReadyMs);
								} else {
									playViaWebViewInterceptor(finalTargetUrl);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();

		} else if (serverKey.contains("odysee") || lowerUrl.contains("odysee.com") || lowerUrl.contains("odycdn.com")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String streamOdysee = "";
						if (finalTargetUrl.contains("odycdn.com") && (finalTargetUrl.contains(".mp4") || finalTargetUrl.contains(".m3u8") || finalTargetUrl.contains("/streams/"))) {
							streamOdysee = finalTargetUrl;
						} else if (finalTargetUrl.contains("signature=")) {
							Uri uri = Uri.parse(finalTargetUrl);
							String sig = uri.getQueryParameter("signature");
							String sigTs = uri.getQueryParameter("signature_ts");
							String path = uri.getPath();
							if (path != null && path.contains("/embed/")) {
								String cleanClaim = path.replace("/$/embed/", "").replace("/embed/", "");
								streamOdysee = "https://player.odycdn.com/api/v4/streams/free/" + cleanClaim + "/" + sig + "/" + sigTs;
							}
						}

						if (streamOdysee.isEmpty()) {
							String html = fetchHttpString(finalTargetUrl, "https://odysee.com/", false);
							Pattern pOdy = Pattern.compile("(https?://player\\.odycdn\\.com/api/v4/streams/[^\"'\\s]+)");
							Matcher mOdy = pOdy.matcher(html);
							if (mOdy.find()) {
								streamOdysee = mOdy.group(1).replace("\\/", "/");
							} else {
								Pattern pMedia = Pattern.compile("(https?://[^\"'\\s]+\\.(?:mp4|m3u8)[^\"'\\s]*)");
								Matcher mMedia = pMedia.matcher(html);
								if (mMedia.find()) {
									streamOdysee = mMedia.group(1).replace("\\/", "/");
								}
							}
						}

						if (streamOdysee.isEmpty() && finalTargetUrl.contains("/embed/")) {
							String afterEmbed = finalTargetUrl.substring(finalTargetUrl.indexOf("/embed/") + 7).split("[?#]")[0];
							String lbryUri = "lbry://" + afterEmbed.replace("/", "#");
							String lbryReq = "{\"method\":\"get\",\"params\":{\"uri\":\"" + lbryUri + "\"}}";
							String lbryRes = postJsonHttp("https://api.na-backend.odysee.com/api/v1/proxy", lbryReq);
							JSONObject lbryJson = new JSONObject(lbryRes);
							if (lbryJson.has("result")) {
								streamOdysee = lbryJson.getJSONObject("result").optString("streaming_url", "");
							}
						}

						final String finalOdyUrl = streamOdysee;
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!finalOdyUrl.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.add("Default");
									qualityUrls.add(finalOdyUrl);
									isAutoQualityMode = true;
									selectedQualityIndex = 0;
									updateQualityBadgeText();

									playVideoUrl(finalOdyUrl, seekOnReadyMs);
								} else {
									playViaWebViewInterceptor(finalTargetUrl);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();

		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if (lowerUrl.contains(".mp4") || lowerUrl.contains(".m3u8")) {
							new Handler(Looper.getMainLooper()).post(new Runnable() {
								@Override
								public void run() {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.add("Default");
									qualityUrls.add(finalTargetUrl);
									isAutoQualityMode = true;
									selectedQualityIndex = 0;
									updateQualityBadgeText();

									playVideoUrl(finalTargetUrl, seekOnReadyMs);
								}
							});
							return;
						}

						String html = fetchHttpString(finalTargetUrl, finalTargetUrl, false);
						String unpacked = unpackDeanEdwards(html);
						String combinedSource = html + "\n" + unpacked;
						String streamFound = "";

						Pattern pSource = Pattern.compile("<(?:source|video)[^>]+src=[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
						Matcher mSource = pSource.matcher(combinedSource);
						if (mSource.find()) {
							streamFound = mSource.group(1).replace("&amp;", "&");
						}

						if (streamFound.isEmpty()) {
							Pattern pMedia = Pattern.compile("(https?:\\\\?/\\\\?/[^\"'\\s<>]+\\.(?:m3u8|mp4)[^\"'\\s<>]*)", Pattern.CASE_INSENSITIVE);
							Matcher mMedia = pMedia.matcher(combinedSource);
							if (mMedia.find()) {
								streamFound = mMedia.group(1).replace("\\/", "/").replace("\\u0026", "&");
							}
						}

						if (streamFound.isEmpty()) {
							Pattern pIframe = Pattern.compile("<iframe[^>]+src=[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
							Matcher mIframe = pIframe.matcher(combinedSource);
							if (mIframe.find()) {
								String innerUrl = mIframe.group(1).replace("&amp;", "&");
								currentStreamReferer = innerUrl;
								String innerHtml = fetchHttpString(innerUrl, finalTargetUrl, false);
								String innerUnpacked = unpackDeanEdwards(innerHtml);
								String innerCombined = innerHtml + "\n" + innerUnpacked;
								Pattern pInnerMedia = Pattern.compile("(https?:\\\\?/\\\\?/[^\"'\\s<>]+\\.(?:m3u8|mp4)[^\"'\\s<>]*)", Pattern.CASE_INSENSITIVE);
								Matcher mInnerMedia = pInnerMedia.matcher(innerCombined);
								if (mInnerMedia.find()) {
									streamFound = mInnerMedia.group(1).replace("\\/", "/").replace("\\u0026", "&");
								}
							}
						}

						final String finalGenericUrl = streamFound;
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!finalGenericUrl.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.add("Default");
									qualityUrls.add(finalGenericUrl);
									isAutoQualityMode = true;
									selectedQualityIndex = 0;
									updateQualityBadgeText();

									playVideoUrl(finalGenericUrl, seekOnReadyMs);
								} else {
									playViaWebViewInterceptor(finalTargetUrl);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();
		}
	}

	public class ShimmerEpisodeAdapter extends RecyclerView.Adapter<ShimmerEpisodeAdapter.ViewHolder> {
		private final int count;
		public ShimmerEpisodeAdapter(int count) { this.count = count; }

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			float d = parent.getContext().getResources().getDisplayMetrics().density;
			View v = new View(parent.getContext());
			RecyclerView.LayoutParams params = new RecyclerView.LayoutParams((int)(65 * d), (int)(60 * d));
			params.setMarginEnd((int)(8 * d));
			v.setLayoutParams(params);
			v.setBackground(createShimmerDrawable(12 * d));
			return new ViewHolder(v);
		}

		@Override public void onBindViewHolder(ViewHolder holder, int position) {}
		@Override public int getItemCount() { return count; }
		public class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(View v) { super(v); } }
	}

	public class ShimmerRekomendasiAdapter extends RecyclerView.Adapter<ShimmerRekomendasiAdapter.ViewHolder> {
		private final int count;
		public ShimmerRekomendasiAdapter(int count) { this.count = count; }

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			Context ctx = parent.getContext();
			float d = ctx.getResources().getDisplayMetrics().density;

			LinearLayout box = new LinearLayout(ctx);
			box.setOrientation(LinearLayout.VERTICAL);
			GridLayoutManager.LayoutParams lp = new GridLayoutManager.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			);
			lp.setMargins((int)(4 * d), 0, (int)(4 * d), (int)(14 * d));
			box.setLayoutParams(lp);

			View coverStub = new View(ctx);
			LinearLayout.LayoutParams covLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(145 * d));
			coverStub.setLayoutParams(covLp);
			coverStub.setBackground(createShimmerDrawable(10 * d));
			box.addView(coverStub);

			View titleStub = new View(ctx);
			LinearLayout.LayoutParams titLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(14 * d));
			titLp.topMargin = (int)(8 * d);
			titleStub.setLayoutParams(titLp);
			titleStub.setBackground(createShimmerDrawable(5 * d));
			box.addView(titleStub);

			return new ViewHolder(box);
		}

		@Override public void onBindViewHolder(ViewHolder holder, int position) {}
		@Override public int getItemCount() { return count; }
		public class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(View v) { super(v); } }
	}

	public class RecyclerEpisodeAdapter extends RecyclerView.Adapter<RecyclerEpisodeAdapter.ViewHolder> {

		ArrayList<HashMap<String, Object>> _data;

		public RecyclerEpisodeAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.eps_list, parent, false);
			return new ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			final int pos = _position;
			final View itemView = _holder.itemView;
			final float density = itemView.getContext().getResources().getDisplayMetrics().density;

			int widthPx = (int) (65 * density);
			int heightPx = (int) (60 * density);
			int marginEndPx = (int) (8 * density);

			RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(widthPx, heightPx);
			params.setMarginEnd(marginEndPx);
			itemView.setLayoutParams(params);

			GradientDrawable itemBg = new GradientDrawable();
			itemBg.setCornerRadius(12 * density);

			TextView tvEpisode = itemView.findViewById(R.id.textview1);
			final boolean isActive = (pos == currentEpisodeIndex);

			if (isActive) {
				itemBg.setColor(0xFF3B82F6);
				if (tvEpisode != null) {
					tvEpisode.setTextColor(0xFFFFFFFF);
					tvEpisode.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
				}
			} else {
				itemBg.setColor(Color.parseColor("#12141A"));
				if (tvEpisode != null) {
					tvEpisode.setTextColor(0xFFCBD5E1);
					tvEpisode.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
				}
			}
			itemView.setBackground(itemBg);

			if (tvEpisode != null && _data.get(pos) != null) {
				HashMap<String, Object> itemData = _data.get(pos);
				if (itemData.containsKey("episode")) {
					Object epObj = itemData.get("episode");
					if (epObj != null) {
						tvEpisode.setText(epObj.toString().replace("Ep ", "").replace("Eps ", "").trim());
					}
				} else if (itemData.containsKey("title")) {
					Object tObj = itemData.get("title");
					if (tObj != null) {
						tvEpisode.setText(tObj.toString().replaceAll("[^0-9]", "").trim());
					}
				} else {
					tvEpisode.setText(String.valueOf(pos + 1));
				}
			}

			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentEpisodeIndex == pos && mediaPlayer != null && mediaPlayer.isPlaying()) {
						return;
					}

					if (episodeList != _data) {
						episodeList.clear();
						episodeList.addAll(_data);
					}

					playEpisodeByIndex(pos, 0);
				}
			});
		}

		@Override
		public int getItemCount() {
			return _data.size();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}

	public class RecyclerRekomendasiAdapter extends RecyclerView.Adapter<RecyclerRekomendasiAdapter.ViewHolder> {

		ArrayList<HashMap<String, Object>> _data;

		public RecyclerRekomendasiAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = getLayoutInflater().inflate(R.layout.custom_anime, parent, false);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			final HashMap<String, Object> item = _data.get(position);
			View view = holder.itemView;
			float d = view.getResources().getDisplayMetrics().density;

			GridLayoutManager.LayoutParams lp = new GridLayoutManager.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			);
			lp.setMargins((int)(4 * d), 0, (int)(4 * d), (int)(14 * d));
			view.setLayoutParams(lp);

			TextView textTitle = view.findViewById(R.id.text_title);
			TextView textStatus = view.findViewById(R.id.text_status);
			TextView textEpisode = view.findViewById(R.id.text_episode);
			ImageView imgCover = view.findViewById(R.id.img_cover);
			View view2 = view.findViewById(R.id.view2);

			if (textStatus != null) {
				GradientDrawable statusBg = new GradientDrawable();
				statusBg.setCornerRadius(50f);
				statusBg.setColor(0xFF3B82F6);
				textStatus.setBackground(statusBg);
				textStatus.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
				textStatus.setText(item.get("status") != null ? item.get("status").toString() : "");
			}
			if (view2 != null) {
				view2.setBackground(new GradientDrawable(
					GradientDrawable.Orientation.BOTTOM_TOP,
					new int[]{Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT}
				));
			}
			if (textTitle != null) {
				textTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
				textTitle.setIncludeFontPadding(false);
				textTitle.setText(item.get("title") != null ? item.get("title").toString() : "");
			}
			if (textEpisode != null) {
				textEpisode.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
				textEpisode.setText(item.get("episode") != null ? item.get("episode").toString() : "");
			}

			String coverUrl = item.get("cover") != null ? item.get("cover").toString().trim() : "";
			if (coverUrl.contains("i.ibb.co.com")) coverUrl = coverUrl.replace("i.ibb.co.com", "i.ibb.co");
			if (coverUrl.startsWith("http://")) coverUrl = coverUrl.replace("http://", "https://");
			if (imgCover != null && !coverUrl.isEmpty()) {
				Glide.with(getApplicationContext()).load(Uri.parse(coverUrl)).into(imgCover);
			}

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String nextSlug = item.get("slug") != null ? item.get("slug").toString() : "";
					if (!nextSlug.isEmpty()) {
						Intent intent = new Intent(WatchActivity.this, WatchActivity.class);
						intent.putExtra("slug", nextSlug);
						startActivity(intent);
						finish();
					}
				}
			});
		}

		@Override
		public int getItemCount() {
			return _data.size();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}
}
