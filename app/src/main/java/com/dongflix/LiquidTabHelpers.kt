package com.dongflix

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LocalLiquidBottomTabScale = compositionLocalOf { { 1f } }

class DampedDragAnimation(
	private val animationScope: CoroutineScope,
	initialValue: Float,
	val valueRange: ClosedFloatingPointRange<Float>,
	visibilityThreshold: Float = 0.001f,
	val initialScale: Float = 1f,
	val pressedScale: Float = 1.15f,
	private val onDragStarted: () -> Unit = {},
	private val onDragStopped: () -> Unit = {},
	private val onDrag: (size: IntSize, dragAmount: Offset) -> Unit
) {
	private val valueAnimatable = Animatable(initialValue).apply {
		this.visibilityThreshold = visibilityThreshold
	}
	private val scaleAnimatable = Animatable(initialScale)

	val value: Float
		get() = valueAnimatable.value

	val targetValue: Float
		get() = valueAnimatable.targetValue

	val velocity: Float
		get() = valueAnimatable.velocity

	val pressProgress: Float
		get() = ((scaleAnimatable.value - initialScale) / (pressedScale - initialScale).coerceAtLeast(0.0001f))
			.fastCoerceIn(0f, 1f)

	val scaleX: Float
		get() = scaleAnimatable.value

	val scaleY: Float
		get() = scaleAnimatable.value

	val modifier: Modifier = Modifier.pointerInput(valueRange) {
		detectTapGestures(
			onPress = {
				onDragStarted()
				animationScope.launch {
					scaleAnimatable.animateTo(
						pressedScale,
						spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
					)
				}
				try {
					awaitRelease()
				} finally {
					animationScope.launch {
						scaleAnimatable.animateTo(
							initialScale,
							spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
						)
					}
					onDragStopped()
				}
			}
		)
	}.pointerInput(valueRange) {
		detectHorizontalDragGestures(
			onDragStart = {
				onDragStarted()
				animationScope.launch {
					scaleAnimatable.animateTo(
						pressedScale,
						spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
					)
				}
			},
			onDragEnd = {
				animationScope.launch {
					scaleAnimatable.animateTo(
						initialScale,
						spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
					)
				}
				onDragStopped()
			},
			onDragCancel = {
				animationScope.launch {
					scaleAnimatable.animateTo(
						initialScale,
						spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
					)
				}
				onDragStopped()
			},
			onHorizontalDrag = { change, dragAmount ->
				change.consume()
				onDrag(size, Offset(dragAmount, 0f))
			}
		)
	}

	fun updateValue(newValue: Float) {
		val clamped = newValue.fastCoerceIn(valueRange.start, valueRange.endInclusive)
		animationScope.launch {
			valueAnimatable.snapTo(clamped)
		}
	}

	fun animateToValue(target: Float) {
		val clamped = target.fastCoerceIn(valueRange.start, valueRange.endInclusive)
		animationScope.launch {
			valueAnimatable.animateTo(
				clamped,
				spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
			)
		}
	}
}

class InteractiveHighlight(
	private val animationScope: CoroutineScope,
	private val position: (size: IntSize, offset: Offset) -> Offset
) {
	private val pressAnim = Animatable(0f)
	private var lastOffset: Offset = Offset.Zero

	val modifier: Modifier = Modifier
	val gestureModifier: Modifier = Modifier.pointerInput(Unit) {
		detectTapGestures(
			onPress = { offset ->
				lastOffset = offset
				animationScope.launch { pressAnim.animateTo(1f) }
				try {
					awaitRelease()
				} finally {
					animationScope.launch { pressAnim.animateTo(0f) }
				}
			}
		)
	}

	fun currentCenter(size: IntSize): Offset = position(size, lastOffset)
}
