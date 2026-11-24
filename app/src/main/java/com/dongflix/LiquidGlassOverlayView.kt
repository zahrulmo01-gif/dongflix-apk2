package com.dongflix

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class LiquidGlassOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var isDropletMode: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var pressProgress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val clipPath = Path()
    private val rectF = RectF()

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val topSheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var liquidShader: Any? = null
    private var shaderFailed = false

    init {
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                liquidShader = RuntimeShader(AGSL_LIQUID_GLASS_SHADER)
            } catch (_: Throwable) {
                liquidShader = null
                shaderFailed = true
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 2f || h <= 2f) return

        val d = resources.displayMetrics.density
        val radius = min(w, h) * 0.5f

        rectF.set(0f, 0f, w, h)
        clipPath.reset()
        clipPath.addRoundRect(rectF, radius, radius, Path.Direction.CW)

        val saveCount = canvas.save()
        try {
            canvas.clipPath(clipPath)

            var drewAgsl = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !shaderFailed && liquidShader is RuntimeShader) {
                try {
                    val shader = liquidShader as RuntimeShader
                    shader.setFloatUniform("uSize", w, h)
                    shader.setFloatUniform("uRadius", radius)
                    shader.setFloatUniform("uPress", pressProgress)
                    shader.setFloatUniform("uIsDroplet", if (isDropletMode) 1.0f else 0.0f)
                    shaderPaint.shader = shader
                    canvas.drawRect(rectF, shaderPaint)
                    drewAgsl = true
                } catch (_: Throwable) {
                    shaderFailed = true
                    shaderPaint.shader = null
                }
            }

            if (!drewAgsl) {
                topSheenPaint.shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(
                        Color.argb(if (isDropletMode) 95 else 58, 255, 255, 255),
                        Color.argb(16, 255, 255, 255),
                        Color.TRANSPARENT,
                        Color.argb(if (isDropletMode) 48 else 28, 140, 210, 255)
                    ),
                    floatArrayOf(0f, 0.34f, 0.68f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(rectF, topSheenPaint)
            }

            if (isDropletMode || pressProgress > 0.01f) {
                val glowAlpha = if (isDropletMode) (42 + (38 * pressProgress)).toInt() else (28 * pressProgress).toInt()
                innerGlowPaint.shader = RadialGradient(
                    w * 0.5f, h * 0.22f, maxOf(w, h) * 0.72f,
                    intArrayOf(Color.argb(glowAlpha, 255, 255, 255), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(rectF, innerGlowPaint)
            }

            val strokeW = (if (isDropletMode) 1.35f else 1.15f) * d
            borderPaint.strokeWidth = strokeW
            borderPaint.shader = LinearGradient(
                0f, 0f, w * 0.25f, h,
                intArrayOf(
                    Color.argb(if (isDropletMode) 195 else 150, 255, 255, 255),
                    Color.argb(38, 255, 255, 255),
                    Color.argb(22, 148, 163, 184),
                    Color.argb(if (isDropletMode) 135 else 95, 210, 235, 255)
                ),
                floatArrayOf(0f, 0.38f, 0.72f, 1f),
                Shader.TileMode.CLAMP
            )
            val inset = strokeW * 0.5f
            val rInset = maxOf(0f, radius - inset)
            rectF.set(inset, inset, w - inset, h - inset)
            canvas.drawRoundRect(rectF, rInset, rInset, borderPaint)
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    companion object {
        private const val AGSL_LIQUID_GLASS_SHADER = """
            uniform float2 uSize;
            uniform float uRadius;
            uniform float uPress;
            uniform float uIsDroplet;

            float sdRoundBox(float2 p, float2 b, float r) {
                float2 q = abs(p) - b + float2(r, r);
                return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0, 0.0))) - r;
            }

            half4 main(float2 fragCoord) {
                float2 halfSize = uSize * 0.5;
                float2 p = fragCoord - halfSize;
                float r = min(uRadius, min(halfSize.x, halfSize.y));

                float dist = sdRoundBox(p, halfSize, r);
                if (dist > 0.0) {
                    return half4(0.0, 0.0, 0.0, 0.0);
                }

                float bezelWidth = clamp(r * 0.42, 8.0, 26.0);
                float edgeFactor = 1.0 - clamp(-dist / max(bezelWidth, 1.0), 0.0, 1.0);
                float lensCurve = edgeFactor * edgeFactor;

                float2 norm = normalize(p + float2(0.001, 0.001));
                float topLight = clamp(-norm.y * 0.75 - norm.x * 0.25, 0.0, 1.0);
                float bottomBounce = clamp(norm.y * 0.65, 0.0, 1.0);

                float disp = lensCurve * (0.14 + uPress * 0.08 + uIsDroplet * 0.06);
                float rChan = disp * clamp(topLight + 0.35, 0.0, 1.2);
                float gChan = disp * clamp(topLight + 0.30, 0.0, 1.2);
                float bChan = disp * clamp(topLight + bottomBounce * 0.8 + 0.42, 0.0, 1.3);

                float ny = fragCoord.y / max(uSize.y, 1.0);
                float surfaceSheen = pow(clamp(1.0 - ny * 1.45, 0.0, 1.0), 2.2)
                                   * (0.16 + uIsDroplet * 0.14 + uPress * 0.08);

                float baseTint = 0.04 + uIsDroplet * 0.08 + uPress * 0.04;
                float alpha = clamp(baseTint + surfaceSheen + lensCurve * (0.24 + uIsDroplet * 0.10), 0.0, 0.62);

                float3 col = float3(
                    clamp(0.88 + rChan + surfaceSheen, 0.0, 1.0),
                    clamp(0.92 + gChan + surfaceSheen, 0.0, 1.0),
                    clamp(1.00 + bChan + surfaceSheen, 0.0, 1.0)
                );

                return half4(col * alpha, alpha);
            }
        """
    }
}
