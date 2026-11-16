package com.vudev.pet.render

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import com.vudev.pet.core.Mood
import kotlin.math.sin

/**
 * View hiển thị thú cưng. Tự quản lý vòng vẽ bằng Choreographer, giảm FPS khi nghỉ,
 * dừng khi view không còn hiển thị. Không chứa logic nghiệp vụ.
 *
 * App (Activity hoặc overlay) chỉ cần: setMood, setWalking, setFacingLeft,
 * setLookDirection, setTalking, burst, setPaused.
 */
class PetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val renderer = PetRenderer()
    private val visual = PetVisual()

    var look: PetLook = PetLooks.BONG
        set(value) {
            field = value
            invalidate()
        }

    /** FPS mục tiêu khi nghỉ; khi đi lại/tương tác luôn vẽ mượt hơn. */
    var idleFps: Int = 15
    var activeFps: Int = 60

    private var running = false
    private var paused = false
    private var lastFrameMs = 0L
    private var walkPhase = 0f
    private var blink = 0f
    private var blinking = false
    private var blinkStartMs = 0L
    private var nextBlinkAtMs = 0L
    private var zzzAtMs = 0L
    private val random = kotlin.random.Random(System.currentTimeMillis())

    private val frameCallback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running || paused) return
            val now = frameTimeNanos / 1_000_000L
            val dtMs = if (lastFrameMs == 0L) 16L else (now - lastFrameMs).coerceAtMost(100L)
            lastFrameMs = now
            step(dtMs.toFloat() / 1000f, now)
            invalidate()
            val interval = if (isActiveNow()) 1000L / activeFps.coerceAtLeast(1) else 1000L / idleFps.coerceAtLeast(1)
            Choreographer.getInstance().postFrameCallbackDelayed(frameCallback, interval)
        }
    }

    private fun isActiveNow(): Boolean = visual.walking || visual.talking || renderer.hasParticles()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startLoop()
    }

    override fun onDetachedFromWindow() {
        stopLoop()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) startLoop() else stopLoop()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) startLoop() else stopLoop()
    }

    private fun startLoop() {
        if (running) return
        running = true
        lastFrameMs = 0L
        nextBlinkAtMs = SystemClock.uptimeMillis() + 2_000L + random.nextInt(3_000)
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopLoop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun step(dt: Float, nowMs: Long) {
        val dtMs = dt * 1000f
        // Nhịp thở nhẹ khi đứng, nhún khi đi.
        if (visual.walking) {
            walkPhase += dt * 11f
            visual.bobPx = -kotlin.math.abs(sin(walkPhase)) * (height * 0.025f)
            visual.squash = 1f + sin(walkPhase * 2f) * 0.035f
        } else {
            walkPhase += dt * 2.2f
            visual.bobPx = sin(walkPhase) * (height * 0.006f)
            visual.squash = 1f + sin(walkPhase * 0.9f) * 0.012f
        }
        visual.touchGlow = (visual.touchGlow - dt * 1.8f).coerceAtLeast(0f)
        visual.tiltDeg = visual.tiltDeg * 0.92f

        // Chớp mắt: 160 ms, cách nhau 2.5-6 giây, không chớp khi buồn ngủ/đang nhắm.
        if (visual.mood == Mood.SLEEPY) {
            blink = 1f
        } else if (blink >= 0.99f && !blinking) {
            // Vừa thức dậy sau mood buồn ngủ: mở mắt lại thay vì kẹt trạng thái nhắm.
            blink = 0f
            nextBlinkAtMs = nowMs + 2_000L + random.nextInt(3_000)
        } else if (!blinking && nowMs >= nextBlinkAtMs) {
            blinking = true
            blinkStartMs = nowMs
        } else if (blinking) {
            val p = (nowMs - blinkStartMs) / 160f
            blink = if (p < 0.5f) p * 2f else (1f - p) * 2f
            if (p >= 1f) {
                blinking = false
                blink = 0f
                nextBlinkAtMs = nowMs + 2_500L + random.nextInt(3_500)
            }
        }

        // Buồn ngủ thì thỉnh thoảng nhả chữ Z.
        if (visual.mood == Mood.SLEEPY && nowMs - zzzAtMs > 2_200L) {
            zzzAtMs = nowMs
            renderer.burst(Effect.ZZZ, minOf(width, height) * 0.4f)
        }
        renderer.update(dt.coerceAtMost(0.1f))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        renderer.draw(canvas, width, height, look, visual, blink.coerceIn(0f, 1f))
    }

    fun setMood(mood: Mood) {
        visual.mood = mood
        if (mood == Mood.EXCITED || mood == Mood.HAPPY) renderer.burst(Effect.SPARKLES, minOf(width, height) * 0.35f)
        if (mood == Mood.ANGRY) renderer.burst(Effect.ANGER, minOf(width, height) * 0.35f)
        invalidate()
    }

    fun setWalking(walking: Boolean) {
        visual.walking = walking
    }

    fun setFacingLeft(left: Boolean) {
        visual.facingLeft = left
        // Lật ngang bằng scaleX để không phải vẽ lại toàn bộ toạ độ.
        scaleX = if (left) -1f else 1f
    }

    fun setLookDirection(x: Float, y: Float) {
        visual.lookX = x.coerceIn(-1f, 1f)
        visual.lookY = y.coerceIn(-1f, 1f)
    }

    fun setTalking(talking: Boolean) {
        visual.talking = talking
    }

    fun pulseTouch() {
        visual.touchGlow = 1f
    }

    fun burst(effect: Effect) {
        renderer.burst(effect, minOf(width, height) * 0.35f)
        invalidate()
    }

    /** Dừng vẽ khi màn hình tắt hoặc app yêu cầu tiết kiệm pin. */
    fun setPaused(paused: Boolean) {
        this.paused = paused
        if (paused) {
            stopLoop()
        } else {
            startLoop()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val size = if (w > 0 && h > 0) minOf(w, h) else (96 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(size, size)
    }
}
