package com.dongflix

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ShimmerHelper {

	private var animator: ValueAnimator? = null
	private var phase = 0f
	private val trackedViews = ArrayList<View>()

	fun createDrawable(cornerRadiusPx: Float): Drawable {
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

				val translateX = -w + (2f * w * phase)
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

	fun applyToView(v: View?, cornerRadiusDp: Float) {
		if (v == null) return
		val d = v.resources.displayMetrics.density
		v.background = createDrawable(cornerRadiusDp * d)
		if (!trackedViews.contains(v)) {
			trackedViews.add(v)
		}
	}

	fun start() {
		if (animator?.isRunning == true) return
		animator = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = 1250L
			repeatCount = ValueAnimator.INFINITE
			interpolator = LinearInterpolator()
			addUpdateListener { animation ->
				phase = animation.animatedValue as Float
				for (v in trackedViews) {
					v.invalidate()
					if (v is ViewGroup) {
						invalidateChildren(v)
					}
				}
			}
			start()
		}
	}

	private fun invalidateChildren(vg: ViewGroup) {
		for (i in 0 until vg.childCount) {
			val c = vg.getChildAt(i)
			c.invalidate()
			if (c is ViewGroup) {
				invalidateChildren(c)
			}
		}
	}

	fun stop() {
		animator?.cancel()
		animator = null
		for (v in trackedViews) {
			v.background = null
		}
		trackedViews.clear()
	}

	fun createGridAdapter(itemCount: Int): RecyclerView.Adapter<*> {
		return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
			override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
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
					layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (150 * d).toInt())
					background = createDrawable(10 * d)
				}
				box.addView(coverStub)

				val titleStub = View(ctx).apply {
					layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (14 * d).toInt()).apply {
						topMargin = (8 * d).toInt()
					}
					background = createDrawable(5 * d)
				}
				box.addView(titleStub)

				if (!trackedViews.contains(box)) trackedViews.add(box)
				return object : RecyclerView.ViewHolder(box) {}
			}

			override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
			override fun getItemCount(): Int = itemCount
		}
	}

	fun createHorizontalAdapter(itemCount: Int, widthDp: Int, heightDp: Int): RecyclerView.Adapter<*> {
		return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
			override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
				val ctx = parent.context
				val d = ctx.resources.displayMetrics.density

				val cardStub = View(ctx).apply {
					layoutParams = RecyclerView.LayoutParams((widthDp * d).toInt(), (heightDp * d).toInt()).apply {
						marginEnd = (10 * d).toInt()
					}
					background = createDrawable(12 * d)
				}

				if (!trackedViews.contains(cardStub)) trackedViews.add(cardStub)
				return object : RecyclerView.ViewHolder(cardStub) {}
			}

			override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
			override fun getItemCount(): Int = itemCount
		}
	}
}
